package com.colewinfield.liftingtracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.colewinfield.liftingtracker.data.AppContainer
import com.colewinfield.liftingtracker.data.Day
import com.colewinfield.liftingtracker.data.Effort
import com.colewinfield.liftingtracker.ui.components.EffortDot
import com.colewinfield.liftingtracker.ui.components.LtButton
import com.colewinfield.liftingtracker.ui.components.LtButtonSize
import com.colewinfield.liftingtracker.ui.components.LtButtonVariant
import com.colewinfield.liftingtracker.ui.components.LtCard
import com.colewinfield.liftingtracker.ui.components.LtCardVariant
import com.colewinfield.liftingtracker.ui.theme.RobotoMono

@Composable
fun ProgramEditScreen(
    onBack: () -> Unit,
    onOpenDay: (dayId: String) -> Unit,
    onAddDay: () -> Unit,
) {
    val context = LocalContext.current
    val repo = remember(context) { AppContainer.repository(context) }
    val viewModel: ProgramEditViewModel = viewModel(factory = ProgramEditViewModel.factory(repo))
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showEditName by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top)),
    ) {
        EditTopRow(
            title = "Edit program",
            onBack = onBack,
            onSave = { viewModel.save(onBack) },
            saveEnabled = state.loaded,
        )

        if (!state.loaded) return@Column

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 100.dp)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
        ) {
            // Name
            EditSectionLabel("PROGRAM NAME", topPadding = 12)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { showEditName = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit name",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }

            // Cycle length
            EditSectionLabel("CYCLE LENGTH", topPadding = 20)
            CycleLengthCard(
                value = state.cycleLength,
                onChange = viewModel::setCycleLength,
            )

            // Deload week
            EditSectionLabel("DELOAD WEEK", topPadding = 20)
            DeloadModeRow(
                mode = state.deloadMode,
                cycleLength = state.cycleLength,
                onSelect = viewModel::setDeloadMode,
            )
            if (state.deloadMode == DeloadMode.CUSTOM) {
                Spacer(Modifier.height(8.dp))
                CustomDeloadWeekRow(
                    week = state.customDeloadWeek.coerceIn(1, state.cycleLength),
                    cycleLength = state.cycleLength,
                    onChange = viewModel::setCustomDeloadWeek,
                )
            }
            Text(
                text = "50% working weight \u00B7 same reps \u00B7 4 RIR",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
            )

            // Training days
            Row(
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 24.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "TRAINING DAYS \u00B7 ${state.days.count { !it.isRest }}",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                LtButton(
                    onClick = onAddDay,
                    variant = LtButtonVariant.Text,
                    size = LtButtonSize.Sm,
                    icon = Icons.Default.Add,
                ) {
                    Text("Add day")
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.days.forEachIndexed { index, day ->
                    ProgramEditDayRow(
                        day = day,
                        number = index + 1,
                        onClick = { onOpenDay(day.id) },
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
            LtButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                variant = LtButtonVariant.Error,
                icon = Icons.Default.Delete,
            ) {
                Text("Delete program")
            }
        }
    }

    if (showEditName) {
        EditNameDialog(
            initial = state.name,
            onClose = { showEditName = false },
            onSave = { newName ->
                viewModel.setName(newName)
                showEditName = false
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete program?") },
            text = {
                Text(
                    "This will remove all days, lifts, and history. " +
                        "A fresh sample program will be seeded on next launch.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteProgram(onBack)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
internal fun EditTopRow(
    title: String,
    onBack: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )
        TextButton(onClick = onSave, enabled = saveEnabled) {
            Text("Save")
        }
    }
}

@Composable
internal fun EditSectionLabel(text: String, topPadding: Int = 16) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.5.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            start = 4.dp, end = 4.dp,
            top = topPadding.dp, bottom = 6.dp,
        ),
    )
}

@Composable
private fun CycleLengthCard(value: Int, onChange: (Int) -> Unit) {
    LtCard(modifier = Modifier.fillMaxWidth(), variant = LtCardVariant.Filled) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StepperButton(
                icon = Icons.Default.Remove,
                onClick = { onChange((value - 1).coerceAtLeast(1)) },
                enabled = value > 1,
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = RobotoMono,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "WEEKS",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.8.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StepperButton(
                icon = Icons.Default.Add,
                onClick = { onChange(value + 1) },
                enabled = value < 52,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeloadModeRow(
    mode: DeloadMode,
    cycleLength: Int,
    onSelect: (DeloadMode) -> Unit,
) {
    val options = listOf(
        DeloadMode.NONE to "None",
        DeloadMode.LAST to "Week $cycleLength",
        DeloadMode.CUSTOM to "Custom",
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (option, label) ->
            SegmentedButton(
                selected = option == mode,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) {
                Text(label)
            }
        }
    }
}

@Composable
private fun CustomDeloadWeekRow(
    week: Int,
    cycleLength: Int,
    onChange: (Int) -> Unit,
) {
    LtCard(modifier = Modifier.fillMaxWidth(), variant = LtCardVariant.Filled) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "DELOAD ON WEEK",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            StepperButton(
                icon = Icons.Default.Remove,
                onClick = { onChange((week - 1).coerceAtLeast(1)) },
                enabled = week > 1,
            )
            Text(
                text = week.toString(),
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = RobotoMono),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(40.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            StepperButton(
                icon = Icons.Default.Add,
                onClick = { onChange((week + 1).coerceAtMost(cycleLength)) },
                enabled = week < cycleLength,
            )
        }
    }
}

@Composable
internal fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val tint =
        if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun ProgramEditDayRow(
    day: Day,
    number: Int,
    onClick: () -> Unit,
) {
    if (day.isRest) {
        val outline = MaterialTheme.colorScheme.outlineVariant
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .drawBehind {
                    val stroke = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(8.dp.toPx(), 6.dp.toPx())
                        ),
                    )
                    drawRoundRect(
                        color = outline,
                        size = size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                        style = stroke,
                    )
                }
                .clickable(onClick = onClick)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Rest",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = day.dayOfWeek.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        return
    }
    LtCard(
        modifier = Modifier.fillMaxWidth(),
        variant = LtCardVariant.Filled,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = day.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = day.dayOfWeek.short,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "${day.lifts.size} lifts \u00B7 ${day.focus}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                if (day.lifts.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        day.lifts
                            .sortedBy {
                                when (it.effort) {
                                    Effort.HIGH -> 0
                                    Effort.MED -> 1
                                    Effort.LOW -> 2
                                }
                            }
                            .forEach { lift ->
                                EffortDot(effort = lift.effort, size = 6.dp)
                            }
                    }
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
