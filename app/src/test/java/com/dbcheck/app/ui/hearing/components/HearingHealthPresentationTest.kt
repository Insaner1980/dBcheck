package com.dbcheck.app.ui.hearing.components

import com.dbcheck.app.R
import com.dbcheck.app.domain.hearing.HearingHealthStatus
import com.dbcheck.app.domain.hearing.HearingHealthSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HearingHealthPresentationTest {
    @Test
    fun absentSummaryMapsToHonestNoDataPresentation() {
        val presentation = hearingHealthPresentation(summary = null)

        assertEquals(HearingHealthVisual.INFO, presentation.visual)
        assertEquals(HearingHealthText(R.string.hearing_status_row_no_data), presentation.statusText)
        assertNull(presentation.comparisonText)
    }

    @Test
    fun healthStatusesMapToExactSharedVisualAndTextValues() {
        val cases =
            listOf(
                HearingHealthStatus.SAFE to
                    (HearingHealthVisual.SAFE to R.string.hearing_health_safe),
                HearingHealthStatus.WARNING to
                    (HearingHealthVisual.WARNING to R.string.hearing_health_warning),
                HearingHealthStatus.DANGER to
                    (HearingHealthVisual.DANGER to R.string.hearing_health_danger),
            )

        cases.forEach { (status, expected) ->
            val presentation = hearingHealthPresentation(summary(status = status, deltaPercent = 0))

            assertEquals(expected.first, presentation.visual)
            assertEquals(HearingHealthText(expected.second), presentation.statusText)
            assertEquals(HearingHealthText(R.string.hearing_health_today_matches), presentation.comparisonText)
        }
    }

    @Test
    fun positiveComparisonKeepsAboveTextAndSignedMagnitude() {
        val presentation = hearingHealthPresentation(summary(HearingHealthStatus.SAFE, deltaPercent = 12))

        assertEquals(HearingHealthText(R.string.hearing_health_today_above, 12), presentation.comparisonText)
    }

    @Test
    fun negativeComparisonKeepsBelowTextAndAbsoluteMagnitude() {
        val presentation = hearingHealthPresentation(summary(HearingHealthStatus.WARNING, deltaPercent = -12))

        assertEquals(HearingHealthText(R.string.hearing_health_today_below, 12), presentation.comparisonText)
    }

    @Test
    fun zeroComparisonKeepsMatchesTextWithoutFormatArgument() {
        val presentation = hearingHealthPresentation(summary(HearingHealthStatus.DANGER, deltaPercent = 0))

        assertEquals(HearingHealthText(R.string.hearing_health_today_matches), presentation.comparisonText)
    }

    @Test
    fun missingTodayComparisonOmitsComparisonText() {
        val presentation = hearingHealthPresentation(summary(HearingHealthStatus.SAFE, deltaPercent = null))

        assertNull(presentation.comparisonText)
    }

    private fun summary(status: HearingHealthStatus, deltaPercent: Int?): HearingHealthSummary = HearingHealthSummary(
            weeklyAverageDb = 70f,
            healthStatus = status,
            todayVsWeekPercent = deltaPercent,
        )
}
