package com.dbcheck.app.service

import com.dbcheck.app.domain.audio.DecibelReading
import com.dbcheck.app.domain.noise.NoiseLevel

internal const val MEASUREMENT_PERSISTENCE_INTERVAL_MS = 1_000L

class MeasurementPersistenceSampler {
    private var lastPersistedReading: DecibelReading? = null
    private var latestReading: DecibelReading? = null

    fun shouldPersist(reading: DecibelReading, currentMaxDbBeforeReading: Float): Boolean {
        rememberLatest(reading)
        val persisted = lastPersistedReading ?: return true
        return reading.timestamp - persisted.timestamp >= MEASUREMENT_PERSISTENCE_INTERVAL_MS ||
            crossesPeakThreshold(persisted, reading) ||
            reading.weightedDb > currentMaxDbBeforeReading
    }

    fun rememberLatest(reading: DecibelReading) {
        latestReading = reading
    }

    fun markPersisted(reading: DecibelReading) {
        lastPersistedReading = reading
        latestReading = reading
    }

    fun latestUnpersistedOnStop(): DecibelReading? =
        latestReading?.takeIf { latest -> latest !== lastPersistedReading }

    fun reset() {
        lastPersistedReading = null
        latestReading = null
    }

    private fun crossesPeakThreshold(
        previous: DecibelReading,
        current: DecibelReading,
    ): Boolean =
        (previous.weightedDb < NoiseLevel.ELEVATED.maxDb && current.weightedDb >= NoiseLevel.ELEVATED.maxDb) ||
            (previous.weightedDb >= NoiseLevel.ELEVATED.maxDb && current.weightedDb < NoiseLevel.ELEVATED.maxDb)
}
