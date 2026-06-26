package com.dbcheck.app.ui.analytics.components

import com.dbcheck.app.ui.analytics.state.AnalyticsOverviewRange
import com.dbcheck.app.ui.analytics.state.AnalyticsSection

internal enum class AnalyticsSectionCard {
    WEEKLY_EXPOSURE,
    HEARING_HEALTH,
    MONTHLY_TREND,
    YEARLY_REPORT,
    HEARING_TEST,
    SLEEP_SETUP,
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
    sleepCardEnabled: Boolean = false,
): List<AnalyticsSectionCard> = when (section) {
        AnalyticsSection.OVERVIEW -> overviewCards(overviewRange, sleepCardEnabled)

        AnalyticsSection.SPECTRAL -> listOf(AnalyticsSectionCard.SPECTRAL_ANALYSIS)

        AnalyticsSection.ENVIRONMENT ->
            environmentCards(
                isRecording = isRecording,
                isProUser = isProUser,
                soundDetectionEnabled = soundDetectionEnabled,
            )
    }

private fun overviewCards(
    overviewRange: AnalyticsOverviewRange,
    sleepCardEnabled: Boolean,
): List<AnalyticsSectionCard> = when (overviewRange) {
        AnalyticsOverviewRange.WEEKLY ->
            buildList {
                add(AnalyticsSectionCard.WEEKLY_EXPOSURE)
                add(AnalyticsSectionCard.HEARING_HEALTH)
                add(AnalyticsSectionCard.YEARLY_REPORT)
                add(AnalyticsSectionCard.HEARING_TEST)
                if (sleepCardEnabled) {
                    add(AnalyticsSectionCard.SLEEP_SETUP)
                }
            }

        AnalyticsOverviewRange.MONTHLY ->
            buildList {
                add(AnalyticsSectionCard.MONTHLY_TREND)
                add(AnalyticsSectionCard.YEARLY_REPORT)
                add(AnalyticsSectionCard.HEARING_TEST)
                if (sleepCardEnabled) {
                    add(AnalyticsSectionCard.SLEEP_SETUP)
                }
            }
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
