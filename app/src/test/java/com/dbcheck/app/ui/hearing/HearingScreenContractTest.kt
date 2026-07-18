package com.dbcheck.app.ui.hearing

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path
import kotlin.io.path.readText

class HearingScreenContractTest {
    @Test
    fun hearingHubRendersSectionsInRequiredOrder() {
        val source = hearingSource("HearingScreen.kt")
        val content = source.substringAfter("private fun HearingScreenContent")

        assertOrdered(
            content,
            "HearingStatusSection(",
            "HearingTestCta(",
            "HearingRecoveryCard(",
            "TinnitusPitchCard(",
            "VoiceBaselineCard(",
            "HearingToolsSection(",
        )
    }

    @Test
    fun hearingScreenCollectsLifecycleAwareStateAndOwnsVoiceBaselineAction() {
        val source = hearingSource("HearingScreen.kt")

        assertTrue(source.contains("viewModel.uiState.collectAsStateWithLifecycle()"))
        assertTrue(source.contains("onCalibrateVoiceBaseline = viewModel::calibrateVoiceBaseline"))
    }

    @Test
    fun hearingActionsExposeAllNavigationHandoffs() {
        val source = hearingSource("HearingScreenActions.kt")

        listOf(
            "onNavigateToHearingTest: () -> Unit",
            "onNavigateToHearingRecovery: () -> Unit",
            "onNavigateToTinnitusPitch: () -> Unit",
            "onNavigateToAmbientSounds: () -> Unit",
            "onNavigateToSleepMonitor: () -> Unit",
            "onNavigateToUpgrade: () -> Unit",
        ).forEach { callback -> assertTrue("Missing action $callback", source.contains(callback)) }
    }

    @Test
    fun hearingStatusAndLatestTestRenderHonestNoDataAndResultStates() {
        val source = hearingSource("HearingScreen.kt")

        assertTrue(source.contains("hearingHealthSummary.toCardState()"))
        assertTrue(source.contains("HearingTestUiState.NoResult"))
        assertTrue(source.contains("is HearingTestUiState.Result"))
        assertTrue(source.contains("R.string.hearing_hub_status_no_data"))
        assertTrue(source.contains("R.string.hearing_hub_latest_test_no_result"))
        assertTrue(source.contains("R.string.hearing_hub_latest_test_result"))
    }

    @Test
    fun hearingCardsPreserveEffectiveFreeProAndSleepContracts() {
        val source = hearingSource("HearingScreen.kt")

        assertTrue(source.contains("isLocked = !state.isProUser"))
        assertTrue(source.contains("state = state.hearingRecovery.toCardState()"))
        assertTrue(source.contains("profile = state.tinnitusPitchProfile"))
        assertTrue(source.contains("if (state.sleepCardVisible)"))
        assertTrue(source.contains("onUpgradeClick = actions.onNavigateToUpgrade"))
    }

    @Test
    fun voiceBaselineCardReceivesExactHubGateAndSavedValues() {
        val source = hearingSource("HearingScreen.kt")

        assertTrue(source.contains("levelDb = state.voiceBaselineLevelDb"))
        assertTrue(source.contains("sampleCount = state.voiceBaselineSampleCount"))
        assertTrue(source.contains("canCalibrate = state.canCalibrateVoiceBaseline"))
        assertTrue(source.contains("isLocked = !state.isProUser"))
        assertTrue(source.contains("onCalibrate = onCalibrateVoiceBaseline"))
    }
}

private fun hearingSource(fileName: String): String {
    val file = Path.of("src", "main", "java", "com", "dbcheck", "app", "ui", "hearing", fileName)
    assertTrue("Missing Hearing source $fileName", file.toFile().isFile)
    return file.readText()
}

private fun assertOrdered(source: String, vararg markers: String) {
    var previousIndex = -1
    markers.forEach { marker ->
        val index = source.indexOf(marker)
        assertTrue("Missing marker $marker", index >= 0)
        assertTrue("Marker $marker is out of order", index > previousIndex)
        previousIndex = index
    }
}
