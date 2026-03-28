package com.dbcheck.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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

    val configuration = LocalConfiguration.current
    val useRail = configuration.screenWidthDp >= 600

    val colors = DbCheckTheme.colorScheme
    val showNavigation =
        BottomNavDestination.entries.any { dest ->
            currentRoute?.startsWith(dest.screen.route) == true
        }

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val navigateToUpgrade: () -> Unit = {
        navController.navigate(Screen.Settings.createRoute(showPro = true)) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
        }
    }

    val bottomNavItems =
        BottomNavDestination.entries.map { dest ->
            BottomNavItem(
                label = dest.label,
                selectedIcon = dest.selectedIcon,
                unselectedIcon = dest.unselectedIcon,
                route = dest.screen.route,
            )
        }

    Row(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.material.background),
    ) {
        // NavigationRail for tablets and foldables (screenWidth >= 600dp)
        if (useRail && showNavigation) {
            NavigationRail(
                containerColor = colors.material.surface,
                modifier = Modifier.statusBarsPadding(),
            ) {
                Spacer(Modifier.weight(1f))
                BottomNavDestination.entries.forEach { dest ->
                    val selected = currentRoute == dest.screen.route
                    NavigationRailItem(
                        selected = selected,
                        onClick = { navigateTo(dest.screen.route) },
                        icon = {
                            Icon(
                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                contentDescription = dest.label,
                            )
                        },
                        label = { Text(dest.label) },
                    )
                }
                Spacer(Modifier.weight(1f))
            }
        }

        Scaffold(
            modifier = Modifier.weight(1f),
            containerColor = colors.material.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!useRail && showNavigation) {
                    BottomNavBar(
                        items = bottomNavItems,
                        currentRoute = currentRoute,
                        onItemClick = { item -> navigateTo(item.route) },
                    )
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Meter.route,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .statusBarsPadding(),
            ) {
                composable(Screen.Meter.route) {
                    MeterScreen(
                        onNavigateToSettings = { navigateTo(Screen.Settings.createRoute()) },
                    )
                }
                composable(Screen.Analytics.route) {
                    AnalyticsScreen(
                        onNavigateToHearingTest = {
                            navController.navigate(Screen.HearingTestSetup.route)
                        },
                        onNavigateToUpgrade = navigateToUpgrade,
                    )
                }
                composable(Screen.History.route) {
                    HistoryScreen()
                }
                composable(
                    route = Screen.Settings.ROUTE_WITH_ARGS,
                    arguments =
                        listOf(
                            navArgument("showPro") {
                                type = NavType.BoolType
                                defaultValue = false
                            },
                        ),
                ) { backStackEntry ->
                    val showPro = backStackEntry.arguments?.getBoolean("showPro") ?: false
                    SettingsScreen(
                        scrollToProCard = showPro,
                        onNavigateToUpgrade = navigateToUpgrade,
                    )
                }

                // Hearing Test flow
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
                        onShare = { },
                    )
                }
            }
        }
    }
}
