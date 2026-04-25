package com.colewinfield.liftingtracker.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.colewinfield.liftingtracker.data.Lift
import com.colewinfield.liftingtracker.data.LiftingRepository
import com.colewinfield.liftingtracker.data.Weekday
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val NEW_ID = "new"

data class DayEditUiState(
    val loaded: Boolean = false,
    val isNew: Boolean = false,
    val dayId: String = "",
    val programId: String = "",
    val name: String = "",
    val dayOfWeek: Weekday = Weekday.MON,
    val focus: String = "",
    val isRest: Boolean = false,
    val lifts: List<Lift> = emptyList(),
)

class DayEditViewModel(
    private val repo: LiftingRepository,
    private val dayId: String,
) : ViewModel() {

    private data class Buffer(
        val name: String? = null,
        val dayOfWeek: Weekday? = null,
        val focus: String? = null,
        val isRest: Boolean? = null,
    )

    private val buffer = MutableStateFlow(Buffer())
    private val newDayDefaults = MutableStateFlow(
        if (dayId == NEW_ID) Buffer(
            name = "New day",
            dayOfWeek = Weekday.MON,
            focus = "",
            isRest = false,
        ) else Buffer()
    )

    val state: StateFlow<DayEditUiState> = combine(
        repo.observeCurrentProgram(),
        buffer,
        newDayDefaults,
    ) { program, b, defaults ->
        if (program == null) return@combine DayEditUiState(loaded = false)
        if (dayId == NEW_ID) {
            DayEditUiState(
                loaded = true,
                isNew = true,
                dayId = NEW_ID,
                programId = program.id,
                name = b.name ?: defaults.name ?: "New day",
                dayOfWeek = b.dayOfWeek ?: defaults.dayOfWeek ?: Weekday.MON,
                focus = b.focus ?: defaults.focus.orEmpty(),
                isRest = b.isRest ?: defaults.isRest ?: false,
                lifts = emptyList(),
            )
        } else {
            val day = program.days.firstOrNull { it.id == dayId }
                ?: return@combine DayEditUiState(loaded = false)
            DayEditUiState(
                loaded = true,
                isNew = false,
                dayId = day.id,
                programId = program.id,
                name = b.name ?: day.name,
                dayOfWeek = b.dayOfWeek ?: day.dayOfWeek,
                focus = b.focus ?: day.focus,
                isRest = b.isRest ?: day.isRest,
                lifts = day.lifts,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DayEditUiState(),
    )

    fun setName(value: String) = buffer.update { it.copy(name = value) }
    fun setDayOfWeek(value: Weekday) = buffer.update { it.copy(dayOfWeek = value) }
    fun setFocus(value: String) = buffer.update { it.copy(focus = value) }
    fun setRest(value: Boolean) = buffer.update { it.copy(isRest = value) }

    fun save(onDone: () -> Unit) {
        val s = state.value
        if (!s.loaded) return
        viewModelScope.launch {
            if (s.isNew) {
                repo.addDay(
                    programId = s.programId,
                    name = s.name.trim().ifBlank { "Day" },
                    dayOfWeek = s.dayOfWeek,
                    focus = s.focus.trim(),
                    isRest = s.isRest,
                )
            } else {
                repo.updateDay(
                    dayId = s.dayId,
                    name = s.name.trim().ifBlank { "Day" },
                    dayOfWeek = s.dayOfWeek,
                    focus = s.focus.trim(),
                    isRest = s.isRest,
                )
            }
            buffer.value = Buffer()
            onDone()
        }
    }

    fun deleteDay(onDone: () -> Unit) {
        val s = state.value
        if (s.isNew || s.dayId.isBlank()) {
            onDone()
            return
        }
        viewModelScope.launch {
            repo.deleteDay(s.dayId)
            onDone()
        }
    }

    /**
     * Resolve the day's id, persisting first if this is a new day. Used by "Add lift" so the
     * lift attaches to a real DB row rather than a transient buffer. Returns the resolved id,
     * or null if the program isn't loaded yet.
     */
    suspend fun ensurePersisted(): String? {
        val s = state.value
        if (!s.loaded) return null
        if (!s.isNew) return s.dayId
        // Persist with current buffer values, then look up the inserted id by re-reading.
        val program = repo.observeCurrentProgram().first() ?: return null
        val newId = repo.addDay(
            programId = program.id,
            name = s.name.trim().ifBlank { "Day" },
            dayOfWeek = s.dayOfWeek,
            focus = s.focus.trim(),
            isRest = s.isRest,
        )
        buffer.value = Buffer()
        // Caller will navigate to lift-edit with the persisted id; nav layer handles route swap.
        return newId
    }

    companion object {
        fun factory(repo: LiftingRepository, dayId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    DayEditViewModel(repo, dayId) as T
            }
    }
}
