package com.dbcheck.app.domain.hearingtest

import com.dbcheck.app.testRecoveryBaselineResult
import org.junit.Assert.assertEquals
import org.junit.Test

class HearingRecoveryCalculatorTest {
    @Test
    fun recoveryCheckUsesShortFrequencySet() {
        assertEquals(listOf(1000f, 4000f, 8000f), HearingTestPolicy.RECOVERY_CHECK_FREQUENCIES)
    }

    @Test
    fun comparesShortCheckThresholdsAgainstBaselineFrequencies() {
        val result =
            HearingRecoveryCalculator.build(
                baseline = baselineResult(),
                thresholds =
                    mapOf(
                        TestKey(Ear.LEFT, 1000f) to -25f,
                        TestKey(Ear.LEFT, 4000f) to -30f,
                        TestKey(Ear.LEFT, 8000f) to -10f,
                        TestKey(Ear.RIGHT, 1000f) to -35f,
                        TestKey(Ear.RIGHT, 4000f) to -15f,
                        TestKey(Ear.RIGHT, 8000f) to -20f,
                    ),
                timestamp = 2_000L,
            )

        assertEquals(1L, result.baselineTestId)
        assertEquals(2_000L, result.timestamp)
        assertEquals(6, result.testedFrequencyCount)
        assertEquals(7.5f, result.averageShiftDb)
        assertEquals(20f, result.maxShiftDb)
        assertEquals(HearingRecoveryStatus.ELEVATED_SHIFT, result.status)
        assertEquals(
            listOf(1000f to 5f, 4000f to 0f, 8000f to 20f),
            result.leftEarShifts,
        )
        assertEquals(
            listOf(1000f to -5f, 4000f to 15f, 8000f to 10f),
            result.rightEarShifts,
        )
    }

    @Test
    fun ignoresFrequenciesThatAreNotInBaseline() {
        val result =
            HearingRecoveryCalculator.build(
                baseline = baselineResult().copy(rightEarThresholds = emptyList()),
                thresholds =
                    mapOf(
                        TestKey(Ear.LEFT, 1000f) to -25f,
                        TestKey(Ear.RIGHT, 1000f) to -10f,
                    ),
                timestamp = 2_000L,
            )

        assertEquals(1, result.testedFrequencyCount)
        assertEquals(5f, result.averageShiftDb)
        assertEquals(5f, result.maxShiftDb)
        assertEquals(HearingRecoveryStatus.STABLE, result.status)
        assertEquals(emptyList<Pair<Float, Float>>(), result.rightEarShifts)
    }

    private fun baselineResult() = testRecoveryBaselineResult()
}
