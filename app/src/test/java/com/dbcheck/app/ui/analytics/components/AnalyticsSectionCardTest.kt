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
    fun overviewCardsIncludeSleepSetupWhenSleepCardIsEnabled() {
        val cards =
            analyticsSectionCards(
                section = AnalyticsSection.OVERVIEW,
                overviewRange = AnalyticsOverviewRange.WEEKLY,
                sleepCardEnabled = true,
            )

        assertEquals(
            listOf(
                AnalyticsSectionCard.WEEKLY_EXPOSURE,
                AnalyticsSectionCard.HEARING_HEALTH,
                AnalyticsSectionCard.YEARLY_REPORT,
                AnalyticsSectionCard.HEARING_TEST,
                AnalyticsSectionCard.HEARING_RECOVERY,
                AnalyticsSectionCard.TINNITUS_PITCH,
                AnalyticsSectionCard.AMBIENT_SOUND,
                AnalyticsSectionCard.SLEEP_SETUP,
            ),
            cards,
        )
    }

    @Test
    fun overviewCardsIncludeHearingRecoveryAndTinnitusPitchAfterHearingTest() {
        val cards =
            analyticsSectionCards(
                section = AnalyticsSection.OVERVIEW,
                overviewRange = AnalyticsOverviewRange.WEEKLY,
            )

        assertEquals(
            listOf(
                AnalyticsSectionCard.WEEKLY_EXPOSURE,
                AnalyticsSectionCard.HEARING_HEALTH,
                AnalyticsSectionCard.YEARLY_REPORT,
                AnalyticsSectionCard.HEARING_TEST,
                AnalyticsSectionCard.HEARING_RECOVERY,
                AnalyticsSectionCard.TINNITUS_PITCH,
                AnalyticsSectionCard.AMBIENT_SOUND,
            ),
            cards,
        )
    }
}
