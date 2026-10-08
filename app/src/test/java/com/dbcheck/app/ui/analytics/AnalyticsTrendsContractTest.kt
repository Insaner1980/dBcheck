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

    @Test
    fun emptyTrendsUsesSubduedUnknownPreviewWhileErrorStaysSeparate() {
        val screen = analyticsSource("AnalyticsScreen.kt")
        val emptyBlock =
            screen
                .substringAfter("is AnalyticsUiState.Empty ->")
                .substringBefore("is AnalyticsUiState.Error ->")
        val errorBlock =
            screen
                .substringAfter("is AnalyticsUiState.Error ->")
                .substringBefore("is AnalyticsUiState.Success ->")
        val preview = analyticsSource("components/AnalyticsEmptyPreviewCard.kt")
        val chartTokens = projectFile("src/main/java/com/dbcheck/app/ui/theme/ChartTokens.kt").readText()
        val defaultStrings = projectFile("src/main/res/values/strings.xml").readText()
        val finnishStrings = projectFile("src/main/res/values-fi/strings.xml").readText()

        assertTrue(emptyBlock.contains("preview = { AnalyticsEmptyPreviewCard() }"))
        assertFalse(errorBlock.contains("AnalyticsEmptyPreviewCard"))
        assertTrue(preview.contains("DbCheckCardEmphasis.Subdued"))
        assertTrue(preview.contains("NeutralChartScaffold()"))
        assertTrue(preview.contains("ChartTokens.PreviewGridAlpha"))
        assertTrue(chartTokens.contains("const val PreviewGridAlpha = 0.5f"))
        listOf(
            "analytics_empty_preview_weekly_exposure",
            "analytics_empty_preview_monthly_trend",
            "analytics_empty_preview_reports",
        ).forEach { resourceName ->
            assertTrue(preview.contains("R.string.$resourceName"))
            assertTrue(defaultStrings.contains("""<string name="$resourceName">"""))
            assertTrue(finnishStrings.contains("""<string name="$resourceName">"""))
        }
        assertTrue(
            defaultStrings.contains(
                """<string name="analytics_empty_preview_unknown">&#8212;</string>""",
            ),
        )
        assertTrue(
            finnishStrings.contains(
                """<string name="analytics_empty_preview_unknown">&#8212;</string>""",
            ),
        )
    }

    private fun analyticsSource(relativePath: String): String =
        projectFile("src/main/java/com/dbcheck/app/ui/analytics/$relativePath").readText()
}
