package com.dbcheck.app.domain.voice

import com.dbcheck.app.domain.audio.SoundClassification
import com.dbcheck.app.domain.noise.DecibelMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceBaselineCalibratorTest {
    @Test
    fun capturesEnergyAverageFromSpeechClassifiedReadingsOnly() {
        val calibrator = VoiceBaselineCalibrator(minSampleCount = 3)

        calibrator.onClassification(SoundClassification(label = "Music", confidence = 0.91f))
        calibrator.onReading(weightedDb = 96f)
        assertNull(calibrator.capture(capturedAtMs = 1_000L))

        calibrator.onClassification(SoundClassification(label = "Speech", confidence = 0.82f))
        calibrator.onReading(weightedDb = 60f)
        calibrator.onReading(weightedDb = 70f)
        assertNull(calibrator.capture(capturedAtMs = 2_000L))

        calibrator.onReading(weightedDb = 80f)

        val capture = calibrator.capture(capturedAtMs = 3_000L)

        val expectedAverage = DecibelMath.energyAverageDb(listOf(60f, 70f, 80f)) ?: error("missing average")
        assertEquals(expectedAverage, capture?.levelDb ?: 0f, 0.001f)
        assertEquals(3, capture?.sampleCount)
        assertEquals(3_000L, capture?.capturedAtMs)
    }

    @Test
    fun resetClearsAccumulatedSpeechReadings() {
        val calibrator = VoiceBaselineCalibrator(minSampleCount = 1)

        calibrator.onClassification(SoundClassification(label = "Speech", confidence = 0.82f))
        calibrator.onReading(weightedDb = 70f)
        calibrator.reset()

        assertNull(calibrator.capture(capturedAtMs = 1_000L))
    }
}
