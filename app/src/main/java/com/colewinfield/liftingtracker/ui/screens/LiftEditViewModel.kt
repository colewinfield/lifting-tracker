package com.colewinfield.liftingtracker.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.colewinfield.liftingtracker.data.Effort
import com.colewinfield.liftingtracker.data.LiftingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LiftEditUiState(
    val loaded: Boolean = false,
    val isNew: Boolean = false,
    val liftId: String = "",
    val dayId: String = "",
    val name: String = "",
    val muscle: String = "",
    val equipment: String = "",
    val setsMin: Int = 3,
    val setsMax: Int = 3,
    val repsMin: Int = 8,
    val repsMax: Int = 10,
    val effort: Effort = Effort.MED,
)

class LiftEditViewModel(
    private val repo: LiftingRepository,
    private val liftId: String,
    private val dayId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(LiftEditUiState())
    val state: StateFlow<LiftEditUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (liftId == NEW_ID) {
                _state.value = LiftEditUiState(
                    loaded = true,
                    isNew = true,
                    liftId = NEW_ID,
                    dayId = dayId,
                    name = "New lift",
                    muscle = "",
                    equipment = "",
                    setsMin = 3,
                    setsMax = 3,
                    repsMin = 8,
                    repsMax = 10,
                    effort = Effort.MED,
                )
            } else {
                val lift = repo.getLift(liftId)
                if (lift == null) {
                    _state.value = LiftEditUiState(loaded = false)
                } else {
                    _state.value = LiftEditUiState(
                        loaded = true,
                        isNew = false,
                        liftId = lift.id,
                        dayId = dayId,
                        name = lift.name,
                        muscle = lift.muscle,
                        equipment = lift.equipment,
                        setsMin = lift.sets.first,
                        setsMax = lift.sets.last,
                        repsMin = lift.reps.first,
                        repsMax = lift.reps.last,
                        effort = lift.effort,
                    )
                }
            }
        }
    }

    fun setIdentity(name: String, muscle: String, equipment: String) = _state.update {
        it.copy(
            name = name.trim().ifBlank { "Lift" },
            muscle = muscle.trim(),
            equipment = equipment.trim(),
        )
    }

    fun setSetsMin(value: Int) = _state.update {
        val newMin = value.coerceAtLeast(1)
        it.copy(setsMin = newMin, setsMax = it.setsMax.coerceAtLeast(newMin))
    }

    fun setSetsMax(value: Int) = _state.update {
        val newMax = value.coerceAtLeast(1)
        it.copy(setsMax = newMax, setsMin = it.setsMin.coerceAtMost(newMax))
    }

    fun setRepsMin(value: Int) = _state.update {
        val newMin = value.coerceAtLeast(1)
        it.copy(repsMin = newMin, repsMax = it.repsMax.coerceAtLeast(newMin))
    }

    fun setRepsMax(value: Int) = _state.update {
        val newMax = value.coerceAtLeast(1)
        it.copy(repsMax = newMax, repsMin = it.repsMin.coerceAtMost(newMax))
    }

    fun setEffort(effort: Effort) = _state.update { it.copy(effort = effort) }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (!s.loaded) return
        viewModelScope.launch {
            if (s.isNew) {
                repo.addLift(
                    dayId = s.dayId,
                    name = s.name.trim().ifBlank { "Lift" },
                    setsMin = s.setsMin,
                    setsMax = s.setsMax,
                    repsMin = s.repsMin,
                    repsMax = s.repsMax,
                    effort = s.effort,
                    muscle = s.muscle.trim(),
                    equipment = s.equipment.trim(),
                )
            } else {
                repo.updateLift(
                    liftId = s.liftId,
                    name = s.name.trim().ifBlank { "Lift" },
                    setsMin = s.setsMin,
                    setsMax = s.setsMax,
                    repsMin = s.repsMin,
                    repsMax = s.repsMax,
                    effort = s.effort,
                    muscle = s.muscle.trim(),
                    equipment = s.equipment.trim(),
                )
            }
            onDone()
        }
    }

    fun deleteLift(onDone: () -> Unit) {
        val s = _state.value
        if (s.isNew || s.liftId.isBlank()) {
            onDone()
            return
        }
        viewModelScope.launch {
            repo.deleteLift(s.liftId)
            onDone()
        }
    }

    companion object {
        fun factory(
            repo: LiftingRepository,
            liftId: String,
            dayId: String,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LiftEditViewModel(repo, liftId, dayId) as T
            }
    }
}
