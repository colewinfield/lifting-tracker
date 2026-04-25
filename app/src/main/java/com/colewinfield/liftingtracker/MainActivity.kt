package com.colewinfield.liftingtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.colewinfield.liftingtracker.data.AppContainer
import com.colewinfield.liftingtracker.data.AppSettings
import com.colewinfield.liftingtracker.data.ThemeMode
import com.colewinfield.liftingtracker.ui.components.LtNavBar
import com.colewinfield.liftingtracker.ui.components.LtNavItem
import com.colewinfield.liftingtracker.ui.navigation.LtDestination
import com.colewinfield.liftingtracker.ui.screens.DetailTab
import com.colewinfield.liftingtracker.ui.screens.ExerciseDetailScreen
import com.colewinfield.liftingtracker.ui.screens.HistoryScreen
import com.colewinfield.liftingtracker.ui.screens.ProgramScreen
import com.colewinfield.liftingtracker.ui.screens.ProfileScreen
import com.colewinfield.liftingtracker.ui.screens.TodayScreen
import com.colewinfield.liftingtracker.ui.theme.LiftingTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val settingsRepo = remember(context) { AppContainer.settings(context) }
            val settings by settingsRepo.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings.Defaults)
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            LiftingTrackerTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.useDynamicColor,
            ) {
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
    val isTopLevel = LtDestination.entries.any { it.route == currentRoute }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // Per-screen TopAppBars apply WindowInsets.statusBars themselves; the bottom NavBar
        // applies its own navigationBars inset. Zeroing here prevents double-padding.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (isTopLevel) {
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
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = LtDestination.Today.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(LtDestination.Today.route) {
                TodayScreen(
                    onOpenLiftDetail = { liftId, tab ->
                        navController.navigate("exercise/$liftId?tab=${DetailTab.toKey(tab)}")
                    },
                )
            }
            composable(LtDestination.Program.route) { ProgramScreen() }
            composable(LtDestination.History.route) { HistoryScreen() }
            composable(LtDestination.You.route)     { ProfileScreen() }
            composable(
                route = "exercise/{liftId}?tab={tab}",
                arguments = listOf(
                    navArgument("liftId") { type = NavType.StringType },
                    navArgument("tab") {
                        type = NavType.StringType
                        defaultValue = "history"
                    },
                ),
            ) { entry ->
                val liftId = entry.arguments?.getString("liftId") ?: return@composable
                val tabKey = entry.arguments?.getString("tab") ?: "history"
                ExerciseDetailScreen(
                    liftId = liftId,
                    initialTab = DetailTab.fromKey(tabKey),
                    onBack = { navController.popBackStack() },
                )
            }
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
