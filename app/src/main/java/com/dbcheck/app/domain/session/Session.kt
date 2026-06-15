package com.dbcheck.app.domain.session

import com.dbcheck.app.domain.audio.ResponseTime

data class Session(
    val id: Long,
    val startTime: Long,
    val endTime: Long?,
    val minDb: Float,
    val avgDb: Float,
    val maxDb: Float,
    val peakDb: Float,
    val name: String?,
    val emoji: String?,
    val tags: List<String>,
    val isActive: Boolean,
    val frequencyWeighting: String,
    val location: SessionLocationMetadata? = null,
)

data class SessionLocationMetadata(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val capturedAt: Long,
)

data class SessionHistoryQuery(
    val nameOrTag: String? = null,
    val startTimeFrom: Long? = null,
    val startTimeTo: Long? = null,
    val minAvgDb: Float? = null,
    val maxAvgDb: Float? = null,
    val frequencyWeighting: String? = null,
    val hasLocation: Boolean? = null,
)

data class SessionMeasurement(
    val timestamp: Long,
    val dbValue: Float,
    val dbWeighted: Float,
    val peakDb: Float,
    val aWeightedDb: Float = dbWeighted,
    val responseTime: String = ResponseTime.FAST.name,
)

object SessionHistoryPolicy {
    private const val DAY_MILLIS = 24L * 60 * 60 * 1000
    const val FREE_HISTORY_WINDOW_MILLIS = 7 * DAY_MILLIS

    fun freeHistoryStartMillis(nowMillis: Long = System.currentTimeMillis()): Long =
        nowMillis - FREE_HISTORY_WINDOW_MILLIS

    fun canAccessSession(
        sessionStartTime: Long,
        isProUser: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean = isProUser || sessionStartTime >= freeHistoryStartMillis(nowMillis)
}
