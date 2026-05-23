package com.dbcheck.app.domain.hearingtest

import org.junit.Assert.assertEquals
import org.junit.Test

class HearingTestResultCalculatorTest {
    @Test
    fun buildsScoreRatingAndSerializedEarDataFromThresholds() {
        val result =
            HearingTestResultCalculator.build(
                thresholds =
                    mapOf(
                        TestKey(Ear.LEFT, 250f) to -45f,
                        TestKey(Ear.LEFT, 500f) to -50f,
                        TestKey(Ear.RIGHT, 250f) to -35f,
                        TestKey(Ear.RIGHT, 500f) to -40f,
                    ),
                timestamp = 1_700_000_000_000L,
            )

        assertEquals(70, result.overallScore)
        assertEquals("Fair", result.rating)
        assertEquals(-42.5f, result.avgThreshold, 0.001f)
        assertEquals(68.6f, result.speechClarity, 0.001f)
        assertEquals(listOf(250f to -45f, 500f to -50f), result.leftEarThresholds)
        assertEquals(listOf(250f to -35f, 500f to -40f), result.rightEarThresholds)
    }

    @Test
    fun highFrequencyLimitDoesNotExceedTestedFrequencyRange() {
        val result =
            HearingTestResultCalculator.build(
                thresholds =
                    mapOf(
                        TestKey(Ear.LEFT, 250f) to -45f,
                        TestKey(Ear.LEFT, 8_000f) to -35f,
                    ),
                timestamp = 1_700_000_000_000L,
            )

        assertEquals(TEST_FREQUENCIES.maxOrNull() ?: 0f, result.highFreqLimit, 0.001f)
    }

    @Test
    fun thresholdCodecRoundTripsStableFrequencyThresholdPairs() {
        val thresholds =
            mapOf(
                TestKey(Ear.LEFT, 500f) to -50f,
                TestKey(Ear.LEFT, 250f) to -45f,
            )

        val encoded = HearingTestThresholdCodec.serializeEarData(thresholds, Ear.LEFT)
        val decoded = HearingTestThresholdCodec.parseEarData(encoded)

        assertEquals("250.0:-45.0,500.0:-50.0", encoded)
        assertEquals(listOf(250f to -45f, 500f to -50f), decoded)
    }

    @Test
    fun thresholdCodecSkipsMalformedEntriesInsteadOfThrowing() {
        val decoded =
            HearingTestThresholdCodec.parseEarData(
                "250.0:-45.0,missing-colon,500.0:not-a-number,:,-,1000.0:-50.0,2000.0:",
            )

        assertEquals(listOf(250f to -45f, 1000f to -50f), decoded)
    }
}
