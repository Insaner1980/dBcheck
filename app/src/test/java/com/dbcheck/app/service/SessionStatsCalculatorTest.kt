package com.dbcheck.app.service

import com.dbcheck.app.domain.audio.DecibelReading
import com.dbcheck.app.domain.noise.DecibelMath
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionStatsCalculatorTest {
    @Test
    fun withReadingUsesEnergyAverageForWeightedDb() {
        val stats =
            SessionStats()
                .withReading(reading(weightedDb = 60f))
                .withReading(reading(weightedDb = 70f))

        assertEquals(DecibelMath.energyAverageDb(listOf(60f, 70f)) ?: 0f, stats.avgDb, 0.001f)
    }

    @Test
    fun withReadingTracksWeightedMinMaxAndCWeightedPeak() {
        val stats =
            SessionStats()
                .withReading(reading(instantDb = 92f, weightedDb = 72f, peakDb = 101f))
                .withReading(reading(instantDb = 99f, weightedDb = 68f, peakDb = 97f))

        assertEquals(68f, stats.minDb, 0.001f)
        assertEquals(72f, stats.maxDb, 0.001f)
        assertEquals(101f, stats.peakDb, 0.001f)
        assertEquals(2, stats.sampleCount)
    }

    private fun reading(instantDb: Float = 70f, weightedDb: Float = instantDb, peakDb: Float = instantDb) =
        DecibelReading(
        instantDb = instantDb,
        weightedDb = weightedDb,
        timestamp = 1_000L,
        peakAmplitude = 0.5f,
        peakDb = peakDb,
    )
}
