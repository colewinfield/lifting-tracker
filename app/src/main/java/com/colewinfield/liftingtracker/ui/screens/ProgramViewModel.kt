package com.colewinfield.liftingtracker.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.colewinfield.liftingtracker.data.LiftingRepository
import com.colewinfield.liftingtracker.data.Program
import com.colewinfield.liftingtracker.data.SampleData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProgramUiState(
    val program: Program?,
    val currentWeek: Int,
    val selectedWeek: Int,
) {
    companion object {
        val Empty = ProgramUiState(program = null, currentWeek = 0, selectedWeek = 0)
    }
}

class ProgramViewModel(
    private val repo: LiftingRepository,
) : ViewModel() {

    // TODO: source from a SettingsDao / DataStore once Profile/Onboarding is wired.
    private val currentWeek = SampleData.current.week

    private val selectedWeek = MutableStateFlow(currentWeek)

    val state: StateFlow<ProgramUiState> = combine(
        repo.observeCurrentProgram(),
        selectedWeek,
    ) { program, selected ->
        ProgramUiState(
            program = program,
            currentWeek = currentWeek,
            selectedWeek = selected,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProgramUiState.Empty.copy(currentWeek = currentWeek, selectedWeek = currentWeek),
    )

    init {
        viewModelScope.launch { repo.ensureSeeded() }
    }

    fun selectWeek(week: Int) {
        selectedWeek.value = week
    }

    companion object {
        fun factory(repo: LiftingRepository) = viewModelFactory {
            initializer { ProgramViewModel(repo) }
        }
    }
}
