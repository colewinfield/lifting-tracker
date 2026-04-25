package com.colewinfield.liftingtracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.colewinfield.liftingtracker.ui.theme.LiftingTrackerTheme

enum class LtTopAppBarVariant { Small, Center, Medium, Large }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LtTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    variant: LtTopAppBarVariant = LtTopAppBarVariant.Small,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val titleContent: @Composable () -> Unit = {
        if (subtitle != null) {
            Column {
                Text(title)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Text(title)
        }
    }
    when (variant) {
        LtTopAppBarVariant.Small -> TopAppBar(
            title = titleContent,
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = actions,
            scrollBehavior = scrollBehavior,
        )
        LtTopAppBarVariant.Center -> CenterAlignedTopAppBar(
            title = titleContent,
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = actions,
            scrollBehavior = scrollBehavior,
        )
        LtTopAppBarVariant.Medium -> MediumTopAppBar(
            title = titleContent,
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = actions,
            scrollBehavior = scrollBehavior,
        )
        LtTopAppBarVariant.Large -> LargeTopAppBar(
            title = titleContent,
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = actions,
            scrollBehavior = scrollBehavior,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, backgroundColor = 0xFF17120E)
@Composable
private fun LtTopAppBarPreview() {
    LiftingTrackerTheme(darkTheme = true, dynamicColor = false) {
        LtTopAppBar(
            title = "Today",
            subtitle = "Lower 1 · 6 lifts · 45 min",
            variant = LtTopAppBarVariant.Medium,
            navigationIcon = {
                IconButton(onClick = {}) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
            },
        )
    }
}
