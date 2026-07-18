package com.dbcheck.app.ui.analytics

import com.dbcheck.app.projectFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsTrendsContractTest {
    @Test
    fun viewModelDependsOnMeasurementSourcesWithoutHearingRepositories() {
        val source = analyticsSource("AnalyticsViewModel.kt")

        assertTrue(source.contains("HearingHealthSummaryCalculator.calculate("))
        assertFalse(source.contains("HearingTestRepository"))
        assertFalse(source.contains("HearingRecoveryRepository"))
    }

    @Test
    fun analyticsStateContainsNullableSharedSummaryWithoutHearingToolState() {
        val source = analyticsSource("state/AnalyticsUiState.kt")

        assertTrue(source.contains("val hearingHealthSummary: HearingHealthSummary? = null"))
        listOf("HearingRecoveryUiState", "tinnitusPitchProfile", "sleepCardEnabled", "HealthStatus").forEach { symbol ->
            assertFalse("Analytics state must not contain $symbol", source.contains(symbol))
        }
    }

    @Test
    fun trendsScreenOwnsOneHearingHubActionAndNoToolOrSettingsActions() {
        val screen = analyticsSource("AnalyticsScreen.kt")
        val actions = analyticsSource("AnalyticsScreenActions.kt")
        val hearingRowCall = screen.substringAfter("HearingStatusRow(").substringBefore("\n            )")

        assertTrue(screen.contains("HearingStatusRow("))
        assertTrue(hearingRowCall.contains("summary = state.hearingHealthSummary"))
        assertTrue(hearingRowCall.contains("onNavigateToHearing = navigationActions.onNavigateToHearing"))
        assertFalse(hearingRowCall.contains("onNavigateToUpgrade"))
        listOf(
            "HearingTestCta",
            "HearingRecoveryCard",
            "TinnitusPitchCard",
            "AmbientSoundCard",
            "SleepSetupCta",
            "onNavigateToSettings",
        ).forEach { symbol -> assertFalse("Trends must not own $symbol", screen.contains(symbol)) }
        assertTrue(actions.contains("onNavigateToHearing: () -> Unit"))
        listOf(
            "onNavigateToSettings",
            "onNavigateToHearingTest",
            "onNavigateToHearingRecoveryCheck",
            "onNavigateToTinnitusPitch",
            "onNavigateToAmbientSound",
            "onNavigateToSleepSetup",
        ).forEach { action -> assertFalse("Trends actions must not contain $action", actions.contains(action)) }
    }

    @Test
    fun hearingOwnedStatusRowHasHonestNoDataCopyAndSingleHearingAction() {
        val source =
            projectFile("src/main/java/com/dbcheck/app/ui/hearing/components/HearingStatusRow.kt").readText()
        val presentationSource =
            projectFile("src/main/java/com/dbcheck/app/ui/hearing/components/HearingHealthPresentation.kt").readText()

        assertTrue(source.contains("summary: HearingHealthSummary?"))
        assertTrue(source.contains("hearingHealthPresentation(summary)"))
        assertTrue(presentationSource.contains("R.string.hearing_status_row_no_data"))
        assertTrue(source.contains("onNavigateToHearing: () -> Unit"))
        assertTrue(source.contains("sizeIn(minHeight = DbCheckTheme.spacing.space12)"))
    }

    private fun analyticsSource(relativePath: String): String =
        projectFile("src/main/java/com/dbcheck/app/ui/analytics/$relativePath").readText()
}
