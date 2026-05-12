package com.dbcheck.app.domain.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class SpectralAnalyzerTest {
    private val analyzer = SpectralAnalyzer(FFTProcessor())

    @Test
    fun oneKhzSineWaveProducesDominantFrequencyNearOneKhz() {
        val frame = analyzer.analyze(sineWave(frequencyHz = 1000.0), AudioProcessingConfig.CHUNK_SIZE)

        assertEquals(1000f, frame.dominantFrequencyHz, 25f)
        assertEquals(SpectralBandwidth.NARROW, frame.bandwidth)
    }

    @Test
    fun silentBufferProducesIdleSpectrumWithoutCrashing() {
        val frame = analyzer.analyze(ShortArray(AudioProcessingConfig.CHUNK_SIZE), AudioProcessingConfig.CHUNK_SIZE)

        assertEquals(0f, frame.dominantFrequencyHz, 0f)
        assertEquals(SpectralBandwidth.UNKNOWN, frame.bandwidth)
        assertEquals(SpectralAnalyzer.BAND_COUNT, frame.bands.size)
        assertTrue(frame.bands.all { it.normalizedAmplitude == 0f })
    }

    @Test
    fun normalizedBandsStayWithinUnitRange() {
        val frame = analyzer.analyze(sineWave(frequencyHz = 2500.0), AudioProcessingConfig.CHUNK_SIZE)

        assertEquals(SpectralAnalyzer.BAND_COUNT, frame.bands.size)
        assertTrue(frame.bands.all { it.normalizedAmplitude in 0f..1f })
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
