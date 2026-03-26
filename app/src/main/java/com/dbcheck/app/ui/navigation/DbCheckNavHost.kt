package com.dbcheck.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dbcheck.app.ui.analytics.AnalyticsScreen
import com.dbcheck.app.ui.components.BottomNavBar
import com.dbcheck.app.ui.components.BottomNavItem
import com.dbcheck.app.ui.hearingtest.active.HearingTestActiveScreen
import com.dbcheck.app.ui.hearingtest.results.HearingTestResultsScreen
import com.dbcheck.app.ui.hearingtest.setup.HearingTestSetupScreen
import com.dbcheck.app.ui.history.HistoryScreen
import com.dbcheck.app.ui.meter.MeterScreen
import com.dbcheck.app.ui.settings.SettingsScreen
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun DbCheckNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = BottomNavDestination.entries.map { dest ->
        BottomNavItem(
            label = dest.label,
            selectedIcon = dest.selectedIcon,
            unselectedIcon = dest.unselectedIcon,
            route = dest.screen.route,
        )
    }

    val colors = DbCheckTheme.colorScheme

    // Hide bottom nav during hearing test flow
    val showBottomBar = currentRoute in BottomNavDestination.entries.map { it.screen.route }

    Scaffold(
        containerColor = colors.material.background,
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    items = bottomNavItems,
                    currentRoute = currentRoute,
                    onItemClick = { item ->
                        navController.navigate(item.route) {
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
            startDestination = Screen.Meter.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable(Screen.Meter.route) {
                MeterScreen(
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(Screen.Analytics.route) {
                AnalyticsScreen()
            }
            composable(Screen.History.route) {
                HistoryScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            // Hearing Test flow (Phase 2)
            composable(Screen.HearingTestSetup.route) {
                HearingTestSetupScreen(
                    onStartTest = {
                        navController.navigate(Screen.HearingTestActive.route)
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Screen.HearingTestActive.route) {
                HearingTestActiveScreen(
                    onTestComplete = {
                        navController.navigate(Screen.HearingTestResults.route.replace("{testId}", "0")) {
                            popUpTo(Screen.HearingTestSetup.route) { inclusive = true }
                        }
                    },
                )
            }
            composable(Screen.HearingTestResults.route) {
                HearingTestResultsScreen(
                    onSave = { navController.popBackStack(Screen.Analytics.route, false) },
                    onShare = { /* TODO: Share intent */ },
                )
            }
        }
    }
}
