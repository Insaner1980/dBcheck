package com.dbcheck.app.domain.session

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
