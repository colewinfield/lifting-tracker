package com.colewinfield.liftingtracker.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.colewinfield.liftingtracker.data.Day
import com.colewinfield.liftingtracker.data.LiftingRepository
import com.colewinfield.liftingtracker.data.Program
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DeloadMode { NONE, LAST, CUSTOM }

data class ProgramEditUiState(
    val loaded: Boolean = false,
    val programId: String = "",
    val name: String = "",
    val cycleLength: Int = 9,
    val deloadMode: DeloadMode = DeloadMode.LAST,
    val customDeloadWeek: Int = 1,
    val days: List<Day> = emptyList(),
    val saving: Boolean = false,
    val deleted: Boolean = false,
)

class ProgramEditViewModel(
    private val repo: LiftingRepository,
) : ViewModel() {

    // Buffered edits — start as null until the program loads, then merged in.
    private data class Edits(
        val name: String? = null,
        val cycleLength: Int? = null,
        val deloadMode: DeloadMode? = null,
        val customDeloadWeek: Int? = null,
    )

    private val edits = MutableStateFlow(Edits())
    private val deleted = MutableStateFlow(false)

    val state: StateFlow<ProgramEditUiState> = combine(
        repo.observeCurrentProgram(),
        edits,
        deleted,
    ) { program, e, isDeleted ->
        if (isDeleted) {
            ProgramEditUiState(loaded = true, deleted = true)
        } else if (program == null) {
            ProgramEditUiState(loaded = false)
        } else {
            val resolvedMode = e.deloadMode ?: deriveDeloadMode(program)
            val resolvedCustom = e.customDeloadWeek ?: program.deloadWeek.coerceIn(1, program.cycleLength)
            ProgramEditUiState(
                loaded = true,
                programId = program.id,
                name = e.name ?: program.name,
                cycleLength = e.cycleLength ?: program.cycleLength,
                deloadMode = resolvedMode,
                customDeloadWeek = resolvedCustom,
                days = program.days,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProgramEditUiState(),
    )

    fun setName(value: String) = edits.update { it.copy(name = value) }
    fun setCycleLength(value: Int) =
        edits.update { it.copy(cycleLength = value.coerceIn(1, 52)) }
    fun setDeloadMode(mode: DeloadMode) = edits.update { it.copy(deloadMode = mode) }
    fun setCustomDeloadWeek(week: Int) {
        val cycle = state.value.cycleLength
        edits.update { it.copy(customDeloadWeek = week.coerceIn(1, cycle)) }
    }

    fun save(onDone: () -> Unit) {
        val s = state.value
        if (!s.loaded || s.programId.isBlank()) return
        viewModelScope.launch {
            val resolvedDeload = when (s.deloadMode) {
                DeloadMode.NONE -> 0
                DeloadMode.LAST -> s.cycleLength
                DeloadMode.CUSTOM -> s.customDeloadWeek.coerceIn(1, s.cycleLength)
            }
            repo.updateProgramMeta(
                programId = s.programId,
                name = s.name.trim().ifBlank { "Program" },
                cycleLength = s.cycleLength,
                deloadWeek = resolvedDeload,
            )
            edits.value = Edits()
            onDone()
        }
    }

    fun deleteProgram(onDone: () -> Unit) {
        val id = state.value.programId
        if (id.isBlank()) return
        viewModelScope.launch {
            repo.deleteProgram(id)
            deleted.value = true
            onDone()
        }
    }

    private fun deriveDeloadMode(program: Program): DeloadMode = when {
        program.deloadWeek <= 0 -> DeloadMode.NONE
        program.deloadWeek == program.cycleLength -> DeloadMode.LAST
        else -> DeloadMode.CUSTOM
    }

    companion object {
        fun factory(repo: LiftingRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ProgramEditViewModel(repo) as T
            }
    }
}
