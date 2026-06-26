package com.dbcheck.app.service

import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.domain.audio.DecibelReading
import com.dbcheck.app.domain.noise.NoiseAlertPolicy
import java.time.Instant
import java.time.ZoneId

internal sealed interface NoiseAlertDecision {
    data class Exposure(
        val avgDb: Float,
        val durationMinutes: Int,
        val trigger: NoiseExposureAlertTrigger = NoiseExposureAlertTrigger.AVERAGE_DURATION,
    ) : NoiseAlertDecision

    data class Peak(val peakDb: Float) : NoiseAlertDecision
}

internal enum class NoiseExposureAlertTrigger {
    AVERAGE_DURATION,
    DOSE,
    PROJECTED_DOSE,
}

internal class NoiseAlertEvaluator(
    private val exposureDurationMs: Long = NoiseAlertPolicy.EXPOSURE_DURATION_MS,
    private val peakWarningDb: Float = NoiseAlertPolicy.PEAK_WARNING_DB,
    private val exposureDoseAlertPercent: Float = NoiseAlertPolicy.EXPOSURE_DOSE_ALERT_PERCENT,
    private val projectedDoseAlertPercent: Float = NoiseAlertPolicy.PROJECTED_DOSE_ALERT_PERCENT,
    private val alertRetryCooldownMs: Long = NoiseAlertPolicy.ALERT_RETRY_COOLDOWN_MS,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    private var sessionStartTimeMs = 0L
    private var exposureAlertSent = false
    private var peakWarningSent = false
    private var lastExposureAttemptTimeMs: Long? = null
    private var lastPeakAttemptTimeMs: Long? = null

    fun reset(sessionStartTimeMs: Long) {
        this.sessionStartTimeMs = sessionStartTimeMs
        exposureAlertSent = false
        peakWarningSent = false
        lastExposureAttemptTimeMs = null
        lastPeakAttemptTimeMs = null
    }

    fun evaluate(
        reading: DecibelReading,
        stats: SessionStats,
        preferences: UserPreferences,
        liveExposure: LiveExposureState = LiveExposureState(),
    ): List<NoiseAlertDecision> {
        if (!preferences.notificationSchedule.isActiveAt(Instant.ofEpochMilli(reading.timestamp).atZone(zoneId))) {
            return emptyList()
        }
        return buildList {
            exposureAlertTrigger(reading, stats, liveExposure, preferences)?.let { trigger ->
                lastExposureAttemptTimeMs = reading.timestamp
                add(
                    NoiseAlertDecision.Exposure(
                        avgDb = stats.avgDb,
                        durationMinutes = ((reading.timestamp - sessionStartTimeMs) / MINUTE_MS).toInt(),
                        trigger = trigger,
                    ),
                )
            }
            if (shouldSendPeakWarning(reading, preferences)) {
                lastPeakAttemptTimeMs = reading.timestamp
                add(NoiseAlertDecision.Peak(peakDb = reading.peakDb))
            }
        }
    }

    fun markDelivered(decision: NoiseAlertDecision) {
        when (decision) {
            is NoiseAlertDecision.Exposure -> exposureAlertSent = true
            is NoiseAlertDecision.Peak -> peakWarningSent = true
        }
    }

    private fun exposureAlertTrigger(
        reading: DecibelReading,
        stats: SessionStats,
        liveExposure: LiveExposureState,
        preferences: UserPreferences,
    ): NoiseExposureAlertTrigger? {
        if (
            !preferences.exposureAlertsEnabled ||
            exposureAlertSent ||
            !isOutsideRetryCooldown(lastExposureAttemptTimeMs, reading.timestamp)
        ) {
            return null
        }

        return when {
            liveExposure.sampleCount > 0 && liveExposure.dosePercent >= exposureDoseAlertPercent ->
                NoiseExposureAlertTrigger.DOSE

            liveExposure.sampleCount > 0 && liveExposure.projectedDosePercent >= projectedDoseAlertPercent ->
                NoiseExposureAlertTrigger.PROJECTED_DOSE

            reading.timestamp - sessionStartTimeMs >= exposureDurationMs &&
                stats.avgDb >= preferences.notificationThreshold ->
                NoiseExposureAlertTrigger.AVERAGE_DURATION

            else -> null
        }
    }

    private fun shouldSendPeakWarning(reading: DecibelReading, preferences: UserPreferences): Boolean =
        preferences.peakWarningsEnabled &&
            !peakWarningSent &&
            isOutsideRetryCooldown(lastPeakAttemptTimeMs, reading.timestamp) &&
            reading.peakDb >= peakWarningDb

    private fun isOutsideRetryCooldown(lastAttemptTimeMs: Long?, currentTimeMs: Long): Boolean =
        lastAttemptTimeMs == null || currentTimeMs - lastAttemptTimeMs >= alertRetryCooldownMs
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
private const val MINUTE_MS = 60_000L
