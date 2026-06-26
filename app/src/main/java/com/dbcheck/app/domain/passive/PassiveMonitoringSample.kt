package com.dbcheck.app.domain.passive

import com.dbcheck.app.domain.noise.DecibelMath

data class PassiveMonitoringSample(
    val id: Long = 0L,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val readingCount: Int,
    val minDb: Float,
    val averageDb: Float,
    val maxDb: Float,
    val peakDb: Float,
    val totalEnergy: Double = DecibelMath.energyFromDb(averageDb) * readingCount,
)

data class PassiveMonitoringDailySummary(
    val sampleCount: Int = 0,
    val readingCount: Int = 0,
    val minDb: Float? = null,
    val averageDb: Float? = null,
    val maxDb: Float? = null,
    val peakDb: Float? = null,
) {
    val hasSamples: Boolean
        get() = sampleCount > 0 && readingCount > 0
}
