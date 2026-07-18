package com.dbcheck.app.ui.analytics.components

import com.dbcheck.app.ui.analytics.state.AnalyticsOverviewRange
import com.dbcheck.app.ui.analytics.state.AnalyticsSection
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsSectionContentTest {
    @Test
    fun overviewWeeklyRangeContainsMeasurementReportAndCompactHearingStatus() {
        assertEquals(
            listOf(
                AnalyticsSectionCard.WEEKLY_EXPOSURE,
                AnalyticsSectionCard.HEARING_STATUS,
                AnalyticsSectionCard.YEARLY_REPORT,
            ),
            analyticsSectionCards(
                section = AnalyticsSection.OVERVIEW,
                overviewRange = AnalyticsOverviewRange.WEEKLY,
            ),
        )
    }

    @Test
    fun overviewMonthlyRangeContainsMeasurementReportAndCompactHearingStatus() {
        assertEquals(
            listOf(
                AnalyticsSectionCard.MONTHLY_TREND,
                AnalyticsSectionCard.HEARING_STATUS,
                AnalyticsSectionCard.YEARLY_REPORT,
            ),
            analyticsSectionCards(
                section = AnalyticsSection.OVERVIEW,
                overviewRange = AnalyticsOverviewRange.MONTHLY,
            ),
        )
    }

    @Test
    fun spectralSectionContainsOnlySpectralCard() {
        assertEquals(
            listOf(AnalyticsSectionCard.SPECTRAL_ANALYSIS),
            analyticsSectionCards(
                section = AnalyticsSection.SPECTRAL,
                overviewRange = AnalyticsOverviewRange.MONTHLY,
            ),
        )
    }

    @Test
    fun environmentSectionContainsSoundDetectionAndHistoricalMixCards() {
        assertEquals(
            listOf(
                AnalyticsSectionCard.SOUND_DETECTION,
                AnalyticsSectionCard.ENVIRONMENT_MIX,
            ),
            analyticsSectionCards(
                section = AnalyticsSection.ENVIRONMENT,
                overviewRange = AnalyticsOverviewRange.WEEKLY,
                isRecording = false,
                isProUser = true,
            ),
        )
    }

    @Test
    fun environmentSectionShowsActiveMixBeforeHistoricalMixWhileProUserRecords() {
        assertEquals(
            listOf(
                AnalyticsSectionCard.SOUND_DETECTION,
                AnalyticsSectionCard.ACTIVE_ENVIRONMENT_MIX,
                AnalyticsSectionCard.ENVIRONMENT_MIX,
            ),
            analyticsSectionCards(
                section = AnalyticsSection.ENVIRONMENT,
                overviewRange = AnalyticsOverviewRange.WEEKLY,
                isRecording = true,
                isProUser = true,
            ),
        )
    }

    @Test
    fun environmentSectionDoesNotShowActiveMixForFreeUserRecording() {
        assertEquals(
            listOf(
                AnalyticsSectionCard.SOUND_DETECTION,
                AnalyticsSectionCard.ENVIRONMENT_MIX,
            ),
            analyticsSectionCards(
                section = AnalyticsSection.ENVIRONMENT,
                overviewRange = AnalyticsOverviewRange.WEEKLY,
                isRecording = true,
                isProUser = false,
            ),
        )
    }
}
