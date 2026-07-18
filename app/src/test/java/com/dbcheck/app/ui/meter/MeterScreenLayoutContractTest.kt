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

        assertTrue(readout.contains("var liveDetailsExpanded by rememberSaveable { mutableStateOf(false) }"))
        assertTrue(readout.contains("onLiveDetailsExpandedChange = { liveDetailsExpanded = it }"))
        assertTrue(summary.indexOf("DosimeterGaugeCard(") < summary.indexOf("LiveActivityCard("))
        assertTrue(summary.contains("expanded = liveDetailsExpanded"))
        assertTrue(summary.contains("onExpandedChange = onLiveDetailsExpandedChange"))
    }

    @Test
    fun idleStateShowsInstructionWithoutAnotherCta() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/meter/MeterScreen.kt").readText()
        val sessionStatus =
            source
                .substringAfter("private fun MeterSessionStatus")
                .substringBefore("private fun MeterSelectedModeSummary")

        assertTrue(sessionStatus.contains("if (uiState.isRecording)"))
        assertTrue(sessionStatus.contains("R.string.meter_idle_instruction"))
        assertFalse(sessionStatus.contains("DbCheckButton("))
        assertFalse(sessionStatus.contains("MeterControls("))
    }

    @Test
    fun meterHasNoSleepStateImportsCallbacksOrCta() {
        val screen = projectFile("src/main/java/com/dbcheck/app/ui/meter/MeterScreen.kt").readText()
        val state = projectFile("src/main/java/com/dbcheck/app/ui/meter/state/MeterUiState.kt").readText()
        val viewModel = projectFile("src/main/java/com/dbcheck/app/ui/meter/MeterViewModel.kt").readText()
        val navHost = projectFile("src/main/java/com/dbcheck/app/ui/navigation/DbCheckNavHost.kt").readText()
        val meterRoute =
            navHost
                .substringAfter("composable(Screen.Meter.route)")
                .substringBefore("composable(Screen.CameraOverlay.route)")

        listOf(screen, state, viewModel, meterRoute).forEach { source ->
            assertFalse(source.contains("sleepCardEnabled"))
            assertFalse(source.contains("SleepSetupCta"))
            assertFalse(source.contains("onNavigateToSleepSetup"))
            assertFalse(source.contains("onSleepSetupClick"))
        }
    }
}
