package com.colewinfield.liftingtracker.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.colewinfield.liftingtracker.data.AppSettings
import com.colewinfield.liftingtracker.data.LiftingRepository
import com.colewinfield.liftingtracker.data.Program
import com.colewinfield.liftingtracker.data.SettingsRepository
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
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    // -1 = not yet user-selected. The state flow falls back to currentWeek until the user taps
    // a chip, so a fresh launch always lands on "now". Persisting the selection across launches
    // would feel wrong — the user almost always wants today, not last week's view.
    private val selectedWeekOverride = MutableStateFlow(-1)

    val state: StateFlow<ProgramUiState> = combine(
        repo.observeCurrentProgram(),
        settingsRepo.settings,
        selectedWeekOverride,
    ) { program, settings, override ->
        val current = settings.currentWeek
        ProgramUiState(
            program = program,
            currentWeek = current,
            selectedWeek = if (override > 0) override else current,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProgramUiState.Empty.copy(
            currentWeek = AppSettings.Defaults.currentWeek,
            selectedWeek = AppSettings.Defaults.currentWeek,
        ),
    )

    init {
        viewModelScope.launch { repo.ensureSeeded() }
    }

    fun selectWeek(week: Int) {
        selectedWeekOverride.value = week
    }

    companion object {
        fun factory(repo: LiftingRepository, settingsRepo: SettingsRepository) = viewModelFactory {
            initializer { ProgramViewModel(repo, settingsRepo) }
        }
    }
}
