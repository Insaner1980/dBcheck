package com.dbcheck.app.service

import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.domain.audio.DecibelReading

internal sealed interface NoiseAlertDecision {
    data class Exposure(val avgDb: Float, val durationMinutes: Int) : NoiseAlertDecision

    data class Peak(val peakDb: Float) : NoiseAlertDecision
}

internal class NoiseAlertEvaluator(
    private val exposureDurationMs: Long = DEFAULT_EXPOSURE_DURATION_MS,
    private val peakWarningDb: Float = PEAK_WARNING_DB,
) {
    private var sessionStartTimeMs = 0L
    private var exposureAlertSent = false
    private var peakWarningSent = false

    fun reset(sessionStartTimeMs: Long) {
        this.sessionStartTimeMs = sessionStartTimeMs
        exposureAlertSent = false
        peakWarningSent = false
    }

    fun evaluate(
        reading: DecibelReading,
        stats: SessionStats,
        preferences: UserPreferences,
    ): List<NoiseAlertDecision> = buildList {
            if (shouldSendExposureAlert(reading, stats, preferences)) {
                exposureAlertSent = true
                add(
                    NoiseAlertDecision.Exposure(
                        avgDb = stats.avgDb,
                        durationMinutes = ((reading.timestamp - sessionStartTimeMs) / MINUTE_MS).toInt(),
                    ),
                )
            }
            if (shouldSendPeakWarning(reading, preferences)) {
                peakWarningSent = true
                add(NoiseAlertDecision.Peak(peakDb = reading.instantDb))
            }
        }

    private fun shouldSendExposureAlert(
        reading: DecibelReading,
        stats: SessionStats,
        preferences: UserPreferences,
    ): Boolean = preferences.exposureAlertsEnabled &&
            !exposureAlertSent &&
            reading.timestamp - sessionStartTimeMs >= exposureDurationMs &&
            stats.avgDb >= preferences.notificationThreshold

    private fun shouldSendPeakWarning(reading: DecibelReading, preferences: UserPreferences): Boolean =
        preferences.peakWarningsEnabled &&
            !peakWarningSent &&
            reading.instantDb >= peakWarningDb
}

internal object NotificationPermissionPolicy {
    fun canPostRegularNotification(
        sdkInt: Int,
        runtimePermissionGranted: Boolean,
        notificationsEnabled: Boolean,
    ): Boolean = notificationsEnabled &&
            (sdkInt < ANDROID_13_API || runtimePermissionGranted)
}

private const val ANDROID_13_API = 33
private const val DEFAULT_EXPOSURE_DURATION_MS = 30L * 60_000L
private const val MINUTE_MS = 60_000L
private const val PEAK_WARNING_DB = 120f
