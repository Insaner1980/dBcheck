package com.dbcheck.app.domain.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponseTimeSmootherTest {
    @Test
    fun stepResponseUsesElapsedTimeAndResponseTimeInEnergyDomain() {
        val slowStep = smoothStep(ResponseTime.SLOW)
        val fastStep = smoothStep(ResponseTime.FAST)
        val impulseStep = smoothStep(ResponseTime.IMPULSE)

        assertEquals(expectedStepDb(ResponseTime.SLOW), slowStep, TOLERANCE)
        assertEquals(expectedStepDb(ResponseTime.FAST), fastStep, TOLERANCE)
        assertEquals(expectedStepDb(ResponseTime.IMPULSE), impulseStep, TOLERANCE)
        assertTrue(slowStep < fastStep)
        assertTrue(fastStep < impulseStep)
    }

    @Test
    fun emptyInputReturnsEmptyOutput() {
        val smoother = ResponseTimeSmoother(ResponseTime.FAST)

        assertEquals(emptyList<ResponseTimeSample>(), smoother.smooth(emptyList()))
    }

    @Test
    fun outOfOrderTimestampIsRejected() {
        val smoother = ResponseTimeSmoother(ResponseTime.FAST)

        assertThrows(IllegalArgumentException::class.java) {
            smoother.smooth(
                listOf(
                    ResponseTimeSample(db = START_DB, timestampMs = 1_000L),
                    ResponseTimeSample(db = STEP_DB, timestampMs = 999L),
                ),
            )
        }
    }

    private fun smoothStep(responseTime: ResponseTime): Float {
        val smoother = ResponseTimeSmoother(responseTime)

        return smoother.smooth(
            listOf(
                ResponseTimeSample(db = START_DB, timestampMs = 1_000L),
                ResponseTimeSample(db = STEP_DB, timestampMs = 1_100L),
            ),
        ).last().db
    }

    private fun expectedStepDb(responseTime: ResponseTime): Float = expectedResponseTimeStepDb(
        startDb = START_DB,
        stepDb = STEP_DB,
        responseTime = responseTime,
    )

    private companion object {
        const val START_DB = 60f
        const val STEP_DB = 80f
        const val TOLERANCE = 0.001f
    }
}
