package com.dbcheck.app.ui.analytics.components

import com.dbcheck.app.ui.analytics.state.AnalyticsOverviewRange
import com.dbcheck.app.ui.analytics.state.AnalyticsSection

internal enum class AnalyticsSectionCard {
    WEEKLY_EXPOSURE,
    HEARING_HEALTH,
    MONTHLY_TREND,
    YEARLY_REPORT,
    HEARING_TEST,
    SPECTRAL_ANALYSIS,
    SOUND_DETECTION,
    ACTIVE_ENVIRONMENT_MIX,
    ENVIRONMENT_MIX,
}

internal fun analyticsSectionCards(
    section: AnalyticsSection,
    overviewRange: AnalyticsOverviewRange,
    isRecording: Boolean = false,
    isProUser: Boolean = true,
): List<AnalyticsSectionCard> = when (section) {
        AnalyticsSection.OVERVIEW -> overviewCards(overviewRange)
        AnalyticsSection.SPECTRAL -> listOf(AnalyticsSectionCard.SPECTRAL_ANALYSIS)
        AnalyticsSection.ENVIRONMENT -> environmentCards(isRecording = isRecording, isProUser = isProUser)
    }

private fun overviewCards(overviewRange: AnalyticsOverviewRange): List<AnalyticsSectionCard> = when (overviewRange) {
        AnalyticsOverviewRange.WEEKLY ->
            listOf(
                AnalyticsSectionCard.WEEKLY_EXPOSURE,
                AnalyticsSectionCard.HEARING_HEALTH,
                AnalyticsSectionCard.YEARLY_REPORT,
                AnalyticsSectionCard.HEARING_TEST,
            )

        AnalyticsOverviewRange.MONTHLY ->
            listOf(
                AnalyticsSectionCard.MONTHLY_TREND,
                AnalyticsSectionCard.YEARLY_REPORT,
                AnalyticsSectionCard.HEARING_TEST,
            )
    }

private fun environmentCards(isRecording: Boolean, isProUser: Boolean): List<AnalyticsSectionCard> =
    if (isRecording && isProUser) {
        listOf(
            AnalyticsSectionCard.SOUND_DETECTION,
            AnalyticsSectionCard.ACTIVE_ENVIRONMENT_MIX,
            AnalyticsSectionCard.ENVIRONMENT_MIX,
        )
    } else {
        listOf(
            AnalyticsSectionCard.SOUND_DETECTION,
            AnalyticsSectionCard.ENVIRONMENT_MIX,
        )
    }
