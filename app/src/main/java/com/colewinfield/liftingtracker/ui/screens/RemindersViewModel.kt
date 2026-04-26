package com.colewinfield.liftingtracker.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.colewinfield.liftingtracker.data.LiftingRepository
import com.colewinfield.liftingtracker.data.ReminderLead
import com.colewinfield.liftingtracker.data.ReminderScheduler
import com.colewinfield.liftingtracker.data.ReminderTier
import com.colewinfield.liftingtracker.data.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives [RemindersScreen]. Exposes only the toggleable bits — enabled leads + tier — derived
 * from settings. The current [com.colewinfield.liftingtracker.data.Program] isn't part of the
 * UI state because the screen doesn't render it; we re-read program on each mutation so the
 * scheduler call sees the latest workout-day shape.
 *
 * Cold-start reconciliation lives in
 * [com.colewinfield.liftingtracker.LiftingTrackerApplication], so this VM only handles runtime
 * mutations: toggle a lead → persist + re-apply scheduling; select a tier → persist (the worker
 * reads tier at fire time, so no rescheduling needed).
 */
data class RemindersUiState(
    val enabledLeads: Set<ReminderLead>,
    val tier: ReminderTier,
)

@OptIn(ExperimentalCoroutinesApi::class)
class RemindersViewModel(
    private val repo: LiftingRepository,
    private val settingsRepo: SettingsRepository,
    private val scheduler: ReminderScheduler,
) : ViewModel() {

    val state = settingsRepo.settings.map { settings ->
        RemindersUiState(
            enabledLeads = settings.reminderEnabledLeads
                .mapNotNull { runCatching { ReminderLead.valueOf(it) }.getOrNull() }
                .toSet(),
            tier = settings.reminderTier,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RemindersUiState(
            enabledLeads = emptySet(),
            tier = ReminderTier.FIRM,
        ),
    )

    fun toggleLead(lead: ReminderLead, enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsRepo.settings.first()
            val nextLeads = current.reminderEnabledLeads.toMutableSet().apply {
                if (enabled) add(lead.name) else remove(lead.name)
            }
            settingsRepo.setReminderEnabledLeads(nextLeads)
            val program = repo.observeCurrentProgram().first()
            scheduler.applyReminders(program, settingsRepo.settings.first())
        }
    }

    fun selectTier(tier: ReminderTier) {
        viewModelScope.launch { settingsRepo.setReminderTier(tier) }
    }

    companion object {
        fun factory(
            repo: LiftingRepository,
            settingsRepo: SettingsRepository,
            scheduler: ReminderScheduler,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                RemindersViewModel(repo, settingsRepo, scheduler) as T
        }
    }
}
