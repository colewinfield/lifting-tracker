package com.colewinfield.liftingtracker.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.colewinfield.liftingtracker.data.AppContainer
import com.colewinfield.liftingtracker.ui.components.LtButton
import com.colewinfield.liftingtracker.ui.components.LtButtonSize
import com.colewinfield.liftingtracker.ui.components.LtButtonVariant
import com.colewinfield.liftingtracker.ui.components.LtCard
import com.colewinfield.liftingtracker.ui.components.LtCardVariant
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

@Composable
fun BackupSection(
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val service = remember(context) { AppContainer.backup(context) }
    val settingsRepo = remember(context) { AppContainer.settings(context) }
    val viewModel: BackupViewModel = viewModel(
        factory = BackupViewModel.factory(service, settingsRepo),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Wire VM events through to the parent's snackbar host. Re-collected on VM identity, which
    // is stable across recompositions thanks to viewModel(...) caching.
    LaunchedEffect(viewModel) {
        viewModel.events.collect { onShowMessage(it) }
    }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        // Null when the user cancels the picker — silent ignore. The contract handles its own
        // takePersistableUriPermission grant flags; setBackupFolder also calls
        // takePersistableUriPermission so the grant survives reboots.
        if (uri != null) viewModel.setBackupFolder(uri)
    }

    var showRestoreConfirm by rememberSaveable { mutableStateOf(false) }
    var showDisconnectConfirm by rememberSaveable { mutableStateOf(false) }

    BackupSectionContent(
        state = state,
        onPickFolder = { pickerLauncher.launch(null) },
        onBackupNow = viewModel::backupNow,
        onRestoreClick = { showRestoreConfirm = true },
        onDisconnectClick = { showDisconnectConfirm = true },
        modifier = modifier,
    )

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restore from backup?") },
            text = {
                Text(
                    "This replaces all of your current data with what's in the backup file. " +
                        "Anything you've logged on this device since the last backup will be lost.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreConfirm = false
                    viewModel.restoreNow()
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text("Cancel") }
            },
        )
    }

    if (showDisconnectConfirm) {
        AlertDialog(
            onDismissRequest = { showDisconnectConfirm = false },
            title = { Text("Disconnect backup?") },
            text = {
                Text(
                    "Your backup file stays put — this just stops automatic backups and forgets " +
                        "the folder. You can reconnect anytime.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDisconnectConfirm = false
                    viewModel.clearBackupFolder()
                }) { Text("Disconnect") }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectConfirm = false }) { Text("Cancel") }
            },
        )
    }

    // Auto-shown when setBackupFolder finds an existing snapshot AND the user hasn't been asked
    // about restoring yet on this install. This is the SAF-equivalent of the "first launch after
    // reinstall" prompt — there's no folder URI before the first pick, so we can't ask any
    // sooner. Dismissing this dialog without the user choosing would silently overwrite their
    // backup, which is why this isn't dismissable on outside-tap or back press.
    if (state.restorePromptVisible) {
        AlertDialog(
            onDismissRequest = { /* require an explicit choice */ },
            title = { Text("Backup found in this folder") },
            text = {
                Text(
                    "There's already a lifting-tracker backup in this folder. Restore it now? " +
                        "Choosing \"Use fresh data\" will overwrite the existing backup with whatever " +
                        "is currently on this device.",
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::acceptRestorePrompt) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::declineRestorePrompt) { Text("Use fresh data") }
            },
        )
    }
}

@Composable
private fun BackupSectionContent(
    state: BackupUiState,
    onPickFolder: () -> Unit,
    onBackupNow: () -> Unit,
    onRestoreClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "BACKUP",
            style = MaterialTheme.typography.titleSmall.copy(
                letterSpacing = 0.5.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        )
        LtCard(modifier = Modifier.fillMaxWidth(), variant = LtCardVariant.Filled) {
            Column(modifier = Modifier.padding(16.dp)) {
                BackupStatus(state = state)
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(16.dp))
                BackupActions(
                    state = state,
                    onPickFolder = onPickFolder,
                    onBackupNow = onBackupNow,
                    onRestoreClick = onRestoreClick,
                    onDisconnectClick = onDisconnectClick,
                )
            }
        }
    }
}

@Composable
private fun BackupStatus(state: BackupUiState) {
    val (icon, tint, headline, sub) = when {
        !state.configured -> StatusVisual(
            icon = Icons.Default.CloudOff,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            headline = "No backup folder set",
            sub = "Pick a folder (in Drive, or anywhere) and your lifts will be saved there.",
        )
        !state.accessible -> StatusVisual(
            icon = Icons.Default.ErrorOutline,
            tint = MaterialTheme.colorScheme.error,
            headline = "Permission revoked",
            sub = "Re-pick the folder to keep backups going.",
        )
        else -> StatusVisual(
            icon = Icons.Default.Cloud,
            tint = MaterialTheme.colorScheme.primary,
            headline = "Backing up to ${state.folderName ?: "your chosen folder"}",
            sub = formatLastBackup(state.lastBackupAt),
        )
    }

    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = headline,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BackupActions(
    state: BackupUiState,
    onPickFolder: () -> Unit,
    onBackupNow: () -> Unit,
    onRestoreClick: () -> Unit,
    onDisconnectClick: () -> Unit,
) {
    if (state.inProgress) {
        // Single in-flight action across all buttons; show a spinner so taps don't double-fire.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Working…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    when {
        !state.configured || !state.accessible -> {
            LtButton(
                onClick = onPickFolder,
                variant = LtButtonVariant.Filled,
                size = LtButtonSize.Md,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (state.configured) "Re-pick folder" else "Set up backup") }
        }
        else -> {
            // Configured + accessible: full set of actions.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LtButton(
                        onClick = onBackupNow,
                        variant = LtButtonVariant.Filled,
                        size = LtButtonSize.Md,
                        modifier = Modifier.weight(1f),
                    ) { Text("Back up now") }
                    LtButton(
                        onClick = onRestoreClick,
                        variant = LtButtonVariant.Tonal,
                        size = LtButtonSize.Md,
                        modifier = Modifier.weight(1f),
                    ) { Text("Restore") }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LtButton(
                        onClick = onPickFolder,
                        variant = LtButtonVariant.Outlined,
                        size = LtButtonSize.Md,
                        modifier = Modifier.weight(1f),
                    ) { Text("Change folder") }
                    LtButton(
                        onClick = onDisconnectClick,
                        variant = LtButtonVariant.Text,
                        size = LtButtonSize.Md,
                        modifier = Modifier.weight(1f),
                    ) { Text("Disconnect") }
                }
            }
        }
    }
}

private data class StatusVisual(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: androidx.compose.ui.graphics.Color,
    val headline: String,
    val sub: String,
)

/**
 * Human-readable "Last backup …" sub-line. Sub-minute ages render as "just now" so we don't show
 * jittery seconds counters; older timestamps fall through to a date+time string from the user's
 * locale.
 */
private fun formatLastBackup(epochMillis: Long): String {
    if (epochMillis <= 0L) return "Never backed up yet."
    val now = System.currentTimeMillis()
    val delta = now - epochMillis
    if (delta < 0) return "Backed up at " + DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(epochMillis))

    val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
    val hours = TimeUnit.MILLISECONDS.toHours(delta)
    val days = TimeUnit.MILLISECONDS.toDays(delta)

    return when {
        minutes < 1L -> "Last backup: just now."
        minutes < 60L -> "Last backup: ${minutes}m ago."
        hours < 24L -> "Last backup: ${hours}h ago."
        days < 7L -> "Last backup: ${days}d ago."
        else -> "Last backup: " +
            DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(epochMillis)) + "."
    }
}
