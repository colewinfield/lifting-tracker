package com.colewinfield.liftingtracker.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.colewinfield.liftingtracker.data.AppSettings
import com.colewinfield.liftingtracker.data.LiftingRepository
import com.colewinfield.liftingtracker.data.SettingsRepository
import com.colewinfield.liftingtracker.data.ThemeMode
import com.colewinfield.liftingtracker.data.WeightUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val programName: String,
    val sessionCount: Int,
    val cycleNumber: Int,
    val unit: WeightUnit,
    val themeMode: ThemeMode,
    val useDynamicColor: Boolean,
    val currentWeek: Int,
    val cycleLength: Int,
    val displayName: String,
    val bodyweight: Double,
    val heightInches: Int,
    val age: Int,
) {
    companion object {
        val Empty = ProfileUiState(
            programName = "—",
            sessionCount = 0,
            cycleNumber = 1,
            unit = WeightUnit.LB,
            themeMode = ThemeMode.SYSTEM,
            useDynamicColor = true,
            currentWeek = AppSettings.Defaults.currentWeek,
            cycleLength = 9,
            displayName = AppSettings.Defaults.displayName,
            bodyweight = AppSettings.Defaults.bodyweight,
            heightInches = AppSettings.Defaults.heightInches,
            age = AppSettings.Defaults.age,
        )
    }
}

class ProfileViewModel(
    private val repo: LiftingRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    val state: StateFlow<ProfileUiState> = combine(
        settingsRepo.settings,
        repo.observeCurrentProgram(),
        repo.observeAllSessions(),
    ) { settings, program, sessions ->
        ProfileUiState(
            programName = program?.name ?: "—",
            sessionCount = sessions.count { it.finishedAt != null },
            cycleNumber = 1,
            unit = settings.unit,
            themeMode = settings.themeMode,
            useDynamicColor = settings.useDynamicColor,
            currentWeek = settings.currentWeek,
            cycleLength = program?.cycleLength ?: 9,
            displayName = settings.displayName,
            bodyweight = settings.bodyweight,
            heightInches = settings.heightInches,
            age = settings.age,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState.Empty,
    )

    fun toggleUnit() {
        viewModelScope.launch {
            val next = if (state.value.unit == WeightUnit.LB) WeightUnit.KG else WeightUnit.LB
            settingsRepo.setUnit(next)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepo.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setUseDynamicColor(enabled) }
    }

    fun setCurrentWeek(week: Int) {
        viewModelScope.launch {
            settingsRepo.setCurrentWeek(week.coerceIn(1, state.value.cycleLength))
        }
    }

    fun saveProfile(name: String, bodyweight: Double, heightInches: Int, age: Int) {
        viewModelScope.launch {
            settingsRepo.setProfile(
                displayName = name.trim().ifEmpty { "You" },
                bodyweight = bodyweight.coerceAtLeast(0.0),
                heightInches = heightInches.coerceAtLeast(0),
                age = age.coerceAtLeast(0),
            )
        }
    }

    companion object {
        fun factory(repo: LiftingRepository, settingsRepo: SettingsRepository) = viewModelFactory {
            initializer { ProfileViewModel(repo, settingsRepo) }
        }
    }
}
