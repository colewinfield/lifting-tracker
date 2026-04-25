package com.colewinfield.liftingtracker.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.colewinfield.liftingtracker.data.Alternative
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

enum class SwapTab { SAME_MUSCLE, EQUIPMENT, BROWSE_ALL }

data class SwapSheetUiState(
    val tab: SwapTab = SwapTab.SAME_MUSCLE,
    val curated: List<Alternative> = emptyList(),
    val sameMuscle: List<Alternative> = emptyList(),
    val byEquipment: List<Alternative> = emptyList(),
    val browseResults: List<Alternative> = emptyList(),
    val searchQuery: String = "",
    val loading: Boolean = false,
)

@OptIn(FlowPreview::class)
class SwapSheetViewModel(
    private val repo: LiftingRepository,
    private val liftId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(SwapSheetUiState())
    val state: StateFlow<SwapSheetUiState> = _state.asStateFlow()

    private val searchQuery = MutableStateFlow("")
    private var searchJob: Job? = null

    init {
        loadInitial()
        // Debounced search runs only while the BROWSE_ALL tab is active.
        viewModelScope.launch {
            searchQuery
                .debounce(180)
                .distinctUntilChanged()
                .collect { q -> runBrowse(q) }
        }
    }

    private fun loadInitial() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val curated = repo.alternativesFor(liftId)
            val sameMuscle = repo.catalogMatchesFor(liftId)
            val byEquip = repo.catalogByEquipmentFor(liftId)
            _state.update {
                it.copy(
                    curated = curated,
                    sameMuscle = dedupAgainstCurated(sameMuscle, curated),
                    byEquipment = dedupAgainstCurated(byEquip, curated),
                    loading = false,
                )
            }
            // Seed the Browse-all list with the first page.
            runBrowse(query = "")
        }
    }

    fun setTab(tab: SwapTab) {
        _state.update { it.copy(tab = tab) }
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
        searchQuery.value = query
    }

    private fun runBrowse(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val results = if (query.isBlank()) {
                repo.catalogPage(sourceLiftId = liftId)
            } else {
                repo.searchCatalog(query, sourceLiftId = liftId)
            }
            _state.update { it.copy(browseResults = results) }
        }
    }

    private fun dedupAgainstCurated(
        list: List<Alternative>,
        curated: List<Alternative>,
    ): List<Alternative> {
        if (curated.isEmpty()) return list
        val curatedNames = curated.mapTo(HashSet()) { it.name.lowercase() }
        return list.filterNot { curatedNames.contains(it.name.lowercase()) }
    }

    companion object {
        fun factory(repo: LiftingRepository, liftId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SwapSheetViewModel(repo, liftId) as T
            }
    }
}
