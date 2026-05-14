package com.dbcheck.app.domain.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class FFTProcessorTest {
    private val processor = FFTProcessor()

    @Test
    fun processRoundsNonPowerOfTwoInputDownToNearestPowerOfTwo() {
        val magnitudes = processor.process(ShortArray(3000) { 1 }, 3000)

        assertEquals(1024, magnitudes.size)
    }

    @Test
    fun processCapsInputToAvailableBufferLengthWhenReportedSizeIsLarger() {
        val magnitudes = processor.process(ShortArray(3000) { 1 }, AudioProcessingConfig.CHUNK_SIZE)

        assertEquals(1024, magnitudes.size)
    }

    @Test
    fun processReturnsEmptySpectrumForDegenerateTwoSampleWindow() {
        val magnitudes = processor.process(shortArrayOf(1000, 1000), 2)

        assertEquals(0, magnitudes.size)
    }

    @Test
    fun dominantFrequencySkipsDcBin() {
        val magnitudes = FloatArray(8)
        magnitudes[0] = 10_000f
        magnitudes[2] = 100f

        val dominantFrequency = processor.findDominantFrequency(magnitudes, sampleRate = 16)

        assertEquals(2f, dominantFrequency, 0f)
    }

    @Test
    fun dominantFrequencyReturnsZeroWhenOnlyDcBinHasEnergy() {
        val magnitudes = FloatArray(8)
        magnitudes[0] = 10_000f

        val dominantFrequency = processor.findDominantFrequency(magnitudes, sampleRate = 16)

        assertEquals(0f, dominantFrequency, 0f)
    }
}
