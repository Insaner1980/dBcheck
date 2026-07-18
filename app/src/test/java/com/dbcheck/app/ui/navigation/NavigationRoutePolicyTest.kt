package com.dbcheck.app.ui.navigation

import com.dbcheck.app.R
import com.dbcheck.app.projectFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationRoutePolicyTest {
    @Test
    fun topLevelDestinationsHaveExactFiveItemOrderAndVisibleLabels() {
        assertEquals("hearing", Screen.Hearing.route)
        assertEquals(
            listOf(Screen.Meter, Screen.Analytics, Screen.Hearing, Screen.History, Screen.Settings),
            BottomNavDestination.entries.map(BottomNavDestination::screen),
        )
        assertEquals(
            listOf(
                R.string.nav_meter,
                R.string.nav_analytics,
                R.string.nav_hearing,
                R.string.nav_history,
                R.string.nav_settings,
            ),
            BottomNavDestination.entries.map(BottomNavDestination::labelRes),
        )

        val defaultStrings = projectFile("src/main/res/values/strings.xml").readText()
        assertTrue(defaultStrings.contains("<string name=\"nav_analytics\">Trends</string>"))
        assertTrue(defaultStrings.contains("<string name=\"nav_hearing\">Hearing</string>"))
    }

    @Test
    fun compactBarAndRailUseTheSameDestinationSource() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/navigation/DbCheckNavHost.kt").readText()

        assertTrue(source.contains("BottomNavDestination.entries.map { dest ->"))
        assertTrue(source.contains("BottomNavDestination.entries.forEachIndexed { index, dest ->"))
        assertEquals(2, Regex("BottomNavDestination\\.entries\\.(map|forEachIndexed)").findAll(source).count())
    }

    @Test
    fun settingsRoutesSelectSettingsDestination() {
        assertEquals("showPro", Screen.Settings.ARG_SHOW_PRO)
        assertTrue(Screen.Settings.ROUTE_WITH_ARGS.contains("{${Screen.Settings.ARG_SHOW_PRO}}"))
        assertEquals(Screen.Settings.route, selectedTopLevelRouteFor(Screen.Settings.HOME_ROUTE))
        assertEquals(Screen.Settings.route, selectedTopLevelRouteFor(Screen.Settings.CALIBRATION_ROUTE))
        assertEquals(Screen.Settings.route, selectedTopLevelRouteFor(Screen.Settings.OCTAVE_CALIBRATION_ROUTE))
        assertEquals(Screen.Settings.route, selectedTopLevelRouteFor(Screen.Settings.NOTIFICATIONS_ROUTE))
        assertEquals(Screen.Settings.route, selectedTopLevelRouteFor(Screen.Settings.DATA_PRIVACY_ROUTE))
        assertEquals(Screen.Settings.route, selectedTopLevelRouteFor(Screen.Settings.DISPLAY_ROUTE))
        assertEquals(Screen.Settings.route, selectedTopLevelRouteFor(Screen.Settings.PRO_ABOUT_ROUTE))
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
        assertEquals(Screen.Hearing.route, selectedTopLevelRouteFor(Screen.Hearing.route))
        assertNull(selectedTopLevelRouteFor(Screen.HearingTestSetup.route))
        assertNull(selectedTopLevelRouteFor(Screen.HearingTestActive.route))
        assertNull(selectedTopLevelRouteFor(Screen.HearingRecoverySetup.route))
        assertNull(selectedTopLevelRouteFor(Screen.HearingRecoveryActive.route))
        assertNull(selectedTopLevelRouteFor(Screen.HearingTestResults.route))
        assertNull(selectedTopLevelRouteFor(Screen.HearingTestResults.createRoute(testId = 42L)))
    }

    @Test
    fun hearingRootReselectPreservesRootAndSwitchingStacksRestoresState() {
        val reselectPolicy =
            topLevelNavigationPolicy(
                currentRoute = Screen.Hearing.route,
                targetRoute = Screen.Hearing.route,
            )
        val switchPolicy =
            topLevelNavigationPolicy(
                currentRoute = Screen.History.route,
                targetRoute = Screen.Hearing.route,
            )

        assertTrue(reselectPolicy.isAlreadyAtRoot)
        assertFalse(reselectPolicy.shouldRestoreState)
        assertFalse(switchPolicy.isAlreadyAtRoot)
        assertTrue(switchPolicy.shouldRestoreState)
    }

    @Test
    fun tinnitusPitchRouteDoesNotShowTopLevelNavigation() {
        assertEquals("tinnitus/pitch", Screen.TinnitusPitch.route)
        assertNull(selectedTopLevelRouteFor(Screen.TinnitusPitch.route))
        assertTrue(BottomNavDestination.entries.none { it.screen == Screen.TinnitusPitch })
    }

    @Test
    fun ambientSoundRouteDoesNotShowTopLevelNavigation() {
        assertEquals("ambient/playback", Screen.AmbientSoundPlayback.route)
        assertNull(selectedTopLevelRouteFor(Screen.AmbientSoundPlayback.route))
        assertTrue(BottomNavDestination.entries.none { it.screen == Screen.AmbientSoundPlayback })
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
    fun navHostRegistersTinnitusPitchRouteWithBackAndUpgradeNavigation() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/navigation/DbCheckNavHost.kt").readText()

        assertTrue(source.contains("composable(Screen.TinnitusPitch.route)"))
        assertTrue(source.contains("TinnitusPitchMatcherScreen("))
        assertTrue(source.contains("onNavigateToUpgrade = navigateToUpgrade"))
    }

    @Test
    fun navHostRegistersAmbientSoundPlaybackRouteWithBackAndUpgradeNavigation() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/navigation/DbCheckNavHost.kt").readText()

        assertTrue(source.contains("composable(Screen.AmbientSoundPlayback.route)"))
        assertTrue(source.contains("AmbientSoundPlaybackRoute("))
        assertTrue(source.contains("onNavigateToUpgrade = navigateToUpgrade"))
    }

    @Test
    fun hearingHubActionsOpenExistingFeatureRoutesAndCompatibilityUpgrade() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/navigation/DbCheckNavHost.kt").readText()
        val hearingRoute =
            source
                .substringAfter("composable(Screen.Hearing.route)")
                .substringBefore("composable(Screen.Analytics.route)")

        assertTrue(hearingRoute.contains("HearingScreen("))
        listOf(
            "onNavigateToHearingTest" to "Screen.HearingTestSetup.route",
            "onNavigateToHearingRecovery" to "Screen.HearingRecoverySetup.route",
            "onNavigateToTinnitusPitch" to "Screen.TinnitusPitch.route",
            "onNavigateToAmbientSounds" to "Screen.AmbientSoundPlayback.route",
            "onNavigateToSleepMonitor" to "Screen.SleepSetup.route",
        ).forEach { (callback, route) ->
            assertTrue(hearingRoute.contains("$callback = { navController.navigate($route) }"))
        }
        assertTrue(hearingRoute.contains("onNavigateToUpgrade = navigateToUpgrade"))
        assertTrue(source.contains("onNavigateToHearing = { navigateTo(Screen.Hearing.route) }"))
    }

    @Test
    fun successfulHearingFlowsReturnToHearingRoot() {
        val navSource = projectFile("src/main/java/com/dbcheck/app/ui/navigation/DbCheckNavHost.kt").readText()
        val hearingRoutes = navSource.substringAfter("private fun NavGraphBuilder.hearingTestRoutes")
        val resultsSource =
            projectFile("src/main/java/com/dbcheck/app/ui/hearingtest/results/HearingTestResultsScreen.kt").readText()
        val defaultStrings = projectFile("src/main/res/values/strings.xml").readText()
        val finnishStrings = projectFile("src/main/res/values-fi/strings.xml").readText()

        assertEquals(2, Regex("popBackStack\\(Screen\\.Hearing\\.route, false\\)").findAll(hearingRoutes).count())
        assertFalse(hearingRoutes.contains("popBackStack(Screen.Analytics.route, false)"))
        assertTrue(resultsSource.contains("R.string.hearing_results_back_to_hearing"))
        assertFalse(resultsSource.contains("hearing_results_back_to_analytics"))
        assertTrue(
            defaultStrings.contains("<string name=\"hearing_results_back_to_hearing\">Back to Hearing</string>"),
        )
        assertTrue(
            finnishStrings.contains("<string name=\"hearing_results_back_to_hearing\">Takaisin kuuloon</string>"),
        )
    }

    @Test
    fun topLevelScreensDoNotExposeSettingsShortcuts() {
        val meterSource = projectFile("src/main/java/com/dbcheck/app/ui/meter/MeterScreen.kt").readText()
        val trendsSource = projectFile("src/main/java/com/dbcheck/app/ui/analytics/AnalyticsScreen.kt").readText()
        val historySource = projectFile("src/main/java/com/dbcheck/app/ui/history/HistoryScreen.kt").readText()
        val navSource = projectFile("src/main/java/com/dbcheck/app/ui/navigation/DbCheckNavHost.kt").readText()

        listOf(meterSource, trendsSource, historySource).forEach { source ->
            assertFalse(source.contains("onNavigateToSettings"))
            assertFalse(source.contains("Icons.Outlined.Settings"))
        }
        assertFalse(navSource.contains("onNavigateToSettings ="))
    }

    @Test
    fun topLevelNavigationPolicyKeepsCurrentRootButResetsNestedRoute() {
        val settingsRootPolicy =
            topLevelNavigationPolicy(
                currentRoute = Screen.Settings.HOME_ROUTE,
                targetRoute = Screen.Settings.route,
            )
        val settingsChildPolicy =
            topLevelNavigationPolicy(
                currentRoute = Screen.Settings.DISPLAY_ROUTE,
                targetRoute = Screen.Settings.route,
            )
        val historyDetailPolicy =
            topLevelNavigationPolicy(
                currentRoute = Screen.SessionDetail.route,
                targetRoute = Screen.History.route,
            )

        assertTrue(settingsRootPolicy.isAlreadyAtRoot)
        assertFalse(settingsRootPolicy.shouldRestoreState)
        assertFalse(settingsChildPolicy.isAlreadyAtRoot)
        assertFalse(settingsChildPolicy.shouldRestoreState)
        assertEquals(Screen.Settings.HOME_ROUTE, settingsChildPolicy.navigationRoute)
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
