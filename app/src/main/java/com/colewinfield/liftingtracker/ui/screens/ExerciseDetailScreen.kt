package com.colewinfield.liftingtracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import com.colewinfield.liftingtracker.data.Alternative
import com.colewinfield.liftingtracker.data.AppContainer
import com.colewinfield.liftingtracker.data.Effort
import com.colewinfield.liftingtracker.data.HistoryEntry
import com.colewinfield.liftingtracker.data.HistorySet
import com.colewinfield.liftingtracker.data.Lift
import com.colewinfield.liftingtracker.ui.components.EffortDot
import com.colewinfield.liftingtracker.ui.components.LtCard
import com.colewinfield.liftingtracker.ui.components.LtCardVariant
import com.colewinfield.liftingtracker.ui.components.LtChip
import com.colewinfield.liftingtracker.ui.theme.LiftingTrackerTheme
import com.colewinfield.liftingtracker.ui.theme.RobotoMono
import com.colewinfield.liftingtracker.ui.theme.appColors
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun ExerciseDetailScreen(
    liftId: String,
    initialTab: DetailTab,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repo = remember(context) { AppContainer.repository(context) }
    val viewModel: ExerciseDetailViewModel = viewModel(
        key = "exercise-detail-$liftId",
        factory = ExerciseDetailViewModel.factory(repo, liftId, initialTab),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    ExerciseDetailContent(
        state = state,
        onBack = onBack,
        onSelectTab = viewModel::selectTab,
        onSwap = { /* TODO: open swap sheet */ },
        modifier = modifier,
    )
}

@Composable
private fun ExerciseDetailContent(
    state: ExerciseDetailUiState,
    onBack: () -> Unit,
    onSelectTab: (DetailTab) -> Unit,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        DetailTopBar(onBack = onBack, onSwap = onSwap)
        if (!state.isReady) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return
        }
        val lift = state.lift
        if (lift == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Lift not found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Hero(
                lift = lift,
                bestWeight = state.bestWeight,
                estOneRm = state.estimatedOneRm,
                sessions = state.sessionsCount,
            )
            DetailTabBar(selected = state.tab, onSelect = onSelectTab)
            Box(modifier = Modifier.padding(16.dp)) {
                when (state.tab) {
                    DetailTab.History -> HistoryTab(history = state.history)
                    DetailTab.Graph -> GraphTab(history = state.history)
                    DetailTab.HowTo -> HowToTab(lift = lift)
                    DetailTab.Alts -> AltsTab(lift = lift, alternatives = state.alternatives)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DetailTopBar(onBack: () -> Unit, onSwap: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
            .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onSwap) {
            Icon(Icons.Default.SwapHoriz, contentDescription = "Swap exercise")
        }
        IconButton(onClick = { /* TODO: overflow */ }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More")
        }
    }
}

@Composable
private fun Hero(lift: Lift, bestWeight: Double, estOneRm: Double, sessions: Int) {
    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 6.dp),
        ) {
            EffortDot(effort = lift.effort, size = 10.dp)
            Text(
                text = "${lift.muscle.uppercase()} · ${lift.equipment.uppercase()}",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = lift.name,
            style = MaterialTheme.typography.headlineLarge.copy(letterSpacing = (-0.5).sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HeroStat(label = "BEST", value = if (bestWeight > 0) "${formatWeight(bestWeight)} lb" else "—")
            HeroStat(label = "LAST 1RM EST", value = if (estOneRm > 0) "${estOneRm.roundToInt()} lb" else "—")
            HeroStat(label = "SESSIONS", value = "$sessions")
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = RobotoMono),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DetailTabBar(selected: DetailTab, onSelect: (DetailTab) -> Unit) {
    val tabs = DetailTab.entries
    val selectedIndex = tabs.indexOf(selected)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = Color(0x40FFFFFF),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            },
    ) {
        TabRow(
            selectedTabIndex = selectedIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { positions ->
                if (selectedIndex < positions.size) {
                    Box(
                        modifier = Modifier
                            .tabIndicatorOffset(positions[selectedIndex])
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            },
            divider = {},
        ) {
            tabs.forEachIndexed { i, tab ->
                Tab(
                    selected = i == selectedIndex,
                    onClick = { onSelect(tab) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.5.sp),
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

// ─── History tab ────────────────────────────────────────────────────────────────

@Composable
private fun HistoryTab(history: List<HistoryEntry>) {
    if (history.isEmpty()) {
        EmptyTabHint("No sessions logged yet for this lift.")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        history.forEachIndexed { i, entry ->
            val prev = history.getOrNull(i + 1)
            val thisTop = entry.sets.maxOfOrNull { it.weight } ?: 0.0
            val prevTop = prev?.sets?.maxOfOrNull { it.weight }
            val diff = if (prevTop != null) thisTop - prevTop else null
            HistorySessionCard(entry = entry, diff = diff)
        }
    }
}

@Composable
private fun HistorySessionCard(entry: HistoryEntry, diff: Double?) {
    LtCard(modifier = Modifier.fillMaxWidth(), variant = LtCardVariant.Filled) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "WEEK ${entry.week}",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = entry.date,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (diff != null && diff != 0.0) DiffPill(diff)
            }
            Spacer(Modifier.height(8.dp))
            SetChips(entry.sets)
            if (entry.notes.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                NotesChip(entry.notes)
            }
        }
    }
}

@Composable
private fun DiffPill(diff: Double) {
    val isPositive = diff > 0
    val bg = if (isPositive) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.errorContainer
    val fg = if (isPositive) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onErrorContainer
    val icon: ImageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp
        else Icons.AutoMirrored.Filled.TrendingDown
    val sign = if (isPositive) "+" else ""
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
        Text(
            text = "$sign${formatWeight(diff)} lb",
            style = MaterialTheme.typography.labelMedium,
            color = fg,
        )
    }
}

@Composable
private fun SetChips(sets: List<HistorySet>) {
    FlowRowSimple(horizontalGap = 6.dp, verticalGap = 6.dp) {
        sets.forEach { set ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "${formatWeight(set.weight)} × ${set.reps}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = RobotoMono),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun NotesChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = "\u201C$text\u201D",
            style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

// ─── Graph tab ──────────────────────────────────────────────────────────────────

@Composable
private fun GraphTab(history: List<HistoryEntry>) {
    if (history.isEmpty()) {
        EmptyTabHint("No data yet — log a session to see progression.")
        return
    }
    val ordered = history.asReversed()
    val points = ordered.map { it.week to (it.sets.maxOfOrNull { s -> s.weight } ?: 0.0) }
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LtChip(selected = true, onClick = {}, label = "Top set")
            LtChip(selected = false, onClick = { /* TODO */ }, label = "Volume")
            LtChip(selected = false, onClick = { /* TODO */ }, label = "Est. 1RM")
        }
        Spacer(Modifier.height(12.dp))
        LtCard(modifier = Modifier.fillMaxWidth(), variant = LtCardVariant.Filled) {
            Column(modifier = Modifier.padding(12.dp)) {
                ProgressionChart(points = points)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryTile(
                label = "TOTAL VOLUME",
                value = "${formatThousands(totalVolume(history))} lb",
                sub = trendSub(history),
                subColor = MaterialTheme.appColors.effortLow,
                modifier = Modifier.weight(1f),
            )
            SummaryTile(
                label = "PROGRESSION",
                value = formatProgressionRate(points),
                sub = "avg over ${ordered.size} sessions",
                subColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SummaryTile(
    label: String,
    value: String,
    sub: String,
    subColor: Color,
    modifier: Modifier = Modifier,
) {
    LtCard(modifier = modifier, variant = LtCardVariant.Outlined) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = RobotoMono),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = subColor,
            )
        }
    }
}

@Composable
private fun ProgressionChart(points: List<Pair<Int, Double>>) {
    if (points.isEmpty()) return
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val pointFill = MaterialTheme.colorScheme.surface
    val lineColor = MaterialTheme.colorScheme.primary
    val areaTop = lineColor.copy(alpha = 0.3f)
    val areaBottom = lineColor.copy(alpha = 0f)

    val maxVal = points.maxOf { it.second }
    val minVal = points.minOf { it.second }
    val range = max(1.0, maxVal - minVal)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Y-axis label column
            Column(
                modifier = Modifier
                    .width(30.dp)
                    .height(180.dp)
                    .padding(top = 20.dp, bottom = 30.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${maxVal.roundToInt()}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = RobotoMono),
                    color = labelColor,
                )
                Text(
                    text = "${((maxVal + minVal) / 2.0).roundToInt()}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = RobotoMono),
                    color = labelColor,
                )
                Text(
                    text = "${minVal.roundToInt()}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = RobotoMono),
                    color = labelColor,
                )
            }
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp),
            ) {
                val padR = 10.dp.toPx()
                val padT = 20.dp.toPx()
                val padB = 30.dp.toPx()
                val w = size.width
                val h = size.height
                val plotW = w - padR
                val plotH = h - padT - padB
                val n = points.size
                val xStep = if (n > 1) plotW / (n - 1) else 0f

                fun xAt(i: Int): Float = i * xStep
                fun yAt(v: Double): Float =
                    padT + ((1.0 - (v - minVal) / range) * plotH).toFloat()

                // 5 dashed gridlines (top, 1/4, mid, 3/4, bottom)
                val dash = PathEffect.dashPathEffect(floatArrayOf(2f, 4f), 0f)
                listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { f ->
                    val y = padT + f * plotH
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w - padR, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dash,
                    )
                }

                // Build line path
                val linePath = Path().apply {
                    points.forEachIndexed { idx, (_, v) ->
                        val x = xAt(idx)
                        val y = yAt(v)
                        if (idx == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }

                // Area under line (close down to bottom)
                val areaPath = Path().apply {
                    addPath(linePath)
                    lineTo(xAt(n - 1), padT + plotH)
                    lineTo(xAt(0), padT + plotH)
                    close()
                }
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(areaTop, areaBottom),
                        startY = padT,
                        endY = padT + plotH,
                    ),
                )

                // Line
                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )

                // Markers
                points.forEachIndexed { idx, (_, v) ->
                    val center = Offset(xAt(idx), yAt(v))
                    drawCircle(color = pointFill, radius = 4.dp.toPx(), center = center)
                    drawCircle(
                        color = lineColor,
                        radius = 4.dp.toPx(),
                        center = center,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }
        }
        // Week labels — Row inset by Y-axis column + chart paddings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 30.dp, end = 10.dp),
            horizontalArrangement = if (points.size == 1) Arrangement.Center
                else Arrangement.SpaceBetween,
        ) {
            points.forEach { (week, _) ->
                Text(
                    text = "W$week",
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                )
            }
        }
    }
}

// ─── How-to tab ─────────────────────────────────────────────────────────────────

@Composable
private fun HowToTab(lift: Lift) {
    Column {
        VideoPlaceholder()
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Key cues",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        val cues = remember { defaultCues() }
        cues.forEachIndexed { i, cue ->
            if (i > 0) Spacer(Modifier.height(8.dp))
            CueRow(index = i + 1, text = cue)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Primary muscle",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        FlowRowSimple(horizontalGap = 6.dp, verticalGap = 6.dp) {
            LtChip(selected = false, onClick = {}, label = lift.muscle)
            LtChip(selected = false, onClick = {}, label = lift.equipment)
        }
    }
}

@Composable
private fun VideoPlaceholder() {
    val stripe = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .drawBehind {
                val stripeStep = 18.dp.toPx()
                val stripeWidth = 2.dp.toPx()
                val diag = (size.width + size.height)
                var d = -size.height
                while (d < diag) {
                    drawLine(
                        color = stripe,
                        start = Offset(d, 0f),
                        end = Offset(d + size.height, size.height),
                        strokeWidth = stripeWidth,
                    )
                    d += stripeStep
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play demo",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(32.dp),
            )
        }
        Text(
            text = "demo.mp4 \u00B7 0:42",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = RobotoMono),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 8.dp),
        )
    }
}

@Composable
private fun CueRow(index: Int, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$index",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = RobotoMono),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

// ─── Alts tab ───────────────────────────────────────────────────────────────────

@Composable
private fun AltsTab(lift: Lift, alternatives: List<Alternative>) {
    Column {
        Text(
            text = "Ranked by muscle overlap with ${lift.name}.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
        )
        if (alternatives.isEmpty()) {
            EmptyTabHint("No alternatives recorded for ${lift.name} yet.")
            return
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            alternatives.forEach { alt ->
                AlternativeRow(alt)
            }
        }
    }
}

@Composable
private fun AlternativeRow(alt: Alternative) {
    LtCard(modifier = Modifier.fillMaxWidth(), variant = LtCardVariant.Filled) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alt.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = "${alt.muscle} \u00B7 ${alt.equipment}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${alt.overlapPercent}%",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = RobotoMono),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "OVERLAP",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ─── Shared bits ─────────────────────────────────────────────────────────────

@Composable
private fun EmptyTabHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Minimal flow-row substitute (M3's FlowRow is experimental). Wraps children to the next line
 * when they overflow the available width.
 */
@Composable
private fun FlowRowSimple(
    horizontalGap: androidx.compose.ui.unit.Dp,
    verticalGap: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit,
) {
    androidx.compose.ui.layout.Layout(content = content) { measurables, constraints ->
        val hGap = horizontalGap.roundToPx()
        val vGap = verticalGap.roundToPx()
        val maxWidth = constraints.maxWidth
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        val rows = mutableListOf<MutableList<androidx.compose.ui.layout.Placeable>>()
        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentRowWidth = 0
        placeables.forEach { p ->
            val needed = if (currentRow.isEmpty()) p.width else currentRowWidth + hGap + p.width
            if (needed > maxWidth && currentRow.isNotEmpty()) {
                rows += currentRow
                currentRow = mutableListOf(p)
                currentRowWidth = p.width
            } else {
                currentRow += p
                currentRowWidth = needed
            }
        }
        if (currentRow.isNotEmpty()) rows += currentRow
        val rowHeights = rows.map { row -> row.maxOf { it.height } }
        val totalHeight = rowHeights.sum() + (rows.size - 1).coerceAtLeast(0) * vGap
        val width = maxWidth.coerceAtLeast(rows.maxOfOrNull { row ->
            row.sumOf { it.width } + (row.size - 1).coerceAtLeast(0) * hGap
        } ?: 0)
        layout(width, totalHeight) {
            var y = 0
            rows.forEachIndexed { rowIndex, row ->
                var x = 0
                row.forEach { p ->
                    p.placeRelative(x, y)
                    x += p.width + hGap
                }
                y += rowHeights[rowIndex] + vGap
            }
        }
    }
}

// ─── Helpers ────────────────────────────────────────────────────────────────────

private fun defaultCues(): List<String> = listOf(
    "Set up: brace your core, keep ribs down, find your foot position.",
    "Top of the rep: full lockout without overextending.",
    "Eccentric: control the negative — don't crash into bottom position.",
    "Bottom: brief pause, then drive through the working muscle.",
)

private fun formatWeight(w: Double): String {
    val rounded = w.roundToInt().toDouble()
    return if (kotlin.math.abs(w - rounded) < 0.01) "${rounded.roundToInt()}"
        else "%.1f".format(w)
}

private fun formatThousands(v: Double): String {
    val rounded = v.roundToInt()
    return "%,d".format(rounded)
}

private fun totalVolume(history: List<HistoryEntry>): Double =
    history.sumOf { entry -> entry.sets.sumOf { it.weight * it.reps } }

private fun trendSub(history: List<HistoryEntry>): String {
    if (history.size < 2) return "first cycle"
    val newest = history.first().sets.sumOf { it.weight * it.reps }
    val oldest = history.last().sets.sumOf { it.weight * it.reps }
    if (oldest <= 0) return "first cycle"
    val pct = ((newest - oldest) / oldest * 100).roundToInt()
    val arrow = if (pct >= 0) "\u25B2" else "\u25BC"
    return "$arrow ${kotlin.math.abs(pct)}% over span"
}

private fun formatProgressionRate(points: List<Pair<Int, Double>>): String {
    if (points.size < 2) return "—"
    val first = points.first().second
    val last = points.last().second
    val span = points.size - 1
    val ratePerStep = (last - first) / span
    val rounded = ratePerStep.roundToInt()
    val sign = if (rounded > 0) "+" else if (rounded < 0) "" else "±"
    return "$sign$rounded lb/wk"
}

@Preview(showBackground = true, backgroundColor = 0xFF17120E)
@Composable
private fun ExerciseDetailScreenPreview() {
    LiftingTrackerTheme(darkTheme = true, dynamicColor = false) {
        ExerciseDetailContent(
            state = ExerciseDetailUiState(
                isReady = true,
                lift = Lift(
                    id = "smith-squat",
                    name = "Smith Machine Squat",
                    sets = 3..5, reps = 5..5,
                    effort = Effort.HIGH,
                    muscle = "Quads", equipment = "Smith",
                ),
                tab = DetailTab.History,
                history = listOf(
                    HistoryEntry(3, "Mon, Apr 13", listOf(HistorySet(225.0, 5), HistorySet(225.0, 5), HistorySet(225.0, 4))),
                    HistoryEntry(2, "Mon, Apr 6", listOf(HistorySet(215.0, 5), HistorySet(215.0, 5), HistorySet(215.0, 5)), notes = "felt easy"),
                    HistoryEntry(1, "Mon, Mar 30", listOf(HistorySet(215.0, 5), HistorySet(215.0, 5), HistorySet(215.0, 4))),
                ),
                alternatives = listOf(
                    Alternative("smith-squat", "hack", "Hack Squat", "Quads", "Machine", 90),
                    Alternative("smith-squat", "pendulum", "Pendulum Squat", "Quads", "Machine", 88),
                ),
                bestWeight = 225.0,
                sessionsCount = 3,
                estimatedOneRm = 262.5,
            ),
            onBack = {},
            onSelectTab = {},
            onSwap = {},
        )
    }
}
