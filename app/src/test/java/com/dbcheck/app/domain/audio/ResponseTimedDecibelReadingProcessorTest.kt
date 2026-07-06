package com.dbcheck.app.domain.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponseTimedDecibelReadingProcessorTest {
    @Test
    fun responseTimeSmoothsRmsValuesButLeavesPeakValuesFromCurrentChunk() {
        val processor = ResponseTimedDecibelReadingProcessor(ResponseTime.SLOW)

        val first =
            processor.process(
                reading(
                    instantDb = 60f,
                    weightedDb = 70f,
                    aWeightedDb = 75f,
                    timestamp = 1_000L,
                    peakDb = 120f,
                ),
            )
        val second =
            processor.process(
                reading(
                    instantDb = 80f,
                    weightedDb = 90f,
                    aWeightedDb = 95f,
                    timestamp = 1_100L,
                    peakAmplitude = 0.8f,
                    peakDb = 121f,
                ),
            )

        assertEquals(60f, first.instantDb, TOLERANCE)
        assertEquals(70f, first.weightedDb, TOLERANCE)
        assertEquals(75f, first.aWeightedDb, TOLERANCE)
        assertEquals(
            expectedResponseTimeStepDb(startDb = 60f, stepDb = 80f, responseTime = ResponseTime.SLOW),
            second.instantDb,
            TOLERANCE,
        )
        assertEquals(
            expectedResponseTimeStepDb(startDb = 70f, stepDb = 90f, responseTime = ResponseTime.SLOW),
            second.weightedDb,
            TOLERANCE,
        )
        assertEquals(
            expectedResponseTimeStepDb(startDb = 75f, stepDb = 95f, responseTime = ResponseTime.SLOW),
            second.aWeightedDb,
            TOLERANCE,
        )
        assertTrue(second.instantDb < 80f)
        assertTrue(second.weightedDb < 90f)
        assertTrue(second.aWeightedDb < 95f)
        assertEquals(0.8f, second.peakAmplitude, TOLERANCE)
        assertEquals(121f, second.peakDb, TOLERANCE)
    }

    @Test
    fun responseTimeChangeResetsSmoothingState() {
        val processor = ResponseTimedDecibelReadingProcessor(ResponseTime.SLOW)

        processor.process(reading(instantDb = 60f, weightedDb = 70f, aWeightedDb = 75f, timestamp = 1_000L))
        processor.setResponseTime(ResponseTime.IMPULSE)
        val resetReading =
            processor.process(reading(instantDb = 80f, weightedDb = 90f, aWeightedDb = 95f, timestamp = 1_100L))

        assertEquals(80f, resetReading.instantDb, TOLERANCE)
        assertEquals(90f, resetReading.weightedDb, TOLERANCE)
        assertEquals(95f, resetReading.aWeightedDb, TOLERANCE)
    }

    private fun reading(
        instantDb: Float,
        weightedDb: Float,
        aWeightedDb: Float,
        timestamp: Long,
        peakAmplitude: Float = 0.9f,
        peakDb: Float = 110f,
    ): DecibelReading = DecibelReading(
        instantDb = instantDb,
        weightedDb = weightedDb,
        aWeightedDb = aWeightedDb,
        timestamp = timestamp,
        peakAmplitude = peakAmplitude,
        peakDb = peakDb,
    )

    private companion object {
        const val TOLERANCE = 0.001f
    }
}
