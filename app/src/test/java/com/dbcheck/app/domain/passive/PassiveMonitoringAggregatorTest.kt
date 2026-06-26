package com.dbcheck.app.domain.passive

import com.dbcheck.app.domain.audio.DecibelReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PassiveMonitoringAggregatorTest {
    @Test
    fun emptyAggregateDoesNotCreateSample() {
        val aggregator = PassiveMonitoringAggregator(startedAtMs = START_TIME)

        assertNull(aggregator.toSample(endedAtMs = START_TIME + 1_000L))
    }

    @Test
    fun aggregateSampleStoresOnlyNoiseSummaryFields() {
        val aggregator = PassiveMonitoringAggregator(startedAtMs = START_TIME)

        aggregator.add(reading(timestamp = START_TIME + 100L, weightedDb = 70f, peakDb = 74f))
        aggregator.add(reading(timestamp = START_TIME + 200L, weightedDb = 80f, peakDb = 91f))

        val sample = requireNotNull(aggregator.toSample(endedAtMs = START_TIME + 1_000L))

        assertEquals(START_TIME, sample.startedAtMs)
        assertEquals(START_TIME + 1_000L, sample.endedAtMs)
        assertEquals(2, sample.readingCount)
        assertEquals(70f, sample.minDb, FLOAT_TOLERANCE)
        assertEquals(80f, sample.maxDb, FLOAT_TOLERANCE)
        assertEquals(91f, sample.peakDb, FLOAT_TOLERANCE)
        assertEquals(77.4f, sample.averageDb, 0.1f)
    }

    private fun reading(timestamp: Long, weightedDb: Float, peakDb: Float): DecibelReading = DecibelReading(
            instantDb = weightedDb,
            weightedDb = weightedDb,
            aWeightedDb = weightedDb,
            timestamp = timestamp,
            peakAmplitude = 0.5f,
            peakDb = peakDb,
        )

    private companion object {
        const val START_TIME = 1_700_000_000_000L
        const val FLOAT_TOLERANCE = 0.001f
    }
}
