package com.colewinfield.liftingtracker.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.colewinfield.liftingtracker.data.AppContainer
import com.colewinfield.liftingtracker.data.ThemeMode
import com.colewinfield.liftingtracker.data.WeightUnit
import com.colewinfield.liftingtracker.ui.components.LtCard
import com.colewinfield.liftingtracker.ui.components.LtCardVariant
import com.colewinfield.liftingtracker.ui.components.LtTopAppBar
import com.colewinfield.liftingtracker.ui.components.LtTopAppBarVariant
import com.colewinfield.liftingtracker.ui.theme.LiftingTrackerTheme
import com.colewinfield.liftingtracker.ui.theme.RobotoMono

@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repo = remember(context) { AppContainer.repository(context) }
    val settingsRepo = remember(context) { AppContainer.settings(context) }
    val viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(repo, settingsRepo))
    val state by viewModel.state.collectAsStateWithLifecycle()

    ProfileContent(
        state = state,
        onToggleUnit = viewModel::toggleUnit,
        onSetThemeMode = viewModel::setThemeMode,
        onSetDynamicColor = viewModel::setDynamicColor,
        onSaveProfile = viewModel::saveProfile,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileContent(
    state: ProfileUiState,
    onToggleUnit: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onSaveProfile: (name: String, bodyweight: Double, heightInches: Int, age: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showEditSheet by rememberSaveable { mutableStateOf(false) }
    val openEdit = { showEditSheet = true }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            LtTopAppBar(
                title = "Profile",
                variant = LtTopAppBarVariant.Small,
                actions = {
                    IconButton(onClick = openEdit) {
                        Icon(Icons.Default.Settings, contentDescription = "Edit profile")
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
                bottom = padding.calculateBottomPadding() + 80.dp,
            ),
        ) {
            item { ProfileHeader(state = state, onEdit = openEdit) }
            item { BodyStatsRow(state = state, onEdit = openEdit) }
            item { Spacer(Modifier.height(16.dp)) }
            item { SettingsSectionLabel() }
            item {
                SettingsCard(
                    state = state,
                    onToggleUnit = onToggleUnit,
                    onSetThemeMode = onSetThemeMode,
                    onSetDynamicColor = onSetDynamicColor,
                )
            }
        }
    }

    if (showEditSheet) {
        EditProfileSheet(
            initialName = state.displayName,
            initialBodyweight = state.bodyweight,
            initialHeightInches = state.heightInches,
            initialAge = state.age,
            unit = state.unit,
            onClose = { showEditSheet = false },
            onSave = { name, bw, h, a ->
                onSaveProfile(name, bw, h, a)
                showEditSheet = false
            },
        )
    }
}

@Composable
private fun ProfileHeader(state: ProfileUiState, onEdit: () -> Unit) {
    val initial = state.displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onEdit)
            .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 20.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = state.displayName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${state.sessionCount} sessions \u00B7 cycle ${state.cycleNumber}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BodyStatsRow(state: ProfileUiState, onEdit: () -> Unit) {
    val bwDisplay = if (state.bodyweight > 0) {
        "${formatBodyweight(state.bodyweight)} ${if (state.unit == WeightUnit.LB) "lb" else "kg"}"
    } else "—"
    val heightDisplay = if (state.heightInches > 0) {
        "${state.heightInches / 12}'${state.heightInches % 12}\""
    } else "—"
    val ageDisplay = if (state.age > 0) state.age.toString() else "—"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BodyStatTile(label = "BW", value = bwDisplay, modifier = Modifier.weight(1f), onClick = onEdit)
        BodyStatTile(label = "HEIGHT", value = heightDisplay, modifier = Modifier.weight(1f), onClick = onEdit)
        BodyStatTile(label = "AGE", value = ageDisplay, modifier = Modifier.weight(1f), onClick = onEdit)
    }
}

@Composable
private fun BodyStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    LtCard(modifier = modifier, variant = LtCardVariant.Filled, onClick = onClick) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = RobotoMono),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SettingsSectionLabel() {
    Text(
        text = "SETTINGS",
        style = MaterialTheme.typography.titleSmall.copy(
            letterSpacing = 0.5.sp,
            fontWeight = FontWeight.Medium,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsCard(
    state: ProfileUiState,
    onToggleUnit: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
) {
    LtCard(modifier = Modifier.fillMaxWidth(), variant = LtCardVariant.Filled) {
        Column(modifier = Modifier.padding(4.dp)) {
            SettingRow(
                icon = Icons.Default.CalendarMonth,
                label = "Program",
                trailingText = state.programName,
                onClick = { /* TODO: open program select */ },
            )
            SettingRow(
                icon = Icons.Default.Notifications,
                label = "Reminders",
                trailingText = "Firm",
                onClick = { /* TODO: open reminders */ },
            )
            SettingRow(
                icon = Icons.Default.FitnessCenter,
                label = "Units",
                trailingText = if (state.unit == WeightUnit.LB) "Pounds" else "Kilograms",
                onClick = onToggleUnit,
            )
            ThemeModeRow(
                selected = state.themeMode,
                onSelect = onSetThemeMode,
            )
            SettingRow(
                icon = Icons.Default.Palette,
                label = "Material You",
                control = {
                    Switch(
                        checked = state.useDynamicColor,
                        onCheckedChange = onSetDynamicColor,
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeRow(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    val options = listOf(
        ThemeMode.LIGHT to "Light",
        ThemeMode.SYSTEM to "Auto",
        ThemeMode.DARK to "Dark",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.DarkMode,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = "Theme",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (mode, label) ->
                SegmentedButton(
                    selected = mode == selected,
                    onClick = { onSelect(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                ) {
                    Text(label)
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    label: String,
    trailingText: String? = null,
    control: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 56.dp)
        .let { if (onClick != null && control == null) it.clickable { onClick() } else it }
        .padding(horizontal = 12.dp, vertical = 12.dp)

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (control != null) {
            Spacer(Modifier.width(8.dp))
            control()
        } else {
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatBodyweight(w: Double): String =
    if (w % 1.0 == 0.0) w.toInt().toString() else "%.1f".format(w)

@Preview(showBackground = true, backgroundColor = 0xFF17120E, heightDp = 900)
@Composable
private fun ProfileScreenPreview() {
    LiftingTrackerTheme(darkTheme = true, dynamicColor = false) {
        ProfileContent(
            state = ProfileUiState(
                programName = "Upper/Lower Hybrid",
                sessionCount = 24,
                cycleNumber = 4,
                unit = WeightUnit.LB,
                themeMode = ThemeMode.DARK,
                useDynamicColor = false,
                currentWeek = 4,
                cycleLength = 9,
                displayName = "Cole",
                bodyweight = 185.0,
                heightInches = 71,
                age = 28,
            ),
            onToggleUnit = {},
            onSetThemeMode = {},
            onSetDynamicColor = {},
            onSaveProfile = { _, _, _, _ -> },
        )
    }
}
