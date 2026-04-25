package com.colewinfield.liftingtracker.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.colewinfield.liftingtracker.data.Day
import com.colewinfield.liftingtracker.data.HistoryEntry
import com.colewinfield.liftingtracker.data.LiftingRepository
import com.colewinfield.liftingtracker.data.PerformedSet
import com.colewinfield.liftingtracker.data.Program
import com.colewinfield.liftingtracker.data.SampleData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TodayUiState(
    val program: Program?,
    val day: Day?,
    val weekNumber: Int,
    val isDeload: Boolean,
    val sessionId: String?,
    val sets: Map<String, List<PerformedSet>>,
    val lastWeekByLift: Map<String, HistoryEntry>,
    val expandedLiftId: String?,
) {
    companion object {
        val Empty = TodayUiState(
            program = null,
            day = null,
            weekNumber = 0,
            isDeload = false,
            sessionId = null,
            sets = emptyMap(),
            lastWeekByLift = emptyMap(),
            expandedLiftId = null,
        )
    }
}

private data class UiOnly(val expandedLiftId: String? = null, val userToggled: Boolean = false)

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val repo: LiftingRepository,
) : ViewModel() {

    // TODO: source from a SettingsDao / DataStore once Profile/Onboarding is wired.
    private val currentDayId = SampleData.current.dayId
    private val currentWeek = SampleData.current.week

    private val uiOnly = MutableStateFlow(UiOnly())

    val state: StateFlow<TodayUiState> = combine(
        repo.observeCurrentProgram(),
        repo.observeActiveSession(currentDayId, currentWeek),
        uiOnly,
    ) { program, active, ui ->
        Triple(program, active, ui)
    }.mapLatest { (program, active, ui) ->
        val day = program?.days?.firstOrNull { it.id == currentDayId }
        val lastWeek = if (day != null) {
            repo.lastSessionsFor(day.lifts.map { it.id }, active.sessionId)
        } else emptyMap()
        val expanded = if (ui.userToggled) ui.expandedLiftId else day?.lifts?.firstOrNull()?.id
        TodayUiState(
            program = program,
            day = day,
            weekNumber = currentWeek,
            isDeload = program?.let { currentWeek == it.deloadWeek } ?: false,
            sessionId = active.sessionId,
            sets = active.setsByLift,
            lastWeekByLift = lastWeek,
            expandedLiftId = expanded,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayUiState.Empty,
    )

    init {
        viewModelScope.launch { repo.ensureSeeded() }
    }

    fun toggleExpand(liftId: String) {
        uiOnly.update {
            UiOnly(
                expandedLiftId = if (it.expandedLiftId == liftId) null else liftId,
                userToggled = true,
            )
        }
    }

    fun addSet(liftId: String) {
        viewModelScope.launch {
            val current = state.value
            val day = current.day ?: return@launch
            val lift = day.lifts.firstOrNull { it.id == liftId } ?: return@launch
            val existing = current.sets[liftId].orEmpty()
            val historicalSet = current.lastWeekByLift[liftId]?.sets?.let { sets ->
                sets.getOrNull(existing.size) ?: sets.firstOrNull()
            }
            val weight = historicalSet?.weight ?: 0.0
            val reps = historicalSet?.reps ?: lift.reps.first
            repo.appendSet(
                dayId = currentDayId,
                week = currentWeek,
                liftId = liftId,
                weight = weight,
                reps = reps,
            )
        }
    }

    fun toggleSetDone(setId: Long) {
        viewModelScope.launch { repo.toggleSetDone(setId) }
    }

    fun adjustWeight(setId: Long, delta: Double) {
        viewModelScope.launch { repo.adjustWeight(setId, delta) }
    }

    fun adjustReps(setId: Long, delta: Int) {
        viewModelScope.launch { repo.adjustReps(setId, delta) }
    }

    fun removeSet(setId: Long) {
        viewModelScope.launch { repo.removeSet(setId) }
    }

    fun finishSession() {
        viewModelScope.launch {
            state.value.sessionId?.let { repo.finishSession(it) }
        }
    }

    companion object {
        fun factory(repo: LiftingRepository) = viewModelFactory {
            initializer { TodayViewModel(repo) }
        }
    }
}
