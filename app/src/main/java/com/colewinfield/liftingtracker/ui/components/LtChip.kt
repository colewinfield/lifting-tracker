package com.colewinfield.liftingtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.colewinfield.liftingtracker.ui.theme.LiftingTrackerTheme

@Composable
fun LtChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val iconSlot: (@Composable () -> Unit)? = when {
        selected -> {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        }
        icon != null -> {
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        }
        else -> null
    }
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
        enabled = enabled,
        leadingIcon = iconSlot,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF17120E)
@Composable
private fun LtChipPreview() {
    LiftingTrackerTheme(darkTheme = true, dynamicColor = false) {
        var chestSel by remember { mutableStateOf(true) }
        var backSel by remember { mutableStateOf(false) }
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LtChip(selected = chestSel, onClick = { chestSel = !chestSel }, label = "Chest")
            LtChip(selected = backSel, onClick = { backSel = !backSel }, label = "Back")
            LtChip(
                selected = false,
                onClick = {},
                label = "Favorite",
                icon = Icons.Default.Star,
            )
        }
    }
}
