package com.colewinfield.liftingtracker.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.colewinfield.liftingtracker.data.LiftingRepository
import com.colewinfield.liftingtracker.data.Program
import com.colewinfield.liftingtracker.data.SettingsRepository
import com.colewinfield.liftingtracker.data.db.PerformedSetEntity
import com.colewinfield.liftingtracker.data.db.SessionEntity
import com.colewinfield.liftingtracker.data.weekAndCycle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class HistoryUiState(
    val isReady: Boolean,
    val cycleLabel: String,
    val sessionsThisCycle: Int,
    val volumeLb: Double,
    val streakDays: Int,
    val prsThisCycle: Int,
    val chartWeeks: List<String>,
    val chartSeries: List<HistorySeries>,
    val muscleVolumes: List<MuscleVolume>,
) {
    companion object {
        val Empty = HistoryUiState(
            isReady = false,
            cycleLabel = "",
            sessionsThisCycle = 0,
            volumeLb = 0.0,
            streakDays = 0,
            prsThisCycle = 0,
            chartWeeks = emptyList(),
            chartSeries = emptyList(),
            muscleVolumes = emptyList(),
        )
    }
}

data class HistorySeries(
    val name: String,
    val points: List<HistoryPoint>,
    val deltaLb: Double,
)

data class HistoryPoint(val weekIndex: Int, val value: Double)

data class MuscleVolume(val muscle: String, val sets: Int)

class HistoryViewModel(
    private val repo: LiftingRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    val state: StateFlow<HistoryUiState> = combine(
        repo.observeCurrentProgram(),
        repo.observeAllSessions(),
        repo.observeAllPerformedSets(),
        settingsRepo.settings,
    ) { program, sessions, sets, settings ->
        val wc = weekAndCycle(settings.cycleStartedAt, program?.cycleLength ?: 1)
        if (program == null) {
            HistoryUiState.Empty.copy(cycleLabel = "Cycle ${wc.cycle} \u00B7 Week ${wc.week}")
        } else compute(program, sessions, sets, currentWeek = wc.week, cycleNumber = wc.cycle)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState.Empty,
    )

    init {
        viewModelScope.launch { repo.ensureSeeded() }
    }

    private fun compute(
        program: Program,
        sessions: List<SessionEntity>,
        sets: List<PerformedSetEntity>,
        currentWeek: Int,
        cycleNumber: Int,
    ): HistoryUiState {
        val now = System.currentTimeMillis()
        val cycleStartMs = now - TimeUnit.DAYS.toMillis(((currentWeek - 0.5) * 7).toLong())

        val cycleSessions = sessions.filter { it.date >= cycleStartMs }
        val cycleSessionIds = cycleSessions.map { it.id }.toSet()
        val cycleSets = sets.filter { it.sessionId in cycleSessionIds }

        val volume = cycleSets.sumOf { it.weight * it.reps }
        val streakDays = if (cycleSessions.isEmpty()) 0
        else {
            val earliest = cycleSessions.minOf { it.date }
            (TimeUnit.MILLISECONDS.toDays(now - earliest).toInt() + 1).coerceAtLeast(1)
        }

        val liftById = program.days.flatMap { it.lifts }.associateBy { it.id }

        // PRs = distinct lifts in current cycle whose top weight beats the prior cycle's top.
        val cycleTopByLift = cycleSets
            .groupBy { it.liftId }
            .mapValues { (_, list) -> list.maxOf { it.weight } }
        val priorSessions = sessions.filter { it.date < cycleStartMs }
        val priorSessionIds = priorSessions.map { it.id }.toSet()
        val priorTopByLift = sets
            .filter { it.sessionId in priorSessionIds }
            .groupBy { it.liftId }
            .mapValues { (_, list) -> list.maxOf { it.weight } }
        val prsThisCycle = cycleTopByLift.count { (liftId, top) ->
            val prior = priorTopByLift[liftId]
            if (prior != null) top > prior else true
        }

        // Chart: top 3 lifts by total set count (across all time), weekly top set weight.
        val sessionWeekById = sessions.associate { it.id to it.weekNumber }
        val setsByLift = sets.groupBy { it.liftId }
        val topLiftIds = setsByLift.entries
            .sortedByDescending { it.value.size }
            .take(3)
            .map { it.key }

        val seriesByLift = topLiftIds.map { liftId ->
            val name = liftById[liftId]?.name ?: liftId
            val byWeek = setsByLift[liftId].orEmpty()
                .groupBy { sessionWeekById[it.sessionId] ?: -1 }
                .filterKeys { it > 0 }
                .mapValues { (_, list) -> list.maxOf { it.weight } }
            name to byWeek
        }

        val unionWeeks = seriesByLift.flatMap { it.second.keys }.distinct().sorted()
        val chartWeeks = unionWeeks.map { "W$it" }
        val chartSeries = seriesByLift.map { (name, byWeek) ->
            val points = unionWeeks.mapIndexedNotNull { idx, week ->
                byWeek[week]?.let { HistoryPoint(weekIndex = idx, value = it) }
            }
            val delta = if (points.size >= 2) points.last().value - points.first().value else 0.0
            HistorySeries(name = name, points = points, deltaLb = delta)
        }

        // Muscle volumes from current cycle, sorted by set count, top 6.
        val muscleVolumes = cycleSets
            .mapNotNull { performed -> liftById[performed.liftId]?.muscle?.let { it to performed } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, performed) -> performed.size }
            .entries
            .sortedByDescending { it.value }
            .take(6)
            .map { MuscleVolume(it.key, it.value) }

        return HistoryUiState(
            isReady = true,
            cycleLabel = "Cycle $cycleNumber · Week $currentWeek",
            sessionsThisCycle = cycleSessions.size,
            volumeLb = volume,
            streakDays = streakDays,
            prsThisCycle = prsThisCycle,
            chartWeeks = chartWeeks,
            chartSeries = chartSeries,
            muscleVolumes = muscleVolumes,
        )
    }

    companion object {
        fun factory(repo: LiftingRepository, settingsRepo: SettingsRepository) = viewModelFactory {
            initializer { HistoryViewModel(repo, settingsRepo) }
        }
    }
}
