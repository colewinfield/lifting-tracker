package com.colewinfield.liftingtracker.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.colewinfield.liftingtracker.data.AppContainer
import com.colewinfield.liftingtracker.data.ReminderLead
import com.colewinfield.liftingtracker.data.ReminderTier
import com.colewinfield.liftingtracker.data.ReminderWorker
import com.colewinfield.liftingtracker.ui.components.LtCard
import com.colewinfield.liftingtracker.ui.components.LtCardVariant
import com.colewinfield.liftingtracker.ui.components.LtTopAppBar
import com.colewinfield.liftingtracker.ui.components.LtTopAppBarVariant
import com.colewinfield.liftingtracker.ui.theme.LiftingTrackerTheme
import com.colewinfield.liftingtracker.ui.theme.RobotoMono

@Composable
fun RemindersScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repo = remember(context) { AppContainer.repository(context) }
    val settingsRepo = remember(context) { AppContainer.settings(context) }
    val scheduler = remember(context) { AppContainer.reminderScheduler(context) }
    val viewModel: RemindersViewModel = viewModel(
        factory = RemindersViewModel.factory(repo, settingsRepo, scheduler),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Permission state — re-check whenever `permissionTick` changes (bumped after the system
    // dialog resolves). On API < 33 the helper always returns true, so this collapses to a
    // constant.
    var permissionTick by remember { mutableStateOf(0) }
    val hasPermission = remember(permissionTick) {
        ReminderWorker.hasNotificationPermission(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> permissionTick++ }

    // Auto-prompt on entry when the user already has reminders enabled but hasn't granted yet.
    // Avoids a silent-no-op situation where toggles look "on" but no notifications fire.
    LaunchedEffect(state.enabledLeads.isNotEmpty(), hasPermission) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && state.enabledLeads.isNotEmpty()
            && !hasPermission
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    RemindersContent(
        state = state,
        hasPermission = hasPermission,
        onBack = onBack,
        onToggleLead = { lead, enabled ->
            viewModel.toggleLead(lead, enabled)
            if (
                enabled
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !hasPermission
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onSelectTier = viewModel::selectTier,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemindersContent(
    state: RemindersUiState,
    hasPermission: Boolean,
    onBack: () -> Unit,
    onToggleLead: (ReminderLead, Boolean) -> Unit,
    onSelectTier: (ReminderTier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val leads = remember { ReminderLead.values().toList() }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            LtTopAppBar(
                title = "Reminders",
                subtitle = "We'll nudge you. And nudge harder if you ignore us.",
                variant = LtTopAppBarVariant.Medium,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
        ) {
            if (!hasPermission && state.enabledLeads.isNotEmpty()) {
                item { PermissionWarning() }
                item { Spacer(Modifier.height(8.dp)) }
            }
            item { SectionLabel("NUDGE SCHEDULE") }
            items(items = leads, key = { it.name }) { lead ->
                NudgeRow(
                    lead = lead,
                    enabled = lead in state.enabledLeads,
                    onCheckedChange = { checked -> onToggleLead(lead, checked) },
                )
                Spacer(Modifier.height(6.dp))
            }
            item { Spacer(Modifier.height(10.dp)) }
            item { SectionLabel("MEANNESS LEVEL") }
            item { MeannessCard(selected = state.tier, onSelect = onSelectTier) }
        }
    }
}

@Composable
private fun PermissionWarning() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.NotificationsOff,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "Notifications are blocked. Enable them in system settings to receive reminders.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(
            letterSpacing = 0.5.sp,
            fontWeight = FontWeight.Medium,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
    )
}

@Composable
private fun NudgeRow(
    lead: ReminderLead,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = lead.label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "\u201C${lead.sample}\u201D",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = enabled,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun MeannessCard(
    selected: ReminderTier,
    onSelect: (ReminderTier) -> Unit,
) {
    LtCard(modifier = Modifier.fillMaxWidth(), variant = LtCardVariant.Filled) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ReminderTier.values().forEachIndexed { index, tier ->
                MeannessRow(
                    number = (index + 1).toString(),
                    tier = tier,
                    selected = tier == selected,
                    onClick = { onSelect(tier) },
                )
            }
        }
    }
}

@Composable
private fun MeannessRow(
    number: String,
    tier: ReminderTier,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceContainerLow
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface
    val numberBg = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceContainerHigh
    val numberFg = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(numberBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = RobotoMono),
                color = numberFg,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tier.label,
                style = MaterialTheme.typography.titleSmall,
                color = contentColor,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "\u201C${tier.sample}\u201D",
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.85f),
            )
        }
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = contentColor,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF17120E, heightDp = 900)
@Composable
private fun RemindersPreview() {
    LiftingTrackerTheme(darkTheme = true, dynamicColor = false) {
        RemindersContent(
            state = RemindersUiState(
                enabledLeads = setOf(
                    ReminderLead.THREE_HOURS,
                    ReminderLead.NINETY_MIN,
                    ReminderLead.THIRTY_MIN,
                ),
                tier = ReminderTier.FIRM,
            ),
            hasPermission = true,
            onBack = {},
            onToggleLead = { _, _ -> },
            onSelectTier = {},
        )
    }
}
