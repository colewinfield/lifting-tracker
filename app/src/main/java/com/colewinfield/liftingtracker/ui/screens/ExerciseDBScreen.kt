package com.colewinfield.liftingtracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.colewinfield.liftingtracker.data.AppContainer
import com.colewinfield.liftingtracker.data.CatalogLift
import com.colewinfield.liftingtracker.ui.components.LtChip
import com.colewinfield.liftingtracker.ui.components.LtTopAppBar
import com.colewinfield.liftingtracker.ui.theme.appColors

/**
 * Exercise DB browser. Mirrors `extras2.jsx` `ExerciseDBScreen()` with the JSX's exact filter
 * taxonomy (All / Chest / Back / Quads / Hamstrings / Shoulders / Biceps / Triceps), pill-shaped
 * search field, and 44dp lift rows with a small effort dot derived from `mechanic` (compound →
 * high CNS, isolation → low, unknown → med). Tapping a row calls [onPick] with the chosen lift
 * and the host route is responsible for popping back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDBScreen(
    onBack: () -> Unit,
    onPick: (CatalogLift) -> Unit,
) {
    val context = LocalContext.current
    val repo = remember(context) { AppContainer.repository(context) }
    val viewModel: ExerciseDBViewModel = viewModel(factory = ExerciseDBViewModel.factory(repo))
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top)),
    ) {
        LtTopAppBar(
            title = "Exercises",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            },
            actions = {
                // JSX trailing search icon. Search field is always visible below, so this is
                // visual parity only — no-op tap.
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                    )
                }
            },
        )

        SearchBar(
            query = state.query,
            onChange = viewModel::setQuery,
            onClear = { viewModel.setQuery("") },
        )

        Spacer(Modifier.height(12.dp))

        FilterChipRow(
            active = state.filter,
            onSelect = viewModel::setFilter,
        )

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            if (state.results.isEmpty()) {
                item {
                    EmptyState(
                        text = when {
                            state.loading -> "Loading\u2026"
                            state.query.isNotBlank() ->
                                "No matches for \u201C${state.query}\u201D"
                            else -> "No exercises in this filter."
                        },
                    )
                }
            } else {
                items(state.results, key = { it.id }) { lift ->
                    LiftRow(lift = lift, onClick = { onPick(lift) })
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    // JSX: 48dp pill, surfaceContainer bg, leading search icon, trailing filter icon. We use a
    // BasicTextField inside a Row to keep the pill's exact height + padding (an OutlinedTextField
    // can't easily collapse to 48dp).
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(
                    MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            text = "Find an exercise\u2026",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    inner()
                },
            )
        }
        if (query.isNotEmpty()) {
            IconButton(onClick = onClear, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            // JSX trailing filter icon when nothing is typed; visual parity, no-op.
            IconButton(onClick = {}, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun FilterChipRow(
    active: ExerciseDbFilter,
    onSelect: (ExerciseDbFilter) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(ExerciseDbFilter.entries.toList(), key = { it.name }) { filter ->
            LtChip(
                selected = active == filter,
                onClick = { onSelect(filter) },
                label = filter.label,
            )
        }
    }
}

@Composable
private fun LiftRow(lift: CatalogLift, onClick: () -> Unit) {
    val effortColor = when (lift.mechanic) {
        "compound" -> MaterialTheme.appColors.effortHigh
        "isolation" -> MaterialTheme.appColors.effortLow
        else -> MaterialTheme.appColors.effortMed
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            // Effort dot, top-right (per JSX `position: 'absolute', top: 2, right: 2`).
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(effortColor),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = lift.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${lift.displayMuscle} \u00B7 ${lift.displayEquipment}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, bottom = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
