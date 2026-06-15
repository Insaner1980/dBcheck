package com.dbcheck.app.domain.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.ceil

class YamnetAudioWindowAdapterTest {
    @Test
    fun appendPcm16ReturnsNullUntilFullWindowIsBuffered() {
        val adapter = YamnetAudioWindowAdapter()

        val window = adapter.appendPcm16(ShortArray(AudioProcessingConfig.CHUNK_SIZE), AudioProcessingConfig.CHUNK_SIZE)

        assertNull(window)
    }

    @Test
    fun appendPcm16ReturnsFixedLengthWindowAfterEnoughSourceSamples() {
        val adapter = YamnetAudioWindowAdapter()
        val requiredSourceSamples =
            ceil(
                YamnetAudioConfig.WINDOW_SIZE_SAMPLES.toDouble() *
                    AudioProcessingConfig.SAMPLE_RATE /
                    YamnetAudioConfig.SAMPLE_RATE_HZ,
            ).toInt()

        val window = adapter.appendPcm16(ShortArray(requiredSourceSamples), requiredSourceSamples)

        assertNotNull(window)
        assertEquals(YamnetAudioConfig.WINDOW_SIZE_SAMPLES, window!!.size)
    }

    @Test
    fun appendPcm16PreservesTargetSampleCadenceAcrossAudioChunks() {
        val adapter = YamnetAudioWindowAdapter(windowSizeSamples = YamnetAudioConfig.SAMPLE_RATE_HZ)
        val oneSecondSource = ShortArray(AudioProcessingConfig.SAMPLE_RATE)
        var offset = 0
        var window: FloatArray? = null

        while (offset < oneSecondSource.size) {
            val chunkSize = minOf(AudioProcessingConfig.CHUNK_SIZE, oneSecondSource.size - offset)
            val chunk = oneSecondSource.copyOfRange(offset, offset + chunkSize)
            window = adapter.appendPcm16(chunk, chunk.size)
            offset += chunkSize
        }

        assertNotNull(window)
        assertEquals(YamnetAudioConfig.SAMPLE_RATE_HZ, window!!.size)
    }

    @Test
    fun appendPcm16NormalizesPcm16ToUnitFloatRange() {
        val adapter = YamnetAudioWindowAdapter(sourceSampleRateHz = YamnetAudioConfig.SAMPLE_RATE_HZ)
        val pcmWindow =
            ShortArray(YamnetAudioConfig.WINDOW_SIZE_SAMPLES) { 0 }.also { samples ->
                samples[0] = Short.MIN_VALUE
                samples[1] = 0
                samples[2] = Short.MAX_VALUE
            }

        val window = adapter.appendPcm16(pcmWindow, pcmWindow.size)

        assertNotNull(window)
        assertEquals(-1f, window!![0], 0f)
        assertEquals(0f, window[1], 0f)
        assertEquals(1f, window[2], 0f)
        assertTrue(window.all { sample -> sample in -1f..1f })
    }
}
