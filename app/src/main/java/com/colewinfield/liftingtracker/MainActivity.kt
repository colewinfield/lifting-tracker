package com.colewinfield.liftingtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.colewinfield.liftingtracker.ui.components.LtNavBar
import com.colewinfield.liftingtracker.ui.components.LtNavItem
import com.colewinfield.liftingtracker.ui.navigation.LtDestination
import com.colewinfield.liftingtracker.ui.screens.HistoryScreen
import com.colewinfield.liftingtracker.ui.screens.ProgramScreen
import com.colewinfield.liftingtracker.ui.screens.TodayScreen
import com.colewinfield.liftingtracker.ui.screens.YouScreen
import com.colewinfield.liftingtracker.ui.theme.LiftingTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LiftingTrackerTheme {
                LiftingTrackerApp()
            }
        }
    }
}

@Composable
fun LiftingTrackerApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: LtDestination.Today.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // Per-screen TopAppBars apply WindowInsets.statusBars themselves; the bottom NavBar
        // applies its own navigationBars inset. Zeroing here prevents double-padding.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            LtNavBar(
                items = LtDestination.entries.map {
                    LtNavItem(id = it.route, label = it.label, icon = it.icon)
                },
                activeId = currentRoute,
                onItemClick = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = LtDestination.Today.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(LtDestination.Today.route)   { TodayScreen() }
            composable(LtDestination.Program.route) { ProgramScreen() }
            composable(LtDestination.History.route) { HistoryScreen() }
            composable(LtDestination.You.route)     { YouScreen() }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LiftingTrackerAppPreview() {
    LiftingTrackerTheme(darkTheme = true, dynamicColor = false) {
        LiftingTrackerApp()
    }
}
