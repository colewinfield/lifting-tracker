package com.colewinfield.liftingtracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.colewinfield.liftingtracker.data.AppContainer
import com.colewinfield.liftingtracker.data.Day
import com.colewinfield.liftingtracker.data.Effort
import com.colewinfield.liftingtracker.data.Lift
import com.colewinfield.liftingtracker.data.Program
import com.colewinfield.liftingtracker.data.SampleData
import com.colewinfield.liftingtracker.ui.components.EffortDot
import com.colewinfield.liftingtracker.ui.components.LtCard
import com.colewinfield.liftingtracker.ui.components.LtCardVariant
import com.colewinfield.liftingtracker.ui.theme.LiftingTrackerTheme
import com.colewinfield.liftingtracker.ui.theme.RobotoMono
import com.colewinfield.liftingtracker.ui.theme.appColors

@Composable
fun ProgramScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repo = remember(context) { AppContainer.repository(context) }
    val viewModel: ProgramViewModel = viewModel(factory = ProgramViewModel.factory(repo))
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProgramContent(
        state = state,
        onSelectWeek = viewModel::selectWeek,
        onOpenDay = { /* TODO: navigate to Day detail / Today for that day */ },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgramContent(
    state: ProgramUiState,
    onSelectWeek: (Int) -> Unit,
    onOpenDay: (Day) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Program", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = { /* TODO: program info */ }) {
                        Icon(Icons.Default.Info, contentDescription = "Program info")
                    }
                    IconButton(onClick = { /* TODO: overflow */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                },
                windowInsets = TopAppBarDefaults.windowInsets,
            )
        },
    ) { padding ->
        val program = state.program
        if (program == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
        ) {
            item { ProgramHeader(program) }
            item {
                WeekRing(
                    cycleLength = program.cycleLength,
                    currentWeek = state.currentWeek,
                    selectedWeek = state.selectedWeek,
                    deloadWeek = program.deloadWeek,
                )
            }
            item {
                WeekStrip(
                    cycleLength = program.cycleLength,
                    currentWeek = state.currentWeek,
                    selectedWeek = state.selectedWeek,
                    deloadWeek = program.deloadWeek,
                    onSelect = onSelectWeek,
                )
            }
            item { Spacer(Modifier.height(20.dp)) }
            items(program.days, key = { it.id }) { day ->
                val dayNum = program.days.indexOf(day) + 1
                Box(modifier = Modifier.padding(bottom = 10.dp)) {
                    DayCard(day = day, dayNum = dayNum, onOpen = { onOpenDay(day) })
                }
            }
            item { ProgramNotes(notes = program.notes) }
        }
    }
}

@Composable
private fun ProgramHeader(program: Program) {
    val trainingDays = program.days.count { !it.isRest }
    Column(modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 16.dp)) {
        Text(
            text = "PROGRAM",
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.8.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = program.name,
            style = MaterialTheme.typography.headlineLarge.copy(letterSpacing = (-0.5).sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${program.cycleLength}-week cycle · $trainingDays training days",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WeekRing(
    cycleLength: Int,
    currentWeek: Int,
    selectedWeek: Int,
    deloadWeek: Int,
) {
    val pct = if (cycleLength > 0) currentWeek.toFloat() / cycleLength.toFloat() else 0f
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val progressColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier.size(96.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val sizePx = size.width
                val strokePx = 7.dp.toPx()
                val radius = (sizePx - 2 * strokePx) / 2f
                val center = Offset(sizePx / 2f, sizePx / 2f)
                val arcOffset = Offset(strokePx, strokePx)
                val arcSize = Size(sizePx - 2 * strokePx, sizePx - 2 * strokePx)
                drawCircle(
                    color = trackColor,
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokePx),
                )
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * pct,
                    useCenter = false,
                    topLeft = arcOffset,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "WEEK",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "$selectedWeek",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = RobotoMono,
                        lineHeight = 32.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "of $cycleLength",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${(pct * 100).toInt()}% through cycle",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${(cycleLength - currentWeek).coerceAtLeast(0)} weeks remaining",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Deload on W$deloadWeek",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeekStrip(
    cycleLength: Int,
    currentWeek: Int,
    selectedWeek: Int,
    deloadWeek: Int,
    onSelect: (Int) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(start = 4.dp, end = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(cycleLength) { i ->
            val w = i + 1
            WeekChip(
                week = w,
                isActive = w == selectedWeek,
                isCurrent = w == currentWeek,
                isDeload = w == deloadWeek,
                isPast = w < currentWeek,
                onClick = { onSelect(w) },
            )
        }
    }
}

@Composable
private fun WeekChip(
    week: Int,
    isActive: Boolean,
    isCurrent: Boolean,
    isDeload: Boolean,
    isPast: Boolean,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val effortLow = MaterialTheme.appColors.effortLow
    val bg = when {
        isActive -> cs.primary
        isDeload -> cs.tertiaryContainer
        else -> cs.surfaceContainerHigh
    }
    val fg = when {
        isActive -> cs.onPrimary
        isDeload -> cs.onTertiaryContainer
        else -> cs.onSurface
    }
    val checkColor = if (isDeload) cs.onTertiaryContainer else effortLow
    val currentDotColor = if (isActive) cs.onPrimary else cs.primary

    Box(
        modifier = Modifier
            .width(44.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "WK",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                color = fg.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "$week",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = RobotoMono),
                color = fg,
            )
        }
        if (isCurrent) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(currentDotColor),
            )
        }
        if (isPast && !isActive) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = checkColor,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
                    .size(10.dp),
            )
        }
    }
}

@Composable
private fun DayCard(day: Day, dayNum: Int, onOpen: () -> Unit) {
    if (day.isRest) {
        RestDayCard(day = day, dayNum = dayNum)
    } else {
        WorkoutDayCard(day = day, dayNum = dayNum, onOpen = onOpen)
    }
}

@Composable
private fun RestDayCard(day: Day, dayNum: Int) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .drawBehind {
                drawRoundRect(
                    color = outline,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
                    ),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                )
            }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$dayNum",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Rest",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = day.dayOfWeek,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WorkoutDayCard(day: Day, dayNum: Int, onOpen: () -> Unit) {
    LtCard(
        modifier = Modifier.fillMaxWidth(),
        variant = LtCardVariant.Filled,
        onClick = onOpen,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$dayNum",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row {
                    Text(
                        text = day.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .alignByBaseline()
                            .weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = day.dayOfWeek,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = day.focus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    day.lifts.forEach { lift ->
                        LiftSummaryRow(lift = lift)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                Spacer(Modifier.height(10.dp))
                DayFooter(lifts = day.lifts)
            }
        }
    }
}

@Composable
private fun LiftSummaryRow(lift: Lift) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        EffortDot(effort = lift.effort, size = 8.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = lift.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = formatSetsReps(lift),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = RobotoMono,
                letterSpacing = 0.2.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DayFooter(lifts: List<Lift>) {
    val totalSets = lifts.sumOf { it.sets.last }
    val high = lifts.count { it.effort == Effort.HIGH }
    val med = lifts.count { it.effort == Effort.MED }
    val low = lifts.count { it.effort == Effort.LOW }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$totalSets sets · ${lifts.size} lifts",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(high) { EffortDot(effort = Effort.HIGH, size = 6.dp) }
            repeat(med)  { EffortDot(effort = Effort.MED,  size = 6.dp) }
            repeat(low)  { EffortDot(effort = Effort.LOW,  size = 6.dp) }
        }
    }
}

@Composable
private fun ProgramNotes(notes: List<String>) {
    if (notes.isEmpty()) return
    Column(modifier = Modifier.padding(top = 10.dp)) {
        Text(
            text = "PROGRAM NOTES",
            style = MaterialTheme.typography.titleSmall.copy(letterSpacing = 0.5.sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
        )
        LtCard(
            modifier = Modifier.fillMaxWidth(),
            variant = LtCardVariant.Outlined,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                notes.forEachIndexed { i, note ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "${i + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

private fun formatSetsReps(lift: Lift): String {
    val sets = if (lift.sets.first == lift.sets.last) {
        "${lift.sets.first}"
    } else {
        "${lift.sets.first}\u2013${lift.sets.last}"
    }
    val reps = if (lift.reps.first == lift.reps.last) {
        "${lift.reps.first}"
    } else {
        "${lift.reps.first}\u2013${lift.reps.last}"
    }
    return "$sets\u00D7$reps"
}

@Preview(showBackground = true, backgroundColor = 0xFF17120E)
@Composable
private fun ProgramScreenPreview() {
    LiftingTrackerTheme(darkTheme = true, dynamicColor = false) {
        ProgramContent(
            state = ProgramUiState(
                program = SampleData.program,
                currentWeek = SampleData.current.week,
                selectedWeek = SampleData.current.week,
            ),
            onSelectWeek = {},
            onOpenDay = {},
        )
    }
}
