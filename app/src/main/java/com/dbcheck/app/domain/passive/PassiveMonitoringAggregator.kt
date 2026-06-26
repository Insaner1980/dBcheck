package com.dbcheck.app.domain.passive

import com.dbcheck.app.domain.audio.DecibelReading
import com.dbcheck.app.domain.noise.DecibelMath

class PassiveMonitoringAggregator(private val startedAtMs: Long) {
    private var readingCount = 0
    private var minDb = Float.MAX_VALUE
    private var maxDb = 0f
    private var peakDb = 0f
    private var totalEnergy = 0.0

    fun add(reading: DecibelReading) {
        if (reading.timestamp < startedAtMs) return

        readingCount += 1
        minDb = minOf(minDb, reading.weightedDb)
        maxDb = maxOf(maxDb, reading.weightedDb)
        peakDb = maxOf(peakDb, reading.peakDb)
        totalEnergy += DecibelMath.energyFromDb(reading.weightedDb)
    }

    fun toSample(endedAtMs: Long): PassiveMonitoringSample? {
        if (readingCount == 0) return null

        return PassiveMonitoringSample(
            startedAtMs = startedAtMs,
            endedAtMs = endedAtMs.coerceAtLeast(startedAtMs),
            readingCount = readingCount,
            minDb = minDb.takeIf { it != Float.MAX_VALUE } ?: 0f,
            averageDb = DecibelMath.energyAverageDb(totalEnergy, readingCount) ?: 0f,
            maxDb = maxDb,
            peakDb = peakDb,
            totalEnergy = totalEnergy,
        )
    }
}
