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
    fun dominantFrequencySkipsDcBin() {
        val magnitudes = FloatArray(8)
        magnitudes[0] = 10_000f
        magnitudes[2] = 100f

        val dominantFrequency = processor.findDominantFrequency(magnitudes, sampleRate = 16)

        assertEquals(2f, dominantFrequency, 0f)
    }
}
