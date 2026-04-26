package com.colewinfield.liftingtracker.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.colewinfield.liftingtracker.data.CatalogLift
import com.colewinfield.liftingtracker.data.LiftingRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Filter taxonomy for the Exercise DB picker. The labels match `extras2.jsx`'s `filters` array
 * exactly; [normalizedMuscle] is the lowercase token used in the catalog table (see
 * [com.colewinfield.liftingtracker.data.CatalogSeeder.normalizeMuscle]).
 *
 * `null` for [normalizedMuscle] means "no muscle filter" (the All chip).
 */
enum class ExerciseDbFilter(val label: String, val normalizedMuscle: String?) {
    ALL("All", null),
    CHEST("Chest", "chest"),
    BACK("Back", "lats"),
    QUADS("Quads", "quadriceps"),
    HAMSTRINGS("Hamstrings", "hamstrings"),
    SHOULDERS("Shoulders", "shoulders"),
    BICEPS("Biceps", "biceps"),
    TRICEPS("Triceps", "triceps"),
}

data class ExerciseDbUiState(
    val query: String = "",
    val filter: ExerciseDbFilter = ExerciseDbFilter.ALL,
    val results: List<CatalogLift> = emptyList(),
    val loading: Boolean = false,
)

@OptIn(FlowPreview::class)
class ExerciseDBViewModel(
    private val repo: LiftingRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ExerciseDbUiState())
    val state: StateFlow<ExerciseDbUiState> = _state.asStateFlow()

    private val querySignal = MutableStateFlow("")
    private var loadJob: Job? = null

    init {
        load()
        // Debounce free-text search so we don't hammer the DAO on every keystroke. Filter chip
        // changes flush immediately via [setFilter] -> [load].
        viewModelScope.launch {
            querySignal
                .debounce(180)
                .distinctUntilChanged()
                .collect { load() }
        }
    }

    fun setQuery(value: String) {
        _state.update { it.copy(query = value) }
        querySignal.value = value
    }

    fun setFilter(filter: ExerciseDbFilter) {
        if (_state.value.filter == filter) return
        _state.update { it.copy(filter = filter) }
        load()
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val s = _state.value
            val results = repo.browseCatalog(
                query = s.query,
                primaryMuscle = s.filter.normalizedMuscle,
            )
            _state.update { it.copy(results = results, loading = false) }
        }
    }

    companion object {
        fun factory(repo: LiftingRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ExerciseDBViewModel(repo) as T
            }
    }
}
