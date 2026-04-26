package com.colewinfield.liftingtracker.ui.screens

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.colewinfield.liftingtracker.data.BackupResult
import com.colewinfield.liftingtracker.data.BackupService
import com.colewinfield.liftingtracker.data.RestoreResult
import com.colewinfield.liftingtracker.data.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BackupUiState(
    /** A folder URI is stored. Independent of whether the permission is still valid. */
    val configured: Boolean,
    val folderName: String?,
    /** Permission for the stored folder URI is still held. False when the user revoked it from
     *  system settings, OR when no folder is configured at all. */
    val accessible: Boolean,
    /** Epoch millis of the most recent successful backup. 0 if never backed up. */
    val lastBackupAt: Long,
    /** A backup or restore is currently running. UI disables action buttons while true. */
    val inProgress: Boolean,
    /** True when the user just picked a folder that already contains a snapshot file AND has
     *  never been offered a restore. Drives a one-shot "found a backup, restore it?" dialog. */
    val restorePromptVisible: Boolean,
) {
    companion object {
        val Empty = BackupUiState(
            configured = false,
            folderName = null,
            accessible = false,
            lastBackupAt = 0L,
            inProgress = false,
            restorePromptVisible = false,
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModel(
    private val backup: BackupService,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    // SnackBar-style feedback. SharedFlow (not StateFlow) because each event should fire once and
    // not be re-emitted to a freshly subscribed collector after rotation.
    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val inProgress = MutableStateFlow(false)
    private val restorePromptVisible = MutableStateFlow(false)

    // Accessibility lookup is async (queries persistedUriPermissions). Run it once per URI change
    // and stash the result so the UI can disable buttons without waiting on a per-recompose IPC.
    private val accessibility: StateFlow<Boolean> = settingsRepo.settings
        .map { it.backupFolderUri }
        .distinctUntilChanged()
        .mapLatest { uri ->
            if (uri == null) false else backup.isBackupFolderAccessible()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val state: StateFlow<BackupUiState> = combine(
        settingsRepo.settings,
        accessibility,
        inProgress,
        restorePromptVisible,
    ) { settings, accessible, busy, promptVisible ->
        BackupUiState(
            configured = settings.backupFolderUri != null,
            folderName = settings.backupFolderName,
            accessible = accessible,
            lastBackupAt = settings.lastBackupAt,
            inProgress = busy,
            restorePromptVisible = promptVisible,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BackupUiState.Empty,
    )

    /**
     * Persist the URI returned by the SAF picker, then either:
     * - Offer to restore (if a snapshot already exists in this folder AND the user hasn't been
     *   offered a restore on this install yet), OR
     * - Do an immediate first backup so the file exists *now* — the user can verify it landed
     *   in Drive (or wherever) right away without waiting for the next auto-trigger.
     *
     * The restore-on-pick flow exists because SAF URI grants don't survive an app uninstall, so
     * a reinstall always lands here with no folder URI. "Pick the same folder" is the SAF
     * equivalent of "sign back in", and the snapshot file is right there waiting.
     */
    fun setBackupFolder(uri: Uri) = viewModelScope.launch {
        inProgress.value = true
        try {
            val pick = backup.setBackupFolder(uri)
            if (pick.isFailure) {
                _events.emit("Couldn't save that folder: ${pick.exceptionOrNull()?.message ?: "unknown error"}")
                return@launch
            }
            val settings = settingsRepo.settings.first()
            if (!settings.hasCheckedForBackupRestore && backup.hasExistingSnapshot()) {
                // Hand off to the dialog flow — don't backup or restore yet, the user picks.
                restorePromptVisible.value = true
                return@launch
            }
            when (val result = backup.backupNow()) {
                is BackupResult.Success -> _events.emit("Backup folder set. First backup saved.")
                is BackupResult.Skipped -> _events.emit("Backup folder set.")
                is BackupResult.Error -> _events.emit("Folder set, but first backup failed: ${result.message}")
                BackupResult.NoFolder, BackupResult.NoAccess ->
                    _events.emit("Folder set, but couldn't write a backup. Try \"Back up now\".")
            }
        } finally {
            inProgress.value = false
        }
    }

    /**
     * User accepted the "found a backup — restore it?" dialog. Restores from the file and marks
     * the prompt resolved. The local DB is wiped first inside [BackupService.restoreNow], so
     * any sample-data we seeded since reinstall gets replaced cleanly.
     */
    fun acceptRestorePrompt() = viewModelScope.launch {
        restorePromptVisible.value = false
        inProgress.value = true
        try {
            val result = backup.restoreNow()
            _events.emit(
                when (result) {
                    is RestoreResult.Success -> "Restored from backup."
                    RestoreResult.NoFolder -> "Pick a backup folder first."
                    RestoreResult.NoAccess -> "Permission lost. Re-pick the folder."
                    RestoreResult.NotFound -> "No backup file found in that folder."
                    is RestoreResult.SchemaMismatch ->
                        "Backup is from a different app version (file v${result.snapshotVersion}, app v${result.currentVersion})."
                    is RestoreResult.Error -> "Restore failed: ${result.message}"
                }
            )
        } finally {
            inProgress.value = false
        }
    }

    /**
     * User declined the "found a backup — restore it?" dialog. Marks the prompt resolved so we
     * don't re-prompt on every recompose, and writes a fresh backup from the *current* local
     * data (overwriting whatever was in the folder). Declining is essentially "treat this folder
     * as the new home for my fresh start."
     */
    fun declineRestorePrompt() = viewModelScope.launch {
        restorePromptVisible.value = false
        inProgress.value = true
        try {
            backup.dismissRestorePrompt()
            when (val result = backup.backupNow()) {
                is BackupResult.Success -> _events.emit("Existing backup overwritten with current data.")
                is BackupResult.Skipped -> _events.emit("Existing backup matches current data.")
                is BackupResult.Error -> _events.emit("Couldn't write backup: ${result.message}")
                BackupResult.NoFolder, BackupResult.NoAccess ->
                    _events.emit("Couldn't write backup. Try \"Back up now\".")
            }
        } finally {
            inProgress.value = false
        }
    }

    fun clearBackupFolder() = viewModelScope.launch {
        backup.clearBackupFolder()
        _events.emit("Backup disconnected.")
    }

    fun backupNow() = viewModelScope.launch {
        inProgress.value = true
        try {
            val result = backup.backupNow()
            _events.emit(
                when (result) {
                    is BackupResult.Success -> "Backed up."
                    is BackupResult.Skipped -> "Already up to date."
                    BackupResult.NoFolder -> "Pick a backup folder first."
                    BackupResult.NoAccess -> "Permission lost. Re-pick the folder."
                    is BackupResult.Error -> "Backup failed: ${result.message}"
                }
            )
        } finally {
            inProgress.value = false
        }
    }

    fun restoreNow() = viewModelScope.launch {
        inProgress.value = true
        try {
            val result = backup.restoreNow()
            _events.emit(
                when (result) {
                    is RestoreResult.Success -> "Restored from backup."
                    RestoreResult.NoFolder -> "Pick a backup folder first."
                    RestoreResult.NoAccess -> "Permission lost. Re-pick the folder."
                    RestoreResult.NotFound -> "No backup file found in that folder."
                    is RestoreResult.SchemaMismatch ->
                        "Backup is from a different app version (file v${result.snapshotVersion}, app v${result.currentVersion})."
                    is RestoreResult.Error -> "Restore failed: ${result.message}"
                }
            )
        } finally {
            inProgress.value = false
        }
    }

    companion object {
        fun factory(backup: BackupService, settingsRepo: SettingsRepository) = viewModelFactory {
            initializer { BackupViewModel(backup, settingsRepo) }
        }
    }
}
