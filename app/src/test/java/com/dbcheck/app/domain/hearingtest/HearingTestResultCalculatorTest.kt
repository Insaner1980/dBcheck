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
        assertEquals(500f, result.highFreqLimit, 0.001f)
        assertEquals(listOf(250f to -45f, 500f to -50f), result.leftEarThresholds)
        assertEquals(listOf(250f to -35f, 500f to -40f), result.rightEarThresholds)
    }

    @Test
    fun highFrequencyLimitUsesHighestDetectedTestFrequency() {
        val result =
            HearingTestResultCalculator.build(
                thresholds =
                    mapOf(
                        TestKey(Ear.LEFT, 250f) to -45f,
                        TestKey(Ear.LEFT, 500f) to -35f,
                        TestKey(Ear.LEFT, 1000f) to -20f,
                        TestKey(Ear.LEFT, 2000f) to -10f,
                        TestKey(Ear.LEFT, 4000f) to 0f,
                        TestKey(Ear.LEFT, 8000f) to 0f,
                        TestKey(Ear.RIGHT, 250f) to -45f,
                        TestKey(Ear.RIGHT, 500f) to -35f,
                        TestKey(Ear.RIGHT, 1000f) to -20f,
                        TestKey(Ear.RIGHT, 2000f) to -10f,
                        TestKey(Ear.RIGHT, 4000f) to -5f,
                        TestKey(Ear.RIGHT, 8000f) to 0f,
                    ),
                timestamp = 1_700_000_000_000L,
            )

        assertEquals(4000f, result.highFreqLimit, 0.001f)
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
}
