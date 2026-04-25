package com.colewinfield.liftingtracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.colewinfield.liftingtracker.data.Alternative
import com.colewinfield.liftingtracker.data.AppContainer
import com.colewinfield.liftingtracker.ui.components.LtChip
import com.colewinfield.liftingtracker.ui.theme.RobotoMono

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwapSheet(
    liftId: String,
    liftName: String,
    onClose: () -> Unit,
    onPick: (Alternative) -> Unit,
) {
    val context = LocalContext.current
    val repo = remember(context) { AppContainer.repository(context) }
    val viewModel: SwapSheetViewModel = viewModel(
        key = "swap-sheet-$liftId",
        factory = SwapSheetViewModel.factory(repo, liftId),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onClose, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp),
        ) {
            // Header (non-scrolling)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
            ) {
                Text(
                    text = "Swap exercise",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Temporary \u2014 just for today. Your program isn't affected.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                ReplacingCard(liftName = liftName)
                Spacer(Modifier.height(12.dp))
                FilterChipRow(
                    active = state.tab,
                    onSelect = viewModel::setTab,
                )
                if (state.tab == SwapTab.BROWSE_ALL) {
                    Spacer(Modifier.height(8.dp))
                    SearchField(
                        query = state.searchQuery,
                        onChange = viewModel::setSearchQuery,
                    )
                }
            }

            // Scrolling list (LazyColumn so 800+ catalog rows render efficiently)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
            ) {
                when (state.tab) {
                    SwapTab.SAME_MUSCLE -> sameMuscleSections(
                        curated = state.curated,
                        sameMuscle = state.sameMuscle,
                        onPick = onPick,
                    )
                    SwapTab.EQUIPMENT -> {
                        if (state.byEquipment.isEmpty()) {
                            item { EmptyHint("No catalog matches for this equipment.") }
                        } else {
                            sectionHeader("MATCHING EQUIPMENT")
                            items(state.byEquipment, key = { "eq-${it.id}" }) { alt ->
                                AlternativeRow(alt = alt, onClick = { onPick(alt) })
                            }
                        }
                    }
                    SwapTab.BROWSE_ALL -> {
                        if (state.browseResults.isEmpty()) {
                            item {
                                EmptyHint(
                                    if (state.searchQuery.isBlank()) "Loading library..."
                                    else "No matches for \u201C${state.searchQuery}\u201D"
                                )
                            }
                        } else {
                            items(state.browseResults, key = { "br-${it.id}" }) { alt ->
                                AlternativeRow(alt = alt, onClick = { onPick(alt) })
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.sameMuscleSections(
    curated: List<Alternative>,
    sameMuscle: List<Alternative>,
    onPick: (Alternative) -> Unit,
) {
    if (curated.isEmpty() && sameMuscle.isEmpty()) {
        item { EmptyHint("No alternatives or catalog matches for this lift.") }
        return
    }
    if (curated.isNotEmpty()) {
        sectionHeader("RECOMMENDED")
        items(curated, key = { "cur-${it.id}" }) { alt ->
            AlternativeRow(alt = alt, onClick = { onPick(alt) })
        }
    }
    if (sameMuscle.isNotEmpty()) {
        sectionHeader("SAME MUSCLE")
        items(sameMuscle, key = { "mu-${it.id}" }) { alt ->
            AlternativeRow(alt = alt, onClick = { onPick(alt) })
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.sectionHeader(title: String) {
    item(key = "header-$title") {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.8.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 2.dp),
        )
    }
}

@Composable
private fun ReplacingCard(liftName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Default.SwapHoriz,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = "REPLACING",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = liftName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun FilterChipRow(active: SwapTab, onSelect: (SwapTab) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        LtChip(
            selected = active == SwapTab.SAME_MUSCLE,
            onClick = { onSelect(SwapTab.SAME_MUSCLE) },
            label = "Same muscle",
        )
        LtChip(
            selected = active == SwapTab.EQUIPMENT,
            onClick = { onSelect(SwapTab.EQUIPMENT) },
            label = "Equipment",
        )
        LtChip(
            selected = active == SwapTab.BROWSE_ALL,
            onClick = { onSelect(SwapTab.BROWSE_ALL) },
            label = "Browse all",
            icon = Icons.Default.Search,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else null,
        placeholder = { Text("Find an exercise...") },
        shape = RoundedCornerShape(24.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    )
}

@Composable
private fun EmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AlternativeRow(alt: Alternative, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Column(Modifier.weight(1f)) {
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
        if (alt.overlapPercent > 0) {
            OverlapPill(percent = alt.overlapPercent)
        }
    }
}

@Composable
private fun OverlapPill(percent: Int) {
    val isHigh = percent >= 90
    val bg = if (isHigh) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow
    val fg = if (isHigh) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = RobotoMono),
            color = fg,
        )
    }
}
