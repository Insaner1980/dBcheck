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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
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
import com.dbcheck.app.ui.history.detail.SessionDetailScreen
import com.dbcheck.app.ui.meter.MeterScreen
import com.dbcheck.app.ui.settings.SettingsScreen
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun DbCheckNavHost(isProUser: Boolean = false, onRestartAfterRestore: () -> Unit = {}) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
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

    DbCheckNavigationFrame(
        showNavigation = showNavigation,
        currentRoute = currentRoute,
        bottomNavItems = bottomNavItems,
        navigateTo = navigateTo,
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
            mainRoutes(
                navController = navController,
                navigateTo = navigateTo,
                navigateToUpgrade = navigateToUpgrade,
                onRestartAfterRestore = onRestartAfterRestore,
            )
            hearingTestRoutes(
                navController = navController,
                isProUser = isProUser,
                navigateToUpgrade = navigateToUpgrade,
            )
        }
    }
}

@Composable
private fun DbCheckNavigationFrame(
    showNavigation: Boolean,
    currentRoute: String?,
    bottomNavItems: List<BottomNavItem>,
    navigateTo: (String) -> Unit,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    val useRail = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() >= 600.dp }
    val colors = DbCheckTheme.colorScheme

    Row(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.material.background),
    ) {
        DbCheckNavigationRail(useRail, showNavigation, currentRoute, navigateTo)
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
            content = content,
        )
    }
}

@Composable
private fun DbCheckNavigationRail(
    useRail: Boolean,
    showNavigation: Boolean,
    currentRoute: String?,
    navigateTo: (String) -> Unit,
) {
    if (!useRail || !showNavigation) return

    NavigationRail(
        containerColor = DbCheckTheme.colorScheme.material.surface,
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

private fun NavGraphBuilder.mainRoutes(
    navController: NavHostController,
    navigateTo: (String) -> Unit,
    navigateToUpgrade: () -> Unit,
    onRestartAfterRestore: () -> Unit,
) {
    composable(Screen.Meter.route) {
        MeterScreen(
            onNavigateToSettings = { navigateTo(Screen.Settings.createRoute()) },
            onNavigateToSessionDetail = { sessionId ->
                navController.navigate(Screen.SessionDetail.createRoute(sessionId))
            },
        )
    }
    composable(Screen.Analytics.route) {
        AnalyticsScreen(
            onNavigateToMeter = { navigateTo(Screen.Meter.route) },
            onNavigateToSettings = { navigateTo(Screen.Settings.createRoute()) },
            onNavigateToHearingTest = { navController.navigate(Screen.HearingTestSetup.route) },
            onNavigateToUpgrade = navigateToUpgrade,
        )
    }
    historyRoutes(navController, navigateTo, navigateToUpgrade)
    settingsRoute(onRestartAfterRestore)
}

private fun NavGraphBuilder.historyRoutes(
    navController: NavHostController,
    navigateTo: (String) -> Unit,
    navigateToUpgrade: () -> Unit,
) {
    composable(Screen.History.route) {
        HistoryScreen(
            onNavigateToMeter = { navigateTo(Screen.Meter.route) },
            onNavigateToSettings = { navigateTo(Screen.Settings.createRoute()) },
            onSessionClick = { sessionId ->
                navController.navigate(Screen.SessionDetail.createRoute(sessionId))
            },
            onNavigateToUpgrade = navigateToUpgrade,
        )
    }
    composable(
        route = Screen.SessionDetail.route,
        arguments =
            listOf(
                navArgument(Screen.SessionDetail.ARG_SESSION_ID) {
                    type = NavType.LongType
                },
            ),
    ) {
        SessionDetailScreen(
            onBack = { navController.popBackStack() },
            onNavigateToUpgrade = navigateToUpgrade,
        )
    }
}

private fun NavGraphBuilder.settingsRoute(onRestartAfterRestore: () -> Unit) {
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
            onRestartAfterRestore = onRestartAfterRestore,
        )
    }
}

private fun NavGraphBuilder.hearingTestRoutes(
    navController: NavHostController,
    isProUser: Boolean,
    navigateToUpgrade: () -> Unit,
) {
    proHearingTestComposable(
        route = Screen.HearingTestSetup.route,
        isProUser = isProUser,
        navigateToUpgrade = navigateToUpgrade,
    ) {
        HearingTestSetupScreen(
            onStartTest = { navController.navigate(Screen.HearingTestActive.route) },
            onBack = { navController.popBackStack() },
        )
    }
    proHearingTestComposable(
        route = Screen.HearingTestActive.route,
        isProUser = isProUser,
        navigateToUpgrade = navigateToUpgrade,
    ) {
        HearingTestActiveScreen(
            onTestComplete = { testId ->
                navController.navigate(Screen.HearingTestResults.createRoute(testId)) {
                    popUpTo(Screen.HearingTestSetup.route) { inclusive = true }
                }
            },
        )
    }
    proHearingTestComposable(
        route = Screen.HearingTestResults.route,
        isProUser = isProUser,
        navigateToUpgrade = navigateToUpgrade,
        arguments =
            listOf(
                navArgument(Screen.HearingTestResults.ARG_TEST_ID) {
                    type = NavType.LongType
                },
            ),
    ) {
        HearingTestResultsScreen(
            onSave = { navController.popBackStack(Screen.Analytics.route, false) },
        )
    }
}

private fun NavGraphBuilder.proHearingTestComposable(
    route: String,
    isProUser: Boolean,
    navigateToUpgrade: () -> Unit,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable (NavBackStackEntry) -> Unit,
) {
    composable(
        route = route,
        arguments = arguments,
    ) { backStackEntry ->
        RequireProHearingTestRoute(
            access = HearingTestRouteAccessPolicy.accessFor(isProUser),
            onNavigateToUpgrade = navigateToUpgrade,
        ) {
            content(backStackEntry)
        }
    }
}

@Composable
private fun RequireProHearingTestRoute(
    access: HearingTestRouteAccess,
    onNavigateToUpgrade: () -> Unit,
    content: @Composable () -> Unit,
) {
    val currentOnNavigateToUpgrade by rememberUpdatedState(onNavigateToUpgrade)

    when (access) {
        HearingTestRouteAccess.Allowed -> content()

        HearingTestRouteAccess.UpgradeRequired -> {
            LaunchedEffect(Unit) {
                currentOnNavigateToUpgrade()
            }
        }
    }
}
