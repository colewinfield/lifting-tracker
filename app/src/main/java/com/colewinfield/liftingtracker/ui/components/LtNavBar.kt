package com.colewinfield.liftingtracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.colewinfield.liftingtracker.ui.theme.LiftingTrackerTheme

data class LtNavItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun LtNavBar(
    items: List<LtNavItem>,
    activeId: String,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        items.forEach { item ->
            NavigationBarItem(
                selected = item.id == activeId,
                onClick = { onItemClick(item.id) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LtNavBarPreview() {
    LiftingTrackerTheme(darkTheme = true, dynamicColor = false) {
        var active by remember { mutableStateOf("today") }
        val items = listOf(
            LtNavItem("today", "Today", Icons.Default.Home),
            LtNavItem("program", "Program", Icons.Default.Star),
            LtNavItem("history", "History", Icons.Default.Settings),
            LtNavItem("you", "You", Icons.Default.Person),
        )
        LtNavBar(items = items, activeId = active, onItemClick = { active = it })
    }
}
