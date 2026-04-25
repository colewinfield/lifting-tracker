package com.colewinfield.liftingtracker.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.colewinfield.liftingtracker.data.ActiveSessionState
import com.colewinfield.liftingtracker.data.Alternative
import com.colewinfield.liftingtracker.data.AppSettings
import com.colewinfield.liftingtracker.data.Day
import com.colewinfield.liftingtracker.data.HistoryEntry
import com.colewinfield.liftingtracker.data.LiftingRepository
import com.colewinfield.liftingtracker.data.Note
import com.colewinfield.liftingtracker.data.PerformedSet
import com.colewinfield.liftingtracker.data.Program
import com.colewinfield.liftingtracker.data.SettingsRepository
import com.colewinfield.liftingtracker.data.Weekday
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface TodaySheet {
    data class Swap(val liftId: String) : TodaySheet
    data class Notes(val liftId: String) : TodaySheet
}

data class TodayUiState(
    val program: Program?,
    val day: Day?,
    val weekday: Weekday,
    val weekNumber: Int,
    val isDeload: Boolean,
    val sessionId: String?,
    val sets: Map<String, List<PerformedSet>>,
    val lastWeekByLift: Map<String, HistoryEntry>,
    val expandedLiftId: String?,
    // Sheets + per-lift transient state. Swaps are session-scoped (cleared on Finish session).
    val activeSheet: TodaySheet?,
    val swapsByLift: Map<String, Alternative>,
    val alternativesByLift: Map<String, List<Alternative>>,
    val notesByLift: Map<String, List<Note>>,
) {
    companion object {
        val Empty = TodayUiState(
            program = null,
            day = null,
            weekday = Weekday.MON,
            weekNumber = 0,
            isDeload = false,
            sessionId = null,
            sets = emptyMap(),
            lastWeekByLift = emptyMap(),
            expandedLiftId = null,
            activeSheet = null,
            swapsByLift = emptyMap(),
            alternativesByLift = emptyMap(),
            notesByLift = emptyMap(),
        )
    }
}

private data class UiOnly(
    val expandedLiftId: String? = null,
    val userToggled: Boolean = false,
    val activeSheet: TodaySheet? = null,
    val swapsByLift: Map<String, Alternative> = emptyMap(),
    val alternativesByLift: Map<String, List<Alternative>> = emptyMap(),
)

private data class TodaySources(
    val settings: AppSettings,
    val program: Program?,
    val day: Day?,
    val weekday: Weekday,
    val ui: UiOnly,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val repo: LiftingRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    private val uiOnly = MutableStateFlow(UiOnly())

    private val settingsState: StateFlow<AppSettings> = settingsRepo.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings.Defaults,
    )

    val state: StateFlow<TodayUiState> = combine(
        settingsRepo.settings,
        repo.observeCurrentProgram(),
        uiOnly,
    ) { settings, program, ui ->
        val weekday = Weekday.today()
        val day = program?.days?.firstOrNull { it.dayOfWeek == weekday }
        TodaySources(settings, program, day, weekday, ui)
    }.flatMapLatest { src ->
        if (src.day == null) {
            flowOf(Triple(src, ActiveSessionState(sessionId = null, setsByLift = emptyMap()), emptyMap<String, List<Note>>()))
        } else {
            val active = repo.observeActiveSession(src.day.id, src.settings.currentWeek)
            val notes = repo.observeNotesByLift(src.day.lifts.map { it.id })
            combine(active, notes) { a, n -> Triple(src, a, n) }
        }
    }.mapLatest { (src, active, notesByLift) ->
        val lastWeek = if (src.day != null) {
            repo.lastSessionsFor(src.day.lifts.map { it.id }, active.sessionId)
        } else emptyMap()
        val expanded = if (src.ui.userToggled) src.ui.expandedLiftId
            else src.day?.lifts?.firstOrNull()?.id
        TodayUiState(
            program = src.program,
            day = src.day,
            weekday = src.weekday,
            weekNumber = src.settings.currentWeek,
            isDeload = src.program?.let { src.settings.currentWeek == it.deloadWeek } ?: false,
            sessionId = active.sessionId,
            sets = active.setsByLift,
            lastWeekByLift = lastWeek,
            expandedLiftId = expanded,
            activeSheet = src.ui.activeSheet,
            swapsByLift = src.ui.swapsByLift,
            alternativesByLift = src.ui.alternativesByLift,
            notesByLift = notesByLift,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayUiState.Empty,
    )

    init {
        viewModelScope.launch { repo.ensureSeeded() }
        // Eagerly load alternatives once a program is available so the Swap sheet renders
        // without a loading delay. Re-runs when the program structure changes.
        viewModelScope.launch {
            repo.observeCurrentProgram().collect { program ->
                program ?: return@collect
                val byLift = program.days.flatMap { it.lifts }
                    .associate { it.id to repo.alternativesFor(it.id) }
                uiOnly.update { it.copy(alternativesByLift = byLift) }
            }
        }
    }

    fun toggleExpand(liftId: String) {
        uiOnly.update {
            it.copy(
                expandedLiftId = if (it.expandedLiftId == liftId) null else liftId,
                userToggled = true,
            )
        }
    }

    fun addSet(liftId: String) {
        viewModelScope.launch {
            val s = settingsState.value
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
                dayId = day.id,
                week = s.currentWeek,
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
            // Swaps are session-scoped — clear them once the user finishes.
            uiOnly.update { it.copy(swapsByLift = emptyMap()) }
        }
    }

    // ----- Sheets -----

    fun openSwapSheet(liftId: String) {
        uiOnly.update { it.copy(activeSheet = TodaySheet.Swap(liftId)) }
    }

    fun openNotesSheet(liftId: String) {
        uiOnly.update { it.copy(activeSheet = TodaySheet.Notes(liftId)) }
    }

    fun closeSheet() {
        uiOnly.update { it.copy(activeSheet = null) }
    }

    fun applySwap(liftId: String, alternative: Alternative) {
        uiOnly.update {
            it.copy(
                swapsByLift = it.swapsByLift + (liftId to alternative),
                activeSheet = null,
            )
        }
    }

    fun clearSwap(liftId: String) {
        uiOnly.update { it.copy(swapsByLift = it.swapsByLift - liftId) }
    }

    fun addNote(liftId: String, text: String, whoopsy: Boolean) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val s = settingsState.value
            val day = state.value.day ?: return@launch
            repo.appendNote(
                dayId = day.id,
                week = s.currentWeek,
                liftId = liftId,
                text = trimmed,
                whoopsy = whoopsy,
            )
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch { repo.deleteNote(noteId) }
    }

    companion object {
        fun factory(repo: LiftingRepository, settingsRepo: SettingsRepository) = viewModelFactory {
            initializer { TodayViewModel(repo, settingsRepo) }
        }
    }
}
