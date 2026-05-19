package com.dbcheck.app.service

import android.content.Intent
import com.dbcheck.app.sync.HealthConnectAvailability
import com.dbcheck.app.sync.HealthConnectManager
import com.dbcheck.app.sync.HealthConnectPermissions
import com.dbcheck.app.sync.HealthConnectStatus
import com.dbcheck.app.sync.HeartRateSample
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

enum class HealthConnectServiceAvailability {
    AVAILABLE,
    UNAVAILABLE,
    UPDATE_REQUIRED,
}

data class HealthConnectServiceStatus(
    val availability: HealthConnectServiceAvailability = HealthConnectServiceAvailability.UNAVAILABLE,
    val noiseSyncGranted: Boolean = false,
    val heartRateReadGranted: Boolean = false,
    val noiseSyncPermissions: Set<String> = emptySet(),
    val heartRateReadPermissions: Set<String> = emptySet(),
)

data class HeartRateServiceSample(val time: Instant, val beatsPerMinute: Long)

@Singleton
class HealthConnectService
    @Inject
    constructor(private val healthConnectManager: HealthConnectManager) {
        suspend fun getStatus(): HealthConnectServiceStatus = healthConnectManager.getStatus().toServiceStatus()

        suspend fun readHeartRateForSession(start: Instant, end: Instant): List<HeartRateServiceSample> =
            healthConnectManager
                .readHeartRateForSession(start = start, end = end)
                .map { it.toServiceSample() }

        fun createInstallIntent(): Intent = healthConnectManager.createInstallIntent()

        fun createManageDataIntent(): Intent = healthConnectManager.createManageDataIntent()
    }

private fun HealthConnectStatus.toServiceStatus(): HealthConnectServiceStatus = HealthConnectServiceStatus(
        availability =
            when (availability) {
                HealthConnectAvailability.AVAILABLE -> HealthConnectServiceAvailability.AVAILABLE
                HealthConnectAvailability.UNAVAILABLE -> HealthConnectServiceAvailability.UNAVAILABLE
                HealthConnectAvailability.UPDATE_REQUIRED -> HealthConnectServiceAvailability.UPDATE_REQUIRED
            },
        noiseSyncGranted = noiseSyncGranted,
        heartRateReadGranted = heartRateReadGranted,
        noiseSyncPermissions = HealthConnectPermissions.NOISE_SYNC,
        heartRateReadPermissions = HealthConnectPermissions.HEART_RATE_READ,
    )

private fun HeartRateSample.toServiceSample(): HeartRateServiceSample = HeartRateServiceSample(
        time = time,
        beatsPerMinute = beatsPerMinute,
    )
