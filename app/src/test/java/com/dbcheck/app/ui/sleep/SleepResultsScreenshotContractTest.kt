package com.dbcheck.app.ui.sleep

import com.dbcheck.app.projectFile
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepResultsScreenshotContractTest {
    @Test
    fun sleepResultsCardHasRegisteredScreenshotPreview() {
        val source = projectFile("src/screenshotTest/kotlin/com/dbcheck/app/ComponentScreenshotTests.kt").readText()

        assertTrue(source.contains("fun SleepResultsCardPreview()"))
        assertTrue(source.contains("SleepResultsCard("))
        assertTrue(source.contains("SleepResultsUiState("))
        assertTrue(source.contains("fun SleepInsightsCardPreview()"))
        assertTrue(source.contains("SleepInsightsCard("))
        assertTrue(source.contains("SleepInsightsUiState("))
    }

    @Test
    fun sessionDetailOwnsSleepResultsCard() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/history/detail/SessionDetailScreen.kt").readText()

        assertTrue(source.contains("SleepResultsCard("))
        assertTrue(source.contains("state.sleepResults"))
        assertTrue(source.contains("SleepInsightsCard("))
        assertTrue(source.contains("state.sleepInsights"))
        assertTrue(source.contains("R.string.sleep_results_title"))
        assertTrue(source.contains("R.string.sleep_insights_title"))
    }
}
