package com.colewinfield.liftingtracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
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
import androidx.lifecycle.SavedStateHandle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.colewinfield.liftingtracker.data.AppContainer
import com.colewinfield.liftingtracker.data.Effort
import com.colewinfield.liftingtracker.ui.components.LtButton
import com.colewinfield.liftingtracker.ui.components.LtButtonVariant
import com.colewinfield.liftingtracker.ui.components.LtCard
import com.colewinfield.liftingtracker.ui.components.LtCardVariant
import com.colewinfield.liftingtracker.ui.theme.RobotoMono
import com.colewinfield.liftingtracker.ui.theme.appColors

/**
 * Keys used to hand a picked catalog lift back from the [ExerciseDBScreen] picker route.
 * The picker writes these into the previous backstack entry's `SavedStateHandle`; this screen's
 * [LaunchedEffect] reads + clears them, then forwards into the VM.
 */
object ExercisePickerResult {
    const val NAME_KEY = "exercise_picker_name"
    const val MUSCLE_KEY = "exercise_picker_muscle"
    const val EQUIPMENT_KEY = "exercise_picker_equipment"
}

@Composable
fun LiftEditScreen(
    liftId: String,
    dayId: String,
    onBack: () -> Unit,
    onPickExercise: () -> Unit,
    pickerResultHandle: SavedStateHandle?,
) {
    val context = LocalContext.current
    val repo = remember(context) { AppContainer.repository(context) }
    val viewModel: LiftEditViewModel = viewModel(
        key = "lift-edit-$liftId-$dayId",
        factory = LiftEditViewModel.factory(repo, liftId, dayId),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    // Observe the picker result via the SavedStateHandle's StateFlow — direct writes don't
    // recompose consumers on their own. Guard on state.loaded so the apply doesn't race with
    // the VM's initial DB load (which would otherwise clobber the pick). Clear all three keys
    // after applying so a back-then-forward navigation doesn't re-apply stale data.
    LaunchedEffect(pickerResultHandle, state.loaded) {
        val handle = pickerResultHandle ?: return@LaunchedEffect
        if (!state.loaded) return@LaunchedEffect
        handle.getStateFlow<String?>(ExercisePickerResult.NAME_KEY, null)
            .collect { name ->
                if (name.isNullOrBlank()) return@collect
                val muscle = handle.get<String>(ExercisePickerResult.MUSCLE_KEY).orEmpty()
                val equipment = handle.get<String>(ExercisePickerResult.EQUIPMENT_KEY).orEmpty()
                viewModel.setIdentity(name, muscle, equipment)
                handle.remove<String>(ExercisePickerResult.NAME_KEY)
                handle.remove<String>(ExercisePickerResult.MUSCLE_KEY)
                handle.remove<String>(ExercisePickerResult.EQUIPMENT_KEY)
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top)),
    ) {
        EditTopRow(
            title = if (state.isNew) "New lift" else "Edit lift",
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
            // Exercise picker — taps push the ExerciseDBScreen route, which writes the picked
            // catalog lift back through pickerResultHandle (consumed by the LaunchedEffect above).
            EditSectionLabel("EXERCISE", topPadding = 12)
            ExerciseIdentityCard(
                name = state.name,
                muscle = state.muscle,
                equipment = state.equipment,
                onClick = onPickExercise,
            )

            // Set range
            EditSectionLabel("SET RANGE", topPadding = 20)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RangeStepperCard(
                    label = "MIN",
                    value = state.setsMin,
                    onChange = viewModel::setSetsMin,
                    modifier = Modifier.weight(1f),
                )
                RangeStepperCard(
                    label = "MAX",
                    value = state.setsMax,
                    onChange = viewModel::setSetsMax,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = "Start at ${state.setsMin}, add sets as you build up",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
            )

            // Rep range
            EditSectionLabel("REP RANGE", topPadding = 20)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RangeStepperCard(
                    label = "MIN",
                    value = state.repsMin,
                    onChange = viewModel::setRepsMin,
                    modifier = Modifier.weight(1f),
                )
                RangeStepperCard(
                    label = "MAX",
                    value = state.repsMax,
                    onChange = viewModel::setRepsMax,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = "Hit top of range on all sets \u2192 add weight next session",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
            )

            // Effort
            EditSectionLabel("EFFORT (CNS LOAD)", topPadding = 20)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EffortChoice(
                    effort = Effort.HIGH,
                    label = "High",
                    description = "Compounds",
                    selected = state.effort == Effort.HIGH,
                    onClick = { viewModel.setEffort(Effort.HIGH) },
                    modifier = Modifier.weight(1f),
                )
                EffortChoice(
                    effort = Effort.MED,
                    label = "Med",
                    description = "Assist",
                    selected = state.effort == Effort.MED,
                    onClick = { viewModel.setEffort(Effort.MED) },
                    modifier = Modifier.weight(1f),
                )
                EffortChoice(
                    effort = Effort.LOW,
                    label = "Low",
                    description = "Isolation",
                    selected = state.effort == Effort.LOW,
                    onClick = { viewModel.setEffort(Effort.LOW) },
                    modifier = Modifier.weight(1f),
                )
            }

            // Remove button (only on edit)
            if (!state.isNew) {
                Spacer(Modifier.height(32.dp))
                LtButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    variant = LtButtonVariant.Error,
                    icon = Icons.Default.Delete,
                ) {
                    Text("Remove from day")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove lift?") },
            text = { Text("All history for this lift will also be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteLift(onBack)
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ExerciseIdentityCard(
    name: String,
    muscle: String,
    equipment: String,
    onClick: () -> Unit,
) {
    LtCard(
        modifier = Modifier.fillMaxWidth(),
        variant = LtCardVariant.Filled,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val subtitle = listOf(muscle, equipment)
                    .filter { it.isNotBlank() }
                    .joinToString(" \u00B7 ")
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = "Tap to set muscle / equipment",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Pick exercise",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun RangeStepperCard(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(top = 10.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            StepperButton(
                icon = Icons.Default.Remove,
                onClick = { onChange((value - 1).coerceAtLeast(1)) },
                enabled = value > 1,
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = RobotoMono),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(44.dp),
                textAlign = TextAlign.Center,
            )
            StepperButton(
                icon = Icons.Default.Add,
                onClick = { onChange(value + 1) },
            )
        }
    }
}

@Composable
private fun EffortChoice(
    effort: Effort,
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = when (effort) {
        Effort.HIGH -> MaterialTheme.appColors.effortHigh
        Effort.MED -> MaterialTheme.appColors.effortMed
        Effort.LOW -> MaterialTheme.appColors.effortLow
    }
    val borderWidth = if (selected) 2.dp else 1.dp
    val borderColor = if (selected) color else MaterialTheme.colorScheme.outlineVariant
    val bg = if (selected) MaterialTheme.colorScheme.surfaceContainerHigh
    else androidx.compose.ui.graphics.Color.Transparent
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

