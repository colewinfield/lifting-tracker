package com.colewinfield.liftingtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.colewinfield.liftingtracker.ui.screens.DayEditScreen
import com.colewinfield.liftingtracker.ui.screens.DetailTab
import com.colewinfield.liftingtracker.ui.screens.ExerciseDetailScreen
import com.colewinfield.liftingtracker.ui.screens.HistoryScreen
import com.colewinfield.liftingtracker.ui.screens.LiftEditScreen
import com.colewinfield.liftingtracker.ui.screens.NEW_ID
import com.colewinfield.liftingtracker.ui.screens.ProgramEditScreen
import com.colewinfield.liftingtracker.ui.screens.ProgramScreen
import com.colewinfield.liftingtracker.ui.screens.ProfileScreen
import com.colewinfield.liftingtracker.ui.screens.TodayScreen
import com.colewinfield.liftingtracker.ui.theme.LiftingTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            // Re-apply edge-to-edge whenever the in-app dark mode changes so the system status
            // bar / nav bar icons flip contrast with the app, not with the OS setting alone.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        LightScrim,
                        DarkScrim,
                    ) { darkTheme },
                )
                onDispose {}
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

// Scrims used behind 3-button nav on devices that don't auto-enforce contrast. Gestural-nav
// devices ignore these and render the bar fully transparent.
private val LightScrim = android.graphics.Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DarkScrim = android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b)

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
            composable(LtDestination.You.route) {
                ProfileScreen(
                    onEditProgram = { navController.navigate("program/edit") },
                )
            }
            composable("program/edit") {
                ProgramEditScreen(
                    onBack = { navController.popBackStack() },
                    onOpenDay = { dayId ->
                        navController.navigate("program/edit/day/$dayId")
                    },
                    onAddDay = {
                        navController.navigate("program/edit/day/$NEW_ID")
                    },
                )
            }
            composable(
                route = "program/edit/day/{dayId}",
                arguments = listOf(navArgument("dayId") { type = NavType.StringType }),
            ) { entry ->
                val dayId = entry.arguments?.getString("dayId") ?: return@composable
                DayEditScreen(
                    dayId = dayId,
                    onBack = { navController.popBackStack() },
                    onOpenLift = { resolvedDayId, liftId ->
                        navController.navigate("program/edit/day/$resolvedDayId/lift/$liftId")
                    },
                )
            }
            composable(
                route = "program/edit/day/{dayId}/lift/{liftId}",
                arguments = listOf(
                    navArgument("dayId") { type = NavType.StringType },
                    navArgument("liftId") { type = NavType.StringType },
                ),
            ) { entry ->
                val dayId = entry.arguments?.getString("dayId") ?: return@composable
                val liftId = entry.arguments?.getString("liftId") ?: return@composable
                LiftEditScreen(
                    liftId = liftId,
                    dayId = dayId,
                    onBack = { navController.popBackStack() },
                )
            }
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
