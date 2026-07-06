package com.dbcheck.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dbcheck.app.domain.hearingtest.HearingTestMode
import com.dbcheck.app.ui.ambient.AmbientSoundPlaybackRoute
import com.dbcheck.app.ui.analytics.AnalyticsScreen
import com.dbcheck.app.ui.analytics.AnalyticsScreenActions
import com.dbcheck.app.ui.camera.CameraOverlayRoute
import com.dbcheck.app.ui.components.BottomNavBar
import com.dbcheck.app.ui.components.BottomNavItem
import com.dbcheck.app.ui.hearingtest.active.HearingTestActiveScreen
import com.dbcheck.app.ui.hearingtest.results.HearingTestResultsScreen
import com.dbcheck.app.ui.hearingtest.setup.HearingRecoverySetupScreen
import com.dbcheck.app.ui.hearingtest.setup.HearingTestSetupScreen
import com.dbcheck.app.ui.history.HistoryScreen
import com.dbcheck.app.ui.history.detail.SessionDetailScreen
import com.dbcheck.app.ui.meter.MeterScreen
import com.dbcheck.app.ui.settings.SettingsScreen
import com.dbcheck.app.ui.sleep.SleepSetupRoute
import com.dbcheck.app.ui.theme.DbCheckTheme
import com.dbcheck.app.ui.tinnitus.TinnitusPitchMatcherScreen

@Composable
fun DbCheckNavHost(onRestartAfterRestore: () -> Unit = {}) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val selectedTopLevelRoute = selectedTopLevelRouteFor(currentRoute)
    val showNavigation = selectedTopLevelRoute != null
    val navigateTo: (String) -> Unit = navigateTo@{ route ->
        val policy = topLevelNavigationPolicy(currentRoute, route)
        if (policy.isAlreadyAtRoot) return@navigateTo

        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = policy.shouldRestoreState
            }
            launchSingleTop = true
            restoreState = policy.shouldRestoreState
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
            val label = stringResource(dest.labelRes)
            BottomNavItem(
                label = label,
                selectedIcon = dest.selectedIcon,
                unselectedIcon = dest.unselectedIcon,
                route = dest.screen.route,
            )
        }

    DbCheckNavigationFrame(
        showNavigation = showNavigation,
        currentRoute = selectedTopLevelRoute,
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
            hearingTestRoutes(navController)
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
    val windowWidthDp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    val useRail = shouldUseNavigationRail(windowWidthDp.value)
    val contentWindowInsets =
        if (shouldApplyContentNavigationBarPadding(useRail, showNavigation)) {
            WindowInsets.navigationBars
        } else {
            WindowInsets(0, 0, 0, 0)
        }
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
            contentWindowInsets = contentWindowInsets,
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
            val label = stringResource(dest.labelRes)
            NavigationRailItem(
                selected = selected,
                onClick = { navigateTo(dest.screen.route) },
                icon = {
                    Icon(
                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = label,
                    )
                },
                label = { Text(label) },
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

internal fun shouldUseNavigationRail(windowWidthDp: Float): Boolean = windowWidthDp >= NAVIGATION_RAIL_BREAKPOINT_DP

internal fun shouldApplyContentNavigationBarPadding(useRail: Boolean, showNavigation: Boolean): Boolean =
    useRail || !showNavigation

internal data class TopLevelNavigationPolicy(val isAlreadyAtRoot: Boolean, val shouldRestoreState: Boolean)

internal fun selectedTopLevelRouteFor(currentRoute: String?): String? = when {
    currentRoute == Screen.Meter.route -> Screen.Meter.route

    currentRoute == Screen.Analytics.route -> Screen.Analytics.route

    currentRoute == Screen.History.route || currentRoute?.startsWith("${Screen.History.route}/") == true ->
        Screen.History.route

    currentRoute == Screen.Settings.route || currentRoute?.startsWith("${Screen.Settings.route}?") == true ->
        Screen.Settings.route

    else -> null
}

internal fun topLevelNavigationPolicy(currentRoute: String?, targetRoute: String): TopLevelNavigationPolicy {
    val targetTopLevelRoute = selectedTopLevelRouteFor(targetRoute) ?: targetRoute
    val isAlreadyAtRoot = isTopLevelRootRoute(currentRoute, targetTopLevelRoute)
    val isSameTopLevelStack = selectedTopLevelRouteFor(currentRoute) == targetTopLevelRoute

    return TopLevelNavigationPolicy(
        isAlreadyAtRoot = isAlreadyAtRoot,
        shouldRestoreState = !isSameTopLevelStack,
    )
}

private fun isTopLevelRootRoute(currentRoute: String?, topLevelRoute: String): Boolean = when (topLevelRoute) {
    Screen.Settings.route ->
        currentRoute == Screen.Settings.route ||
            currentRoute == Screen.Settings.ROUTE_WITH_ARGS ||
            currentRoute?.startsWith("${Screen.Settings.route}?") == true

    else -> currentRoute == topLevelRoute
}

private const val NAVIGATION_RAIL_BREAKPOINT_DP = 600f

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
            onNavigateToCameraOverlay = {
                navController.navigate(Screen.CameraOverlay.route)
            },
            onNavigateToSleepSetup = {
                navController.navigate(Screen.SleepSetup.route)
            },
            onNavigateToUpgrade = navigateToUpgrade,
        )
    }
    composable(Screen.CameraOverlay.route) {
        CameraOverlayRoute(onBack = { navController.popBackStack() })
    }
    composable(Screen.SleepSetup.route) {
        SleepSetupRoute(
            onBack = { navController.popBackStack() },
            onNavigateToUpgrade = navigateToUpgrade,
        )
    }
    composable(Screen.TinnitusPitch.route) {
        TinnitusPitchMatcherScreen(
            onBack = { navController.popBackStack() },
            onNavigateToUpgrade = navigateToUpgrade,
        )
    }
    composable(Screen.AmbientSoundPlayback.route) {
        AmbientSoundPlaybackRoute(
            onBack = { navController.popBackStack() },
            onNavigateToUpgrade = navigateToUpgrade,
        )
    }
    composable(Screen.Analytics.route) {
        AnalyticsScreen(
            actions =
                AnalyticsScreenActions(
                    onNavigateToMeter = { navigateTo(Screen.Meter.route) },
                    onNavigateToSettings = { navigateTo(Screen.Settings.createRoute()) },
                    onNavigateToHearingTest = { navController.navigate(Screen.HearingTestSetup.route) },
                    onNavigateToHearingRecoveryCheck = { navController.navigate(Screen.HearingRecoverySetup.route) },
                    onNavigateToTinnitusPitch = { navController.navigate(Screen.TinnitusPitch.route) },
                    onNavigateToAmbientSound = { navController.navigate(Screen.AmbientSoundPlayback.route) },
                    onNavigateToSleepSetup = { navController.navigate(Screen.SleepSetup.route) },
                    onNavigateToUpgrade = navigateToUpgrade,
                ),
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
                navArgument(Screen.Settings.ARG_SHOW_PRO) {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
    ) { backStackEntry ->
        val showPro = backStackEntry.arguments?.getBoolean(Screen.Settings.ARG_SHOW_PRO) ?: false
        SettingsScreen(
            scrollToProCard = showPro,
            onRestartAfterRestore = onRestartAfterRestore,
        )
    }
}

private fun NavGraphBuilder.hearingTestRoutes(navController: NavHostController) {
    composable(Screen.HearingTestSetup.route) {
        HearingTestSetupScreen(
            onStartTest = { navController.navigate(Screen.HearingTestActive.route) },
            onBack = { navController.popBackStack() },
        )
    }
    composable(Screen.HearingRecoverySetup.route) {
        HearingRecoverySetupScreen(
            onStartCheck = { navController.navigate(Screen.HearingRecoveryActive.route) },
            onBack = { navController.popBackStack() },
        )
    }
    composable(Screen.HearingTestActive.route) {
        HearingTestActiveScreen(
            onTestComplete = { testId ->
                navController.navigate(Screen.HearingTestResults.createRoute(testId)) {
                    popUpTo(Screen.HearingTestSetup.route) { inclusive = true }
                }
            },
        )
    }
    composable(Screen.HearingRecoveryActive.route) {
        HearingTestActiveScreen(
            mode = HearingTestMode.RECOVERY,
            onTestComplete = {
                navController.popBackStack(Screen.Analytics.route, false)
            },
        )
    }
    composable(
        route = Screen.HearingTestResults.route,
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
