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

        listOf(
            "HearingRecoveryCard" to "onStartRecoveryCheck = actions.onNavigateToHearingRecovery",
            "TinnitusPitchCard" to "onOpenPitchMatcher = actions.onNavigateToTinnitusPitch",
            "AmbientSoundCard" to "onOpenAmbientSound = actions.onNavigateToAmbientSounds",
        ).forEach { (cardName, primaryAction) ->
            val cardCall = source.callBlock(cardName)

            assertTrue(
                "$cardName must lock Free and unlock Pro content",
                cardCall.contains("isLocked = !state.isProUser"),
            )
            assertTrue(
                "$cardName must hand locked previews to the shared upgrade route",
                cardCall.contains("onUpgradeClick = actions.onNavigateToUpgrade"),
            )
            assertTrue("$cardName must preserve its unlocked Pro action", cardCall.contains(primaryAction))
        }

        val recoveryCall = source.callBlock("HearingRecoveryCard")
        val tinnitusCall = source.callBlock("TinnitusPitchCard")
        assertTrue(recoveryCall.contains("state = state.hearingRecovery.toCardState()"))
        assertTrue(tinnitusCall.contains("profile = state.tinnitusPitchProfile"))
        assertTrue(source.contains("if (state.sleepCardVisible)"))
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

private fun String.callBlock(callName: String): String {
    val callStart = indexOf("$callName(")
    assertTrue("Missing call $callName", callStart >= 0)
    val openingParenthesis = indexOf('(', startIndex = callStart)
    var depth = 0

    for (index in openingParenthesis until length) {
        when (this[index]) {
            '(' -> depth++

            ')' -> {
                depth--
                if (depth == 0) return substring(callStart, index + 1)
            }
        }
    }

    error("Unclosed call $callName")
}
