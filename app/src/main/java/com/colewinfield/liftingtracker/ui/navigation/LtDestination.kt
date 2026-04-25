package com.colewinfield.liftingtracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

enum class LtDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Today(   route = "today",   label = "Today",   icon = Icons.Default.FitnessCenter),
    Program( route = "program", label = "Program", icon = Icons.Default.CalendarMonth),
    History( route = "history", label = "History", icon = Icons.Default.BarChart),
    You(     route = "you",     label = "You",     icon = Icons.Default.Person),
}
