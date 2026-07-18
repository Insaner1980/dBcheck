package com.dbcheck.app.ui.analytics.components

import com.dbcheck.app.ui.analytics.state.AnalyticsOverviewRange
import com.dbcheck.app.ui.analytics.state.AnalyticsSection
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsSectionCardTest {
    @Test
    fun environmentCardsHideSoundDetectionWhenFeatureToggleIsOff() {
        val cards =
            analyticsSectionCards(
                section = AnalyticsSection.ENVIRONMENT,
                overviewRange = AnalyticsOverviewRange.WEEKLY,
                isRecording = false,
                isProUser = true,
                soundDetectionEnabled = false,
            )

        assertEquals(listOf(AnalyticsSectionCard.ENVIRONMENT_MIX), cards)
    }

    @Test
    fun recordingEnvironmentCardsKeepActiveMixWhenSoundDetectionToggleIsOff() {
        val cards =
            analyticsSectionCards(
                section = AnalyticsSection.ENVIRONMENT,
                overviewRange = AnalyticsOverviewRange.WEEKLY,
                isRecording = true,
                isProUser = true,
                soundDetectionEnabled = false,
            )

        assertEquals(
            listOf(
                AnalyticsSectionCard.ACTIVE_ENVIRONMENT_MIX,
                AnalyticsSectionCard.ENVIRONMENT_MIX,
            ),
            cards,
        )
    }

    @Test
    fun overviewCardsNeverIncludeHearingToolsOrSleep() {
        val cards =
            analyticsSectionCards(
                section = AnalyticsSection.OVERVIEW,
                overviewRange = AnalyticsOverviewRange.WEEKLY,
            )

        assertEquals(
            listOf(
                AnalyticsSectionCard.WEEKLY_EXPOSURE,
                AnalyticsSectionCard.HEARING_STATUS,
                AnalyticsSectionCard.YEARLY_REPORT,
            ),
            cards,
        )
    }
}
