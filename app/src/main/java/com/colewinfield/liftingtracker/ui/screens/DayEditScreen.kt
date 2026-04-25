package com.colewinfield.liftingtracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.colewinfield.liftingtracker.data.AppContainer
import com.colewinfield.liftingtracker.data.Lift
import com.colewinfield.liftingtracker.data.Weekday
import com.colewinfield.liftingtracker.ui.components.EffortDot
import com.colewinfield.liftingtracker.ui.components.LtButton
import com.colewinfield.liftingtracker.ui.components.LtButtonSize
import com.colewinfield.liftingtracker.ui.components.LtButtonVariant
import com.colewinfield.liftingtracker.ui.components.LtChip
import com.colewinfield.liftingtracker.ui.theme.RobotoMono
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayEditScreen(
    dayId: String,
    onBack: () -> Unit,
    onOpenLift: (dayId: String, liftId: String) -> Unit,
) {
    val context = LocalContext.current
    val repo = remember(context) { AppContainer.repository(context) }
    val viewModel: DayEditViewModel = viewModel(
        key = "day-edit-$dayId",
        factory = DayEditViewModel.factory(repo, dayId),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showEditName by rememberSaveable { mutableStateOf(false) }
    var showEditFocus by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top)),
    ) {
        EditTopRow(
            title = if (state.isNew) "New day" else "Edit day",
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
            // Day name
            EditSectionLabel("DAY NAME", topPadding = 12)
            TapToEditCard(text = state.name, onClick = { showEditName = true })

            // Scheduled (day-of-week)
            EditSectionLabel("SCHEDULED")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Weekday.entries.forEach { wd ->
                    LtChip(
                        selected = wd == state.dayOfWeek,
                        onClick = { viewModel.setDayOfWeek(wd) },
                        label = wd.short,
                    )
                }
            }

            // Focus
            EditSectionLabel("FOCUS")
            TapToEditCard(
                text = state.focus.ifBlank { "—" },
                muted = state.focus.isBlank(),
                onClick = { showEditFocus = true },
            )

            // Rest day toggle
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.NightsStay,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Rest day",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = state.isRest,
                    onCheckedChange = viewModel::setRest,
                )
            }

            // Lifts section (only when not a rest day; rest days have no lifts)
            if (!state.isRest) {
                Row(
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 24.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "LIFTS \u00B7 ${state.lifts.size}",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    LtButton(
                        onClick = {
                            scope.launch {
                                val resolvedDayId = viewModel.ensurePersisted() ?: return@launch
                                onOpenLift(resolvedDayId, NEW_ID)
                            }
                        },
                        variant = LtButtonVariant.Text,
                        size = LtButtonSize.Sm,
                        icon = Icons.Default.Add,
                    ) {
                        Text("Add lift")
                    }
                }
                if (state.lifts.isEmpty()) {
                    Text(
                        text = "No lifts yet \u2014 tap \u201CAdd lift\u201D to start.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.lifts.forEach { lift ->
                            DayEditLiftRow(
                                lift = lift,
                                onClick = { onOpenLift(state.dayId, lift.id) },
                            )
                        }
                    }
                }
            }

            // Delete day (only on edit, not new)
            if (!state.isNew) {
                Spacer(Modifier.height(32.dp))
                LtButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    variant = LtButtonVariant.Error,
                    icon = Icons.Default.Delete,
                ) {
                    Text("Delete day")
                }
            }
        }
    }

    if (showEditName) {
        EditTextFieldDialog(
            title = "Day name",
            initial = state.name,
            label = "Name",
            onClose = { showEditName = false },
            onSave = { newName ->
                viewModel.setName(newName)
                showEditName = false
            },
        )
    }
    if (showEditFocus) {
        EditTextFieldDialog(
            title = "Focus",
            initial = state.focus,
            label = "Focus (e.g. Hamstrings + Glutes)",
            onClose = { showEditFocus = false },
            onSave = { newFocus ->
                viewModel.setFocus(newFocus)
                showEditFocus = false
            },
        )
    }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete day?") },
            text = { Text("All lifts and history attached to this day will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteDay(onBack)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
internal fun TapToEditCard(
    text: String,
    onClick: () -> Unit,
    muted: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Edit",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun DayEditLiftRow(lift: Lift, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        EffortDot(effort = lift.effort, size = 10.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = lift.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val setsText = if (lift.sets.first == lift.sets.last) "${lift.sets.first}"
            else "${lift.sets.first}\u2013${lift.sets.last}"
            val repsText = if (lift.reps.first == lift.reps.last) "${lift.reps.first}"
            else "${lift.reps.first}\u2013${lift.reps.last}"
            Text(
                text = "$setsText \u00D7 $repsText",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = RobotoMono,
                    letterSpacing = 0.2.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}
