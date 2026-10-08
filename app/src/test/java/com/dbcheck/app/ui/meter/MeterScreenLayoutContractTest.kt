package com.dbcheck.app.ui.meter

import com.dbcheck.app.projectFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeterScreenLayoutContractTest {
    @Test
    fun readoutUsesExactCompactContentOrder() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/meter/MeterScreen.kt").readText()
        val readout =
            source
                .substringAfter("private fun MeterReadoutContent")
                .substringBefore("private fun MeterSessionStatus")

        val orderedCalls =
            listOf(
                "MeterModeChipRow(",
                "MeterSessionStatus(",
                "CircularGauge(",
                "MeterSelectedModeSummary(",
                "MeterStatsRow(",
                "SoundReferenceCard(",
            )

        assertTrue(orderedCalls.all(readout::contains))
        val positions = orderedCalls.map(readout::indexOf)
        assertEquals(positions.sorted(), positions)
    }

    @Test
    fun recordingControlsStayOutsideScrollableReadout() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/meter/MeterScreen.kt").readText()
        val meterContent =
            source
                .substringAfter("private fun MeterContent")
                .substringBefore("private fun MeterControlsSection")

        assertTrue(meterContent.contains(".weight(1f)"))
        assertTrue(meterContent.contains(".verticalScroll(scrollState)"))
        assertEquals(1, Regex("MeterControlsSection\\(").findAll(meterContent).count())
        assertTrue(meterContent.indexOf("MeterReadoutContent(") < meterContent.indexOf("MeterControlsSection("))
    }

    @Test
    fun scrollViewportHasConditionalNonInteractiveEdgeFadesAboveFixedControls() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/meter/MeterScreen.kt").readText()
        val meterContent =
            source
                .substringAfter("private fun MeterContent")
                .substringBefore("private fun MeterControlsSection")

        assertTrue(meterContent.contains("if (scrollState.canScrollBackward)"))
        assertTrue(meterContent.contains("if (scrollState.canScrollForward)"))
        assertTrue(meterContent.contains("spacing.meterScrollEdgeFade"))
        assertTrue(meterContent.contains("Brush.verticalGradient"))
        assertFalse(meterContent.contains(".clickable("))
        assertFalse(meterContent.contains(".pointerInput("))
        assertTrue(meterContent.indexOf("MeterReadoutContent(") < meterContent.indexOf("MeterControlsSection("))
    }

    @Test
    fun fixedControlsUseThemeSurfaceAndHairlineBoundary() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/meter/MeterScreen.kt").readText()
        val controls =
            source
                .substringAfter("private fun MeterControlsSection")
                .substringBefore("private fun MeterReadoutContent")

        assertTrue(controls.contains("colors.surfaceContainerLowest"))
        assertTrue(controls.contains("colors.ghostBorder"))
        assertTrue(controls.contains("spacing.hairline"))
    }

    @Test
    fun liveDetailsAreSaveableCollapsedByDefaultAndOnlyUsedForLiveSummary() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/meter/MeterScreen.kt").readText()
        val readout =
            source
                .substringAfter("private fun MeterReadoutContent")
                .substringBefore("private fun MeterSessionStatus")
        val summary =
            source
                .substringAfter("private fun MeterSelectedModeSummary")
                .substringBefore("private fun MeterStatsRow")

        assertTrue(source.contains("val liveDetailsExpanded: Boolean = false"))
        assertTrue(readout.contains("mutableStateOf(initialExpansionState.liveDetailsExpanded)"))
        assertTrue(readout.contains("onLiveDetailsExpandedChange = { liveDetailsExpanded = it }"))
        assertTrue(summary.contains("DosimeterGaugeCard("))
        assertTrue(summary.contains("LiveActivityCard("))
        assertTrue(summary.indexOf("DosimeterGaugeCard(") < summary.indexOf("LiveActivityCard("))
        assertTrue(summary.contains("expanded = liveDetailsExpanded"))
        assertTrue(summary.contains("onExpandedChange = onLiveDetailsExpandedChange"))
    }

    @Test
    fun idleInstructionBelongsToGaugeWhileSessionStatusKeepsMetadataAndErrors() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/meter/MeterScreen.kt").readText()
        val sessionStatus =
            source
                .substringAfter("private fun MeterSessionStatus")
                .substringBefore("private fun MeterSelectedModeSummary")
        val readout =
            source
                .substringAfter("private fun MeterReadoutContent")
                .substringBefore("private fun MeterSessionStatus")

        assertTrue(sessionStatus.contains("if (uiState.isRecording)"))
        assertTrue(sessionStatus.contains("MeterSessionInfoBar("))
        assertTrue(sessionStatus.contains("MeterErrorMessage(error = uiState.error)"))
        assertFalse(sessionStatus.contains("R.string.meter_idle_instruction"))
        assertTrue(readout.contains("isRecording = uiState.isRecording"))
        assertFalse(sessionStatus.contains("DbCheckButton("))
        assertFalse(sessionStatus.contains("MeterControls("))
    }

    @Test
    fun meterSleepEntryUsesEffectiveVisibilityAndNavigatesToSetup() {
        val screen = projectFile("src/main/java/com/dbcheck/app/ui/meter/MeterScreen.kt").readText()
        val state = projectFile("src/main/java/com/dbcheck/app/ui/meter/state/MeterUiState.kt").readText()
        val viewModel = projectFile("src/main/java/com/dbcheck/app/ui/meter/MeterViewModel.kt").readText()
        val navHost = projectFile("src/main/java/com/dbcheck/app/ui/navigation/DbCheckNavHost.kt").readText()
        val meterRoute =
            navHost
                .substringAfter("composable(Screen.Meter.route)")
                .substringBefore("composable(Screen.CameraOverlay.route)")

        assertTrue(state.contains("val sleepCardEnabled: Boolean = false"))
        assertTrue(viewModel.contains("sleepCardEnabled = prefs.isProUser && prefs.sleepCardEnabled"))
        assertTrue(screen.contains("import com.dbcheck.app.ui.sleep.components.SleepSetupCta"))
        assertTrue(screen.contains("onNavigateToSleepSetup: () -> Unit = {}"))
        assertTrue(screen.contains("val onSleepSetupClick: () -> Unit"))
        assertTrue(screen.contains("if (uiState.sleepCardEnabled)"))
        assertTrue(screen.contains("onSleepSetupClick = actions.onSleepSetupClick"))
        assertTrue(screen.contains("onOpenSleepSetup = onSleepSetupClick"))
        val readout =
            screen
                .substringAfter("private fun MeterReadoutContent")
                .substringBefore("private fun MeterSessionStatus")
        assertTrue(readout.contains("SleepSetupCta("))
        assertTrue(meterRoute.contains("onNavigateToSleepSetup = {"))
        assertTrue(meterRoute.contains("navController.navigate(Screen.SleepSetup.route)"))
    }
}
