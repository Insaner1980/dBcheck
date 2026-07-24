package com.dbcheck.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dbcheck.app.domain.hearingtest.HearingTestMode
import com.dbcheck.app.ui.ambient.AmbientSoundPlaybackRoute
import com.dbcheck.app.ui.analytics.AnalyticsScreen
import com.dbcheck.app.ui.analytics.AnalyticsScreenActions
import com.dbcheck.app.ui.camera.CameraOverlayRoute
import com.dbcheck.app.ui.components.BottomNavBar
import com.dbcheck.app.ui.components.BottomNavItem
import com.dbcheck.app.ui.components.DbCheckNavigationIconPill
import com.dbcheck.app.ui.hearing.HearingScreen
import com.dbcheck.app.ui.hearing.HearingScreenActions
import com.dbcheck.app.ui.hearingtest.active.HearingTestActiveScreen
import com.dbcheck.app.ui.hearingtest.results.HearingTestResultsScreen
import com.dbcheck.app.ui.hearingtest.setup.HearingRecoverySetupScreen
import com.dbcheck.app.ui.hearingtest.setup.HearingTestSetupScreen
import com.dbcheck.app.ui.history.HistoryScreen
import com.dbcheck.app.ui.history.detail.SessionDetailScreen
import com.dbcheck.app.ui.meter.MeterScreen
import com.dbcheck.app.ui.settings.SettingsCalibrationPage
import com.dbcheck.app.ui.settings.SettingsDataPrivacyPage
import com.dbcheck.app.ui.settings.SettingsDisplayPage
import com.dbcheck.app.ui.settings.SettingsHomePage
import com.dbcheck.app.ui.settings.SettingsNotificationsPage
import com.dbcheck.app.ui.settings.SettingsOctaveCalibrationPage
import com.dbcheck.app.ui.settings.SettingsProAboutPage
import com.dbcheck.app.ui.settings.SettingsViewModel
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

        navController.navigate(policy.navigationRoute) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = policy.shouldRestoreState
            }
            launchSingleTop = true
            restoreState = policy.shouldRestoreState
        }
    }

    val navigateToUpgrade: () -> Unit = {
        settingsLegacyRedirectPlan(showPro = true).routes.forEachIndexed { index, destination ->
            navController.navigate(destination) {
                if (index == 0) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                }
                launchSingleTop = true
            }
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
            hearingTestRoutes(navController, navigateToUpgrade)
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
        BottomNavDestination.entries.forEachIndexed { index, dest ->
            val selected = currentRoute == dest.screen.route
            val label = stringResource(dest.labelRes)
            DbCheckNavigationRailItem(
                destination = dest,
                selected = selected,
                label = label,
                onClick = { navigateTo(dest.screen.route) },
            )
            if (index != BottomNavDestination.entries.lastIndex) {
                Spacer(Modifier.height(DbCheckTheme.spacing.groupGap))
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun DbCheckNavigationRailItem(
    destination: BottomNavDestination,
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    val colors = DbCheckTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val selectedStateDescription = stringResource(com.dbcheck.app.R.string.a11y_selected)
    val notSelectedStateDescription = stringResource(com.dbcheck.app.R.string.a11y_not_selected)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = label
                    stateDescription =
                        if (selected) {
                            selectedStateDescription
                        } else {
                            notSelectedStateDescription
                        }
                }.selectable(
                    selected = selected,
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Tab,
                    onClick = onClick,
                ).padding(horizontal = DbCheckTheme.spacing.space2, vertical = DbCheckTheme.spacing.space1),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space1),
    ) {
        DbCheckNavigationIconPill(
            selected = selected,
            selectedIcon = destination.selectedIcon,
            unselectedIcon = destination.unselectedIcon,
        )
        Text(
            text = label,
            style = DbCheckTheme.typography.labelSm,
            color = if (selected) colors.material.primary else colors.material.onSurfaceVariant,
        )
    }
}

internal fun shouldUseNavigationRail(windowWidthDp: Float): Boolean = windowWidthDp >= NAVIGATION_RAIL_BREAKPOINT_DP

internal fun shouldApplyContentNavigationBarPadding(useRail: Boolean, showNavigation: Boolean): Boolean =
    useRail || !showNavigation

internal data class TopLevelNavigationPolicy(
    val isAlreadyAtRoot: Boolean,
    val shouldRestoreState: Boolean,
    val navigationRoute: String,
)

internal data class SettingsLegacyRedirectPlan(val routes: List<String>, val backTargetRoute: String)

internal fun settingsLegacyRedirectPlan(showPro: Boolean): SettingsLegacyRedirectPlan = SettingsLegacyRedirectPlan(
    routes =
        if (showPro) {
            listOf(Screen.Settings.HOME_ROUTE, Screen.Settings.PRO_ABOUT_ROUTE)
        } else {
            listOf(Screen.Settings.HOME_ROUTE)
        },
    backTargetRoute = Screen.Settings.HOME_ROUTE,
)

internal fun selectedTopLevelRouteFor(currentRoute: String?): String? = when {
    currentRoute == Screen.Meter.route -> Screen.Meter.route

    currentRoute == Screen.Analytics.route -> Screen.Analytics.route

    currentRoute == Screen.Hearing.route -> Screen.Hearing.route

    currentRoute == Screen.History.route || currentRoute?.startsWith("${Screen.History.route}/") == true ->
        Screen.History.route

    currentRoute == Screen.Settings.route ||
        currentRoute?.startsWith("${Screen.Settings.route}/") == true ||
        currentRoute?.startsWith("${Screen.Settings.route}?") == true ->
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
        navigationRoute =
            if (targetTopLevelRoute == Screen.Settings.route && isSameTopLevelStack) {
                Screen.Settings.HOME_ROUTE
            } else {
                targetRoute
            },
    )
}

private fun isTopLevelRootRoute(currentRoute: String?, topLevelRoute: String): Boolean = when (topLevelRoute) {
    Screen.Settings.route ->
        currentRoute == Screen.Settings.HOME_ROUTE

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
            onNavigateToSessionDetail = { sessionId ->
                navController.navigate(Screen.SessionDetail.createRoute(sessionId))
            },
            onNavigateToCameraOverlay = {
                navController.navigate(Screen.CameraOverlay.route)
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
        ProRouteAccessGate(onNavigateToUpgrade = navigateToUpgrade) {
            TinnitusPitchMatcherScreen(
                onBack = { navController.popBackStack() },
                onNavigateToUpgrade = navigateToUpgrade,
            )
        }
    }
    composable(Screen.AmbientSoundPlayback.route) {
        ProRouteAccessGate(onNavigateToUpgrade = navigateToUpgrade) {
            AmbientSoundPlaybackRoute(
                onBack = { navController.popBackStack() },
                onNavigateToUpgrade = navigateToUpgrade,
            )
        }
    }
    composable(Screen.Hearing.route) {
        HearingScreen(
            actions =
                HearingScreenActions(
                    onNavigateToHearingTest = { navController.navigate(Screen.HearingTestSetup.route) },
                    onNavigateToHearingRecovery = { navController.navigate(Screen.HearingRecoverySetup.route) },
                    onNavigateToTinnitusPitch = { navController.navigate(Screen.TinnitusPitch.route) },
                    onNavigateToAmbientSounds = { navController.navigate(Screen.AmbientSoundPlayback.route) },
                    onNavigateToSleepMonitor = { navController.navigate(Screen.SleepSetup.route) },
                    onNavigateToUpgrade = navigateToUpgrade,
                ),
        )
    }
    composable(Screen.Analytics.route) {
        AnalyticsScreen(
            actions =
                AnalyticsScreenActions(
                    onNavigateToMeter = { navigateTo(Screen.Meter.route) },
                    onNavigateToHearing = { navigateTo(Screen.Hearing.route) },
                    onNavigateToUpgrade = navigateToUpgrade,
                ),
        )
    }
    historyRoutes(navController, navigateTo, navigateToUpgrade)
    settingsRoutes(navController, onRestartAfterRestore)
}

private fun NavGraphBuilder.historyRoutes(
    navController: NavHostController,
    navigateTo: (String) -> Unit,
    navigateToUpgrade: () -> Unit,
) {
    composable(Screen.History.route) {
        HistoryScreen(
            onNavigateToMeter = { navigateTo(Screen.Meter.route) },
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

private fun NavGraphBuilder.settingsRoutes(navController: NavHostController, onRestartAfterRestore: () -> Unit) {
    navigation(
        startDestination = Screen.Settings.HOME_ROUTE,
        route = Screen.Settings.route,
    ) {
        composable(Screen.Settings.HOME_ROUTE) { backStackEntry ->
            settingsGraphViewModel(navController, backStackEntry)
            SettingsHomePage(
                onNavigate = navController::navigate,
            )
        }
        composable(Screen.Settings.CALIBRATION_ROUTE) { backStackEntry ->
            val viewModel = settingsGraphViewModel(navController, backStackEntry)
            SettingsCalibrationPage(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenOctaveCalibration = { navController.navigate(Screen.Settings.OCTAVE_CALIBRATION_ROUTE) },
            )
        }
        composable(Screen.Settings.OCTAVE_CALIBRATION_ROUTE) { backStackEntry ->
            val viewModel = settingsGraphViewModel(navController, backStackEntry)
            SettingsOctaveCalibrationPage(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Settings.NOTIFICATIONS_ROUTE) { backStackEntry ->
            val viewModel = settingsGraphViewModel(navController, backStackEntry)
            SettingsNotificationsPage(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Settings.DATA_PRIVACY_ROUTE) { backStackEntry ->
            val viewModel = settingsGraphViewModel(navController, backStackEntry)
            SettingsDataPrivacyPage(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onRestartAfterRestore = onRestartAfterRestore,
            )
        }
        composable(Screen.Settings.DISPLAY_ROUTE) { backStackEntry ->
            val viewModel = settingsGraphViewModel(navController, backStackEntry)
            SettingsDisplayPage(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Settings.PRO_ABOUT_ROUTE) { backStackEntry ->
            val viewModel = settingsGraphViewModel(navController, backStackEntry)
            SettingsProAboutPage(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
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
        val redirectPlan = settingsLegacyRedirectPlan(showPro)
        LaunchedEffect(redirectPlan) {
            redirectPlan.routes.forEachIndexed { index, destination ->
                navController.navigate(destination) {
                    if (index == 0) {
                        popUpTo(Screen.Settings.ROUTE_WITH_ARGS) { inclusive = true }
                    }
                    launchSingleTop = true
                }
            }
        }
    }
}

@Composable
private fun settingsGraphViewModel(
    navController: NavHostController,
    backStackEntry: androidx.navigation.NavBackStackEntry,
): SettingsViewModel {
    val parentEntry = remember(backStackEntry) {
        navController.getBackStackEntry(Screen.Settings.route)
    }
    return hiltViewModel(parentEntry)
}

private fun NavGraphBuilder.hearingTestRoutes(navController: NavHostController, navigateToUpgrade: () -> Unit) {
    composable(Screen.HearingTestSetup.route) {
        HearingTestSetupScreen(
            onStartTest = { navController.navigate(Screen.HearingTestActive.route) },
            onBack = { navController.popBackStack() },
        )
    }
    composable(Screen.HearingRecoverySetup.route) {
        ProRouteAccessGate(onNavigateToUpgrade = navigateToUpgrade) {
            HearingRecoverySetupScreen(
                onStartCheck = { navController.navigate(Screen.HearingRecoveryActive.route) },
                onBack = { navController.popBackStack() },
            )
        }
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
        ProRouteAccessGate(onNavigateToUpgrade = navigateToUpgrade) {
            HearingTestActiveScreen(
                mode = HearingTestMode.RECOVERY,
                onTestComplete = {
                    navController.popBackStack(Screen.Hearing.route, false)
                },
            )
        }
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
            onSave = { navController.popBackStack(Screen.Hearing.route, false) },
        )
    }
}
