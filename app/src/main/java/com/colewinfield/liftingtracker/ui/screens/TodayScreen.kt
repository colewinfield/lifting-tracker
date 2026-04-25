package com.colewinfield.liftingtracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.colewinfield.liftingtracker.data.AppContainer
import com.colewinfield.liftingtracker.data.Effort
import com.colewinfield.liftingtracker.data.HistoryEntry
import com.colewinfield.liftingtracker.data.Lift
import com.colewinfield.liftingtracker.data.PerformedSet
import com.colewinfield.liftingtracker.data.SampleData
import com.colewinfield.liftingtracker.ui.components.LtButton
import com.colewinfield.liftingtracker.ui.components.LtButtonSize
import com.colewinfield.liftingtracker.ui.components.LtButtonVariant
import com.colewinfield.liftingtracker.ui.components.LtChip
import com.colewinfield.liftingtracker.ui.theme.LiftingTrackerTheme
import com.colewinfield.liftingtracker.ui.theme.RobotoMono
import com.colewinfield.liftingtracker.ui.theme.appColors

@Composable
fun TodayScreen(
    modifier: Modifier = Modifier,
    onOpenLiftDetail: (liftId: String, tab: DetailTab) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val repo = remember(context) { AppContainer.repository(context) }
    val viewModel: TodayViewModel = viewModel(factory = TodayViewModel.factory(repo))
    val state by viewModel.state.collectAsStateWithLifecycle()
    TodayContent(
        state = state,
        onToggleExpand = viewModel::toggleExpand,
        onAddSet = viewModel::addSet,
        onToggleSetDone = viewModel::toggleSetDone,
        onAdjustWeight = viewModel::adjustWeight,
        onAdjustReps = viewModel::adjustReps,
        onFinishSession = viewModel::finishSession,
        onOpenLiftDetail = onOpenLiftDetail,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodayContent(
    state: TodayUiState,
    onToggleExpand: (String) -> Unit,
    onAddSet: (String) -> Unit,
    onToggleSetDone: (Long) -> Unit,
    onAdjustWeight: (Long, Double) -> Unit,
    onAdjustReps: (Long, Int) -> Unit,
    onFinishSession: () -> Unit,
    onOpenLiftDetail: (liftId: String, tab: DetailTab) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        // Parent MainActivity Scaffold already pads for the bottom nav bar, so this Scaffold
        // should not also consume the navigationBars inset (would double-pad).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { /* TODO: open drawer */ }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: pick day */ }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar")
                    }
                    IconButton(onClick = { /* TODO: overflow */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                },
                windowInsets = TopAppBarDefaults.windowInsets,
            )
        },
    ) { padding ->
        val day = state.day
        val program = state.program
        if (day == null || program == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        val totalTargetSets = day.lifts.sumOf { it.sets.first }
        val activeDayPosition = program.days.indexOf(day).let { if (it < 0) 0 else it } + 1

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                DayHeader(
                    dayName = day.name,
                    focus = day.focus,
                    weekNumber = state.weekNumber,
                    cycleLength = program.cycleLength,
                    isDeload = state.isDeload,
                    dayPosition = activeDayPosition,
                    totalDays = program.days.size,
                )
            }
            item {
                SessionProgressCard(
                    targetSets = totalTargetSets,
                    doneSets = state.sets.values.sumOf { sets -> sets.count { it.done } },
                    elapsed = "00:00",
                )
            }
            items(day.lifts, key = { it.id }) { lift ->
                LiftCard(
                    lift = lift,
                    index = day.lifts.indexOf(lift) + 1,
                    performed = state.sets[lift.id].orEmpty(),
                    history = state.lastWeekByLift[lift.id],
                    expanded = state.expandedLiftId == lift.id,
                    onToggleExpand = { onToggleExpand(lift.id) },
                    onAddSet = { onAddSet(lift.id) },
                    onToggleSetDone = onToggleSetDone,
                    onAdjustWeight = onAdjustWeight,
                    onAdjustReps = onAdjustReps,
                    onOpenHistory = { onOpenLiftDetail(lift.id, DetailTab.History) },
                    onOpenHowTo = { onOpenLiftDetail(lift.id, DetailTab.HowTo) },
                )
            }
            item {
                LtButton(
                    onClick = onFinishSession,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    variant = LtButtonVariant.Tonal,
                    size = LtButtonSize.Lg,
                    icon = Icons.Default.Check,
                ) { Text("Finish session") }
            }
        }
    }
}

@Composable
private fun DayHeader(
    dayName: String,
    focus: String,
    weekNumber: Int,
    cycleLength: Int,
    isDeload: Boolean,
    dayPosition: Int,
    totalDays: Int,
) {
    Column(modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            WeekChip(
                weekNumber = weekNumber,
                cycleLength = cycleLength,
                isDeload = isDeload,
            )
            Text(
                text = "Day $dayPosition / $totalDays",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = dayName,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (focus.isNotEmpty()) {
            Text(
                text = focus,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun WeekChip(weekNumber: Int, cycleLength: Int, isDeload: Boolean) {
    val container =
        if (isDeload) MaterialTheme.colorScheme.tertiaryContainer
        else MaterialTheme.colorScheme.primaryContainer
    val onContainer =
        if (isDeload) MaterialTheme.colorScheme.onTertiaryContainer
        else MaterialTheme.colorScheme.onPrimaryContainer
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            Icons.Default.Bolt,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = onContainer,
        )
        Text(
            text = buildString {
                append("Week $weekNumber of $cycleLength")
                if (isDeload) append(" · Deload")
            },
            style = MaterialTheme.typography.labelMedium,
            color = onContainer,
        )
    }
}

@Composable
private fun SessionProgressCard(targetSets: Int, doneSets: Int, elapsed: String) {
    val pct = if (targetSets > 0) (doneSets.toFloat() / targetSets).coerceIn(0f, 1f) else 0f
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        "SESSION PROGRESS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "$doneSets / $targetSets sets",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "ELAPSED",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        elapsed,
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = RobotoMono),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(pct)
                        .height(8.dp)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

@Composable
private fun LiftCard(
    lift: Lift,
    index: Int,
    performed: List<PerformedSet>,
    history: HistoryEntry?,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddSet: () -> Unit,
    onToggleSetDone: (Long) -> Unit,
    onAdjustWeight: (Long, Double) -> Unit,
    onAdjustReps: (Long, Int) -> Unit,
    onOpenHistory: () -> Unit = {},
    onOpenHowTo: () -> Unit = {},
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .clickable(onClick = onToggleExpand)
                .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NumberedEffortBadge(index = index, effort = lift.effort)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = lift.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    Text(
                        text = "${rangeText(lift.sets)} × ${rangeText(lift.reps)} reps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (history != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "${formatWeight(history.sets[0].weight)} lb × ${history.sets[0].reps}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            ExpandedSetEditor(
                performed = performed,
                history = history,
                onAddSet = onAddSet,
                onToggleSetDone = onToggleSetDone,
                onAdjustWeight = onAdjustWeight,
                onAdjustReps = onAdjustReps,
                onOpenHistory = onOpenHistory,
                onOpenHowTo = onOpenHowTo,
            )
        }
    }
}

@Composable
private fun NumberedEffortBadge(index: Int, effort: Effort) {
    val effortColor = when (effort) {
        Effort.HIGH -> MaterialTheme.appColors.effortHigh
        Effort.MED  -> MaterialTheme.appColors.effortMed
        Effort.LOW  -> MaterialTheme.appColors.effortLow
    }
    val ringColor = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(modifier = Modifier.size(40.dp)) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .align(Alignment.BottomStart),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$index",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(14.dp)
                .clip(CircleShape)
                .background(ringColor)
                .padding(2.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(effortColor),
            )
        }
    }
}

@Composable
private fun ExpandedSetEditor(
    performed: List<PerformedSet>,
    history: HistoryEntry?,
    onAddSet: () -> Unit,
    onToggleSetDone: (Long) -> Unit,
    onAdjustWeight: (Long, Double) -> Unit,
    onAdjustReps: (Long, Int) -> Unit,
    onOpenHistory: () -> Unit = {},
    onOpenHowTo: () -> Unit = {},
) {
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (history != null) LastWeekStrip(entry = history, onOpenHistory = onOpenHistory)

        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("SET", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(32.dp))
            Text("WEIGHT (LB)", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f))
            Text("REPS", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f))
            Spacer(Modifier.width(48.dp))
        }

        performed.forEachIndexed { idx, set ->
            SetRow(
                index = idx,
                set = set,
                onToggleDone = { onToggleSetDone(set.id) },
                onAdjustWeight = { delta -> onAdjustWeight(set.id, delta) },
                onAdjustReps = { delta -> onAdjustReps(set.id, delta) },
            )
        }

        AddSetButton(onClick = onAddSet)

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LtChip(selected = false, onClick = { /* TODO */ }, label = "Swap", icon = Icons.Default.SwapHoriz)
            LtChip(selected = false, onClick = { /* TODO */ }, label = "Notes", icon = Icons.AutoMirrored.Filled.StickyNote2)
            LtChip(selected = false, onClick = onOpenHowTo, label = "How-to", icon = Icons.Default.PlayArrow)
        }
    }
}

@Composable
private fun LastWeekStrip(entry: HistoryEntry, onOpenHistory: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(Modifier.weight(1f)) {
            Text(
                "LAST WEEK · W${entry.week}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = entry.sets.joinToString(" · ") { "${formatWeight(it.weight)}×${it.reps}" },
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = RobotoMono),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        TextButton(onClick = onOpenHistory) {
            Text("HISTORY", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun SetRow(
    index: Int,
    set: PerformedSet,
    onToggleDone: () -> Unit,
    onAdjustWeight: (Double) -> Unit,
    onAdjustReps: (Int) -> Unit,
) {
    val rowBg = if (set.done) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val numberFg = if (set.done) MaterialTheme.colorScheme.onSecondaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(rowBg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "${index + 1}",
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = RobotoMono),
            color = numberFg,
            modifier = Modifier.width(32.dp),
        )
        NumberStepper(
            value = formatWeight(set.weight),
            suffix = "lb",
            done = set.done,
            onMinus = { onAdjustWeight(-5.0) },
            onPlus = { onAdjustWeight(5.0) },
            modifier = Modifier.weight(1f),
        )
        NumberStepper(
            value = set.reps.toString(),
            suffix = null,
            done = set.done,
            onMinus = { onAdjustReps(-1) },
            onPlus = { onAdjustReps(1) },
            modifier = Modifier.weight(1f),
        )
        DoneButton(done = set.done, onClick = onToggleDone)
    }
}

@Composable
private fun NumberStepper(
    value: String,
    suffix: String?,
    done: Boolean,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (done) MaterialTheme.colorScheme.surfaceContainer
    else MaterialTheme.colorScheme.surfaceContainerHigh
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(container)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMinus, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(18.dp))
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = RobotoMono),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (suffix != null) {
                Spacer(Modifier.width(3.dp))
                Text(
                    text = suffix,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onPlus, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun DoneButton(done: Boolean, onClick: () -> Unit) {
    val bg = if (done) MaterialTheme.colorScheme.primary else Color.Transparent
    val fg = if (done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .then(
                if (!done) Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                else Modifier
            )
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (done) {
            Icon(Icons.Default.Check, contentDescription = "Mark incomplete", tint = fg, modifier = Modifier.size(22.dp))
        } else {
            Icon(Icons.Default.Check, contentDescription = "Mark complete", tint = Color.Transparent)
        }
    }
}

@Composable
private fun AddSetButton(onClick: () -> Unit) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val density = LocalDensity.current
    val cornerPx = with(density) { 12.dp.toPx() }
    val strokeWidthPx = with(density) { 1.dp.toPx() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    style = Stroke(
                        width = strokeWidthPx,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f),
                    ),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                )
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "Add set",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun rangeText(range: IntRange): String =
    if (range.first == range.last) "${range.first}" else "${range.first}–${range.last}"

private fun formatWeight(w: Double): String =
    if (w % 1.0 == 0.0) w.toInt().toString() else "%.1f".format(w)

@Preview(showBackground = true, backgroundColor = 0xFF17120E, heightDp = 1400)
@Composable
private fun TodayScreenPreview() {
    LiftingTrackerTheme(darkTheme = true, dynamicColor = false) {
        val program = SampleData.program
        val day = program.days[0]
        TodayContent(
            state = TodayUiState(
                program = program,
                day = day,
                weekNumber = 4,
                isDeload = false,
                sessionId = "preview",
                expandedLiftId = day.lifts.first().id,
                sets = mapOf(
                    day.lifts.first().id to listOf(
                        PerformedSet(id = 1, weight = 225.0, reps = 5, done = true),
                        PerformedSet(id = 2, weight = 225.0, reps = 5, done = true),
                        PerformedSet(id = 3, weight = 230.0, reps = 5, done = false),
                    ),
                ),
                lastWeekByLift = mapOf(
                    day.lifts.first().id to SampleData.history.getValue(day.lifts.first().id).first(),
                ),
            ),
            onToggleExpand = {},
            onAddSet = {},
            onToggleSetDone = {},
            onAdjustWeight = { _, _ -> },
            onAdjustReps = { _, _ -> },
            onFinishSession = {},
        )
    }
}
