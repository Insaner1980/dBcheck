package com.dbcheck.app.domain.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class OctaveBandRtaCalculatorTest {
    private val calculator = OctaveBandRtaCalculator(FFTProcessor())

    @Test
    fun thirdOctaveBandsUseStandardCenterAndBandEdgeFrequencies() {
        val frame =
            calculator.calculateFromMagnitudes(
                magnitudes = FloatArray(0),
                sampleRate = 48_000,
                resolution = RtaResolution.THIRD_OCTAVE,
                timestamp = 7L,
            )

        assertEquals(RtaResolution.THIRD_OCTAVE, frame.resolution)
        assertEquals(7L, frame.timestamp)
        assertEquals(31, frame.bands.size)

        val oneKilohertzBand = frame.bands.first { abs(it.centerFrequencyHz - 1_000f) < 0.01f }
        assertEquals(891.25f, oneKilohertzBand.lowerEdgeFrequencyHz, 0.01f)
        assertEquals(1_000f, oneKilohertzBand.centerFrequencyHz, 0.01f)
        assertEquals(1_122.02f, oneKilohertzBand.upperEdgeFrequencyHz, 0.01f)
    }

    @Test
    fun octaveBandsUseStandardCenterFrequenciesAcrossAudioRange() {
        val frame =
            calculator.calculateFromMagnitudes(
                magnitudes = FloatArray(0),
                sampleRate = 48_000,
                resolution = RtaResolution.OCTAVE,
                timestamp = 0L,
            )

        assertEquals(10, frame.bands.size)
        assertEquals(31.62f, frame.bands.first().centerFrequencyHz, 0.01f)
        assertEquals(1_000f, frame.bands.first { abs(it.centerFrequencyHz - 1_000f) < 0.01f }.centerFrequencyHz, 0.01f)
        assertEquals(15_848.93f, frame.bands.last().centerFrequencyHz, 0.01f)
    }

    @Test
    fun normalizedAmplitudeUsesTheStrongestRtaBandAsReference() {
        val magnitudes = FloatArray(24)
        magnitudes[1] = 5f
        magnitudes[2] = 10f

        val frame =
            calculator.calculateFromMagnitudes(
                magnitudes = magnitudes,
                sampleRate = 48_000,
                resolution = RtaResolution.THIRD_OCTAVE,
                timestamp = 0L,
            )

        val oneKilohertzBand = frame.bands.first { abs(it.centerFrequencyHz - 1_000f) < 0.01f }
        val twoKilohertzBand = frame.bands.first { abs(it.centerFrequencyHz - 1_995.26f) < 0.01f }

        assertEquals(0.5f, oneKilohertzBand.normalizedAmplitude, 0.001f)
        assertEquals(1f, twoKilohertzBand.normalizedAmplitude, 0.001f)
        assertTrue(
            frame.bands
                .filterNot { it == oneKilohertzBand || it == twoKilohertzBand }
                .all { it.normalizedAmplitude == 0f },
        )
    }

    @Test
    fun analyzeBuildsRtaDataFromTheCurrentFftProcessor() {
        val frame =
            calculator.analyze(
                buffer = sineWave(frequencyHz = 1_000.0),
                size = AudioProcessingConfig.CHUNK_SIZE,
                resolution = RtaResolution.THIRD_OCTAVE,
                timestamp = 11L,
            )

        val strongestBand = frame.bands.maxBy { it.normalizedAmplitude }
        assertEquals(11L, frame.timestamp)
        assertEquals(1_000f, strongestBand.centerFrequencyHz, 0.01f)
        assertEquals(1f, strongestBand.normalizedAmplitude, 0f)
    }

    private fun sineWave(frequencyHz: Double): ShortArray =
        ShortArray(AudioProcessingConfig.CHUNK_SIZE) { index ->
            (sin(2.0 * PI * frequencyHz * index / AudioProcessingConfig.SAMPLE_RATE) * AMPLITUDE)
                .toInt()
                .toShort()
        }

    private companion object {
        const val AMPLITUDE = 12_000
    }
}
