package com.colewinfield.liftingtracker.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.colewinfield.liftingtracker.data.Alternative
import com.colewinfield.liftingtracker.data.HistoryEntry
import com.colewinfield.liftingtracker.data.Lift
import com.colewinfield.liftingtracker.data.LiftingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DetailTab(val label: String) {
    History("HISTORY"),
    Graph("GRAPH"),
    HowTo("HOW-TO"),
    Alts("ALTS");

    companion object {
        fun fromKey(key: String?): DetailTab = when (key) {
            "graph" -> Graph
            "howto" -> HowTo
            "alts" -> Alts
            else -> History
        }

        fun toKey(tab: DetailTab): String = when (tab) {
            History -> "history"
            Graph -> "graph"
            HowTo -> "howto"
            Alts -> "alts"
        }
    }
}

data class ExerciseDetailUiState(
    val isReady: Boolean,
    val lift: Lift?,
    val tab: DetailTab,
    val history: List<HistoryEntry>,
    val alternatives: List<Alternative>,
    val bestWeight: Double,
    val sessionsCount: Int,
    val estimatedOneRm: Double,
) {
    companion object {
        val Empty = ExerciseDetailUiState(
            isReady = false,
            lift = null,
            tab = DetailTab.History,
            history = emptyList(),
            alternatives = emptyList(),
            bestWeight = 0.0,
            sessionsCount = 0,
            estimatedOneRm = 0.0,
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseDetailViewModel(
    private val repo: LiftingRepository,
    private val liftId: String,
    initialTab: DetailTab,
) : ViewModel() {

    private val tab = MutableStateFlow(initialTab)
    private val history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    private val alternatives = MutableStateFlow<List<Alternative>>(emptyList())

    val state: StateFlow<ExerciseDetailUiState> = combine(
        repo.observeCurrentProgram(),
        tab,
        history,
        alternatives,
    ) { program, currentTab, hist, alts ->
        val lift = program?.days?.flatMap { it.lifts }?.firstOrNull { it.id == liftId }
        val best = hist.flatMap { it.sets }.maxOfOrNull { it.weight } ?: 0.0
        val mostRecentSets = hist.firstOrNull()?.sets.orEmpty()
        val estOneRm = mostRecentSets
            .maxOfOrNull { it.weight * (1.0 + it.reps / 30.0) } ?: 0.0
        ExerciseDetailUiState(
            isReady = program != null,
            lift = lift,
            tab = currentTab,
            history = hist,
            alternatives = alts,
            bestWeight = best,
            sessionsCount = hist.size,
            estimatedOneRm = estOneRm,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExerciseDetailUiState.Empty.copy(tab = initialTab),
    )

    init {
        viewModelScope.launch {
            repo.ensureSeeded()
            history.value = repo.historyForLift(liftId)
            alternatives.value = repo.alternativesFor(liftId)
        }
    }

    fun selectTab(t: DetailTab) {
        tab.value = t
    }

    companion object {
        fun factory(repo: LiftingRepository, liftId: String, initialTab: DetailTab) =
            viewModelFactory {
                initializer { ExerciseDetailViewModel(repo, liftId, initialTab) }
            }
    }
}
