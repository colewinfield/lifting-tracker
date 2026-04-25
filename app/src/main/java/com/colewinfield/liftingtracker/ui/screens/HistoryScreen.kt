package com.colewinfield.liftingtracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.colewinfield.liftingtracker.data.AppContainer
import com.colewinfield.liftingtracker.ui.components.LtCard
import com.colewinfield.liftingtracker.ui.components.LtCardVariant
import com.colewinfield.liftingtracker.ui.components.LtTopAppBar
import com.colewinfield.liftingtracker.ui.components.LtTopAppBarVariant
import com.colewinfield.liftingtracker.ui.theme.LiftingTrackerTheme
import com.colewinfield.liftingtracker.ui.theme.RobotoMono
import com.colewinfield.liftingtracker.ui.theme.appColors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repo = remember(context) { AppContainer.repository(context) }
    val settingsRepo = remember(context) { AppContainer.settings(context) }
    val viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(repo, settingsRepo))
    val state by viewModel.state.collectAsStateWithLifecycle()
    HistoryContent(state = state, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryContent(
    state: HistoryUiState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LtTopAppBar(
                title = "History",
                subtitle = state.cycleLabel.takeIf { it.isNotEmpty() },
                variant = LtTopAppBarVariant.Medium,
                actions = {
                    IconButton(onClick = { /* TODO: filter / overflow */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                },
            )
        },
    ) { padding ->
        if (!state.isReady) {
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
            item { StatTilesGrid(state) }
            item { Spacer(Modifier.height(16.dp)) }
            item { SectionLabel("Top 3 lifts · top set") }
            item { LiftProgressCard(state) }
            item { Spacer(Modifier.height(20.dp)) }
            item { SectionLabel("Volume by muscle · this cycle") }
            item { MuscleVolumeCard(state.muscleVolumes) }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 10.dp),
    )
}

@Composable
private fun StatTilesGrid(state: HistoryUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = "SESSIONS",
                value = "${state.sessionsThisCycle}",
                sub = "this cycle",
                icon = Icons.Default.FitnessCenter,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "VOLUME",
                value = formatVolume(state.volumeLb),
                sub = "lb moved",
                icon = Icons.Default.Bolt,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = "STREAK",
                value = "${state.streakDays}",
                sub = "days",
                icon = Icons.Default.LocalFireDepartment,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "PRS",
                value = "${state.prsThisCycle}",
                sub = "this cycle",
                icon = Icons.Default.EmojiEvents,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    sub: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    LtCard(modifier = modifier, variant = LtCardVariant.Filled) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = RobotoMono,
                    letterSpacing = (-0.5).sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LiftProgressCard(state: HistoryUiState) {
    val seriesColors = listOf(
        MaterialTheme.appColors.chart1,
        MaterialTheme.appColors.chart2,
        MaterialTheme.appColors.chart3,
    )
    LtCard(modifier = Modifier.fillMaxWidth(), variant = LtCardVariant.Filled) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (state.chartSeries.isEmpty() || state.chartWeeks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Log some sets to see progression",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LiftLineChart(
                    weeks = state.chartWeeks,
                    series = state.chartSeries,
                    colors = seriesColors,
                )
                Spacer(Modifier.height(12.dp))
                // FlowRow so long lift names ("Romanian Deadlift" etc.) wrap to a new line
                // instead of being squeezed character-by-character into a fixed Row split.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    state.chartSeries.forEachIndexed { i, s ->
                        Legend(
                            label = s.name,
                            color = seriesColors.getOrElse(i) { MaterialTheme.colorScheme.primary },
                            delta = formatDelta(s.deltaLb),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Legend(label: String, color: Color, delta: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = delta,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.appColors.effortLow,
            maxLines = 1,
        )
    }
}

@Composable
private fun LiftLineChart(
    weeks: List<String>,
    series: List<HistorySeries>,
    colors: List<Color>,
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val pointFill = MaterialTheme.colorScheme.surface

    val allValues = series.flatMap { it.points.map { p -> p.value } }
    val maxValue = allValues.maxOrNull() ?: 0.0
    val minValue = allValues.minOrNull() ?: 0.0
    val range = max(1.0, maxValue - minValue)

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
        ) {
            val padL = 30.dp.toPx()
            val padR = 10.dp.toPx()
            val padT = 10.dp.toPx()
            val padB = 24.dp.toPx()
            val w = size.width
            val h = size.height
            val plotW = w - padL - padR
            val plotH = h - padT - padB
            val n = weeks.size
            val xStep = if (n > 1) plotW / (n - 1) else 0f

            fun xAt(i: Int): Float = padL + i * xStep
            fun yAt(v: Double): Float =
                padT + ((1.0 - (v - minValue) / range) * plotH).toFloat()

            // Dashed gridlines (top, middle, bottom)
            val dash = PathEffect.dashPathEffect(floatArrayOf(2f, 3f), 0f)
            listOf(0f, 0.5f, 1f).forEach { f ->
                val y = padT + f * plotH
                drawLine(
                    color = gridColor.copy(alpha = 0.6f),
                    start = Offset(padL, y),
                    end = Offset(w - padR, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dash,
                )
            }

            // Series lines
            series.forEachIndexed { i, s ->
                if (s.points.size < 2) return@forEachIndexed
                val color = colors.getOrElse(i) { return@forEachIndexed }
                val path = Path().apply {
                    s.points.forEachIndexed { idx, p ->
                        val x = xAt(p.weekIndex)
                        val y = yAt(p.value)
                        if (idx == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }

            // Series points
            series.forEachIndexed { i, s ->
                val color = colors.getOrElse(i) { return@forEachIndexed }
                s.points.forEach { p ->
                    val center = Offset(xAt(p.weekIndex), yAt(p.value))
                    drawCircle(color = pointFill, radius = 3.dp.toPx(), center = center)
                    drawCircle(
                        color = color,
                        radius = 3.dp.toPx(),
                        center = center,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }
        }
        // Week labels — Row inset by chart paddings so labels align to data points
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 30.dp, end = 10.dp),
            horizontalArrangement = if (weeks.size == 1) Arrangement.Center
                else Arrangement.SpaceBetween,
        ) {
            weeks.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                )
            }
        }
    }
}

@Composable
private fun MuscleVolumeCard(volumes: List<MuscleVolume>) {
    val palette = listOf(
        MaterialTheme.appColors.chart1,
        MaterialTheme.appColors.chart2,
        MaterialTheme.appColors.effortHigh,
        MaterialTheme.appColors.effortMed,
        MaterialTheme.appColors.chart3,
        MaterialTheme.appColors.effortLow,
    )
    LtCard(modifier = Modifier.fillMaxWidth(), variant = LtCardVariant.Filled) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (volumes.isEmpty()) {
                Text(
                    text = "No sets logged yet this cycle",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val maxSets = max(1, volumes.maxOf { it.sets })
                volumes.forEachIndexed { i, mv ->
                    if (i > 0) Spacer(Modifier.height(10.dp))
                    MuscleBarRow(
                        muscle = mv.muscle,
                        sets = mv.sets,
                        fraction = mv.sets.toFloat() / maxSets.toFloat(),
                        color = palette[i % palette.size],
                    )
                }
            }
        }
    }
}

@Composable
private fun MuscleBarRow(muscle: String, sets: Int, fraction: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = muscle,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(88.dp),
        )
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "$sets sets",
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = RobotoMono),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(56.dp),
        )
    }
}

private fun formatVolume(volumeLb: Double): String {
    val rounded = volumeLb.roundToInt()
    if (rounded < 1000) return "$rounded"
    val k = rounded / 1000.0
    val text = if (k >= 100) k.roundToInt().toString()
        else "%.1f".format(k).trimEnd('0').trimEnd('.')
    return "${text}K"
}

private fun formatDelta(deltaLb: Double): String {
    val rounded = deltaLb.roundToInt()
    val sign = when {
        rounded > 0 -> "+"
        rounded < 0 -> "−"
        else -> ""
    }
    return "$sign${abs(rounded)} lb"
}

@Preview(showBackground = true, backgroundColor = 0xFF17120E)
@Composable
private fun HistoryScreenPreview() {
    LiftingTrackerTheme(darkTheme = true, dynamicColor = false) {
        HistoryContent(
            state = HistoryUiState(
                isReady = true,
                cycleLabel = "Cycle 1 · Week 4",
                sessionsThisCycle = 6,
                volumeLb = 24500.0,
                streakDays = 21,
                prsThisCycle = 3,
                chartWeeks = listOf("W1", "W2", "W3"),
                chartSeries = listOf(
                    HistorySeries(
                        name = "Smith Squat",
                        points = listOf(
                            HistoryPoint(0, 215.0),
                            HistoryPoint(1, 215.0),
                            HistoryPoint(2, 225.0),
                        ),
                        deltaLb = 10.0,
                    ),
                    HistorySeries(
                        name = "RDL",
                        points = listOf(
                            HistoryPoint(0, 180.0),
                            HistoryPoint(1, 180.0),
                            HistoryPoint(2, 185.0),
                        ),
                        deltaLb = 5.0,
                    ),
                    HistorySeries(
                        name = "Flat DB",
                        points = listOf(
                            HistoryPoint(0, 80.0),
                            HistoryPoint(1, 80.0),
                            HistoryPoint(2, 85.0),
                        ),
                        deltaLb = 5.0,
                    ),
                ),
                muscleVolumes = listOf(
                    MuscleVolume("Quads", 12),
                    MuscleVolume("Hamstrings", 8),
                    MuscleVolume("Chest", 9),
                    MuscleVolume("Back", 8),
                    MuscleVolume("Calves", 6),
                    MuscleVolume("Abs", 6),
                ),
            ),
        )
    }
}
