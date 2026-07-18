package com.dbcheck.app.ui.analytics.components

import com.dbcheck.app.ui.analytics.state.AnalyticsOverviewRange
import com.dbcheck.app.ui.analytics.state.AnalyticsSection

internal enum class AnalyticsSectionCard {
    WEEKLY_EXPOSURE,
    HEARING_STATUS,
    MONTHLY_TREND,
    YEARLY_REPORT,
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
    soundDetectionEnabled: Boolean = true,
): List<AnalyticsSectionCard> = when (section) {
        AnalyticsSection.OVERVIEW -> overviewCards(overviewRange)

        AnalyticsSection.SPECTRAL -> listOf(AnalyticsSectionCard.SPECTRAL_ANALYSIS)

        AnalyticsSection.ENVIRONMENT ->
            environmentCards(
                isRecording = isRecording,
                isProUser = isProUser,
                soundDetectionEnabled = soundDetectionEnabled,
            )
    }

private fun overviewCards(overviewRange: AnalyticsOverviewRange): List<AnalyticsSectionCard> = when (overviewRange) {
        AnalyticsOverviewRange.WEEKLY ->
            listOf(
                AnalyticsSectionCard.WEEKLY_EXPOSURE,
                AnalyticsSectionCard.HEARING_STATUS,
                AnalyticsSectionCard.YEARLY_REPORT,
            )

        AnalyticsOverviewRange.MONTHLY ->
            listOf(
                AnalyticsSectionCard.MONTHLY_TREND,
                AnalyticsSectionCard.HEARING_STATUS,
                AnalyticsSectionCard.YEARLY_REPORT,
            )
    }

private fun environmentCards(
    isRecording: Boolean,
    isProUser: Boolean,
    soundDetectionEnabled: Boolean,
): List<AnalyticsSectionCard> {
    val cards = mutableListOf<AnalyticsSectionCard>()
    if (soundDetectionEnabled) {
        cards += AnalyticsSectionCard.SOUND_DETECTION
    }
    if (isRecording && isProUser) {
        cards += AnalyticsSectionCard.ACTIVE_ENVIRONMENT_MIX
    }
    cards += AnalyticsSectionCard.ENVIRONMENT_MIX
    return cards
}
