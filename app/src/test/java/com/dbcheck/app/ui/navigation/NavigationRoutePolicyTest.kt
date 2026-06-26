package com.dbcheck.app.ui.navigation

import com.dbcheck.app.projectFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationRoutePolicyTest {
    @Test
    fun settingsRoutesSelectSettingsDestination() {
        assertEquals("showPro", Screen.Settings.ARG_SHOW_PRO)
        assertTrue(Screen.Settings.ROUTE_WITH_ARGS.contains("{${Screen.Settings.ARG_SHOW_PRO}}"))
        assertEquals(Screen.Settings.route, selectedTopLevelRouteFor(Screen.Settings.ROUTE_WITH_ARGS))
        assertEquals(Screen.Settings.route, selectedTopLevelRouteFor(Screen.Settings.createRoute(showPro = true)))
        assertEquals(Screen.Settings.route, selectedTopLevelRouteFor(Screen.Settings.createRoute(showPro = false)))
    }

    @Test
    fun sessionDetailRoutesSelectHistoryDestination() {
        assertEquals(Screen.History.route, selectedTopLevelRouteFor(Screen.History.route))
        assertEquals(Screen.History.route, selectedTopLevelRouteFor(Screen.SessionDetail.route))
        assertEquals(Screen.History.route, selectedTopLevelRouteFor(Screen.SessionDetail.createRoute(sessionId = 42L)))
    }

    @Test
    fun hearingTestRoutesDoNotShowTopLevelNavigation() {
        assertNull(selectedTopLevelRouteFor(Screen.HearingTestSetup.route))
        assertNull(selectedTopLevelRouteFor(Screen.HearingTestActive.route))
        assertNull(selectedTopLevelRouteFor(Screen.HearingTestResults.route))
        assertNull(selectedTopLevelRouteFor(Screen.HearingTestResults.createRoute(testId = 42L)))
    }

    @Test
    fun cameraOverlayRouteDoesNotShowTopLevelNavigation() {
        assertEquals("camera_overlay", Screen.CameraOverlay.route)
        assertNull(selectedTopLevelRouteFor(Screen.CameraOverlay.route))
        assertTrue(BottomNavDestination.entries.none { it.screen == Screen.CameraOverlay })
    }

    @Test
    fun sleepSetupRouteDoesNotShowTopLevelNavigation() {
        assertEquals("sleep/setup", Screen.SleepSetup.route)
        assertNull(selectedTopLevelRouteFor(Screen.SleepSetup.route))
        assertTrue(BottomNavDestination.entries.none { it.screen == Screen.SleepSetup })
    }

    @Test
    fun navHostRegistersCameraOverlayRouteWithBackNavigation() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/navigation/DbCheckNavHost.kt").readText()

        assertTrue(source.contains("composable(Screen.CameraOverlay.route)"))
        assertTrue(source.contains("CameraOverlayRoute(onBack = { navController.popBackStack() })"))
    }

    @Test
    fun navHostRegistersSleepSetupRouteWithBackAndUpgradeNavigation() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/navigation/DbCheckNavHost.kt").readText()

        assertTrue(source.contains("composable(Screen.SleepSetup.route)"))
        assertTrue(source.contains("SleepSetupRoute("))
        assertTrue(source.contains("onNavigateToUpgrade = navigateToUpgrade"))
    }

    @Test
    fun topLevelNavigationPolicyKeepsCurrentRootButResetsNestedRoute() {
        val settingsRootPolicy =
            topLevelNavigationPolicy(
                currentRoute = Screen.Settings.ROUTE_WITH_ARGS,
                targetRoute = Screen.Settings.route,
            )
        val historyDetailPolicy =
            topLevelNavigationPolicy(
                currentRoute = Screen.SessionDetail.route,
                targetRoute = Screen.History.route,
            )

        assertTrue(settingsRootPolicy.isAlreadyAtRoot)
        assertFalse(settingsRootPolicy.shouldRestoreState)
        assertFalse(historyDetailPolicy.isAlreadyAtRoot)
        assertFalse(historyDetailPolicy.shouldRestoreState)
    }

    @Test
    fun topLevelNavigationPolicyRestoresStateWhenSwitchingStacks() {
        val policy =
            topLevelNavigationPolicy(
                currentRoute = Screen.Analytics.route,
                targetRoute = Screen.History.route,
            )

        assertFalse(policy.isAlreadyAtRoot)
        assertTrue(policy.shouldRestoreState)
    }
}
