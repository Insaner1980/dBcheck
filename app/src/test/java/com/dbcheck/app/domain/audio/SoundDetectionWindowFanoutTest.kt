package com.dbcheck.app.domain.audio

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.ceil

@OptIn(ExperimentalCoroutinesApi::class)
class SoundDetectionWindowFanoutTest {
    @Test
    fun disabledFanoutDoesNotEmitClassifierWindows() = runTest {
        val fanout = SoundDetectionWindowFanout()
        val windows = mutableListOf<FloatArray>()
        val collectJob = launch { fanout.windows.collect { windows += it } }
        runCurrent()

        fanout.processPcm16(ShortArray(requiredSourceSamples()), requiredSourceSamples())
        runCurrent()

        assertTrue(windows.isEmpty())
        collectJob.cancel()
    }

    @Test
    fun enabledFanoutEmitsFixedLengthClassifierWindow() = runTest {
        val fanout = SoundDetectionWindowFanout()
        val windows = mutableListOf<FloatArray>()
        val collectJob = launch { fanout.windows.collect { windows += it } }
        runCurrent()

        fanout.setEnabled(true)
        fanout.processPcm16(ShortArray(requiredSourceSamples()), requiredSourceSamples())
        runCurrent()

        assertEquals(1, windows.size)
        assertEquals(YamnetAudioConfig.WINDOW_SIZE_SAMPLES, windows.single().size)
        collectJob.cancel()
    }

    @Test
    fun disablingFanoutClearsPartialWindow() = runTest {
        val fanout = SoundDetectionWindowFanout()
        val windows = mutableListOf<FloatArray>()
        val collectJob = launch { fanout.windows.collect { windows += it } }
        runCurrent()
        val halfWindow = requiredSourceSamples() / 2

        fanout.setEnabled(true)
        fanout.processPcm16(ShortArray(halfWindow), halfWindow)
        fanout.setEnabled(false)
        fanout.setEnabled(true)
        fanout.processPcm16(ShortArray(halfWindow), halfWindow)
        runCurrent()

        assertTrue(windows.isEmpty())
        collectJob.cancel()
    }

    private fun requiredSourceSamples(): Int = ceil(
            YamnetAudioConfig.WINDOW_SIZE_SAMPLES.toDouble() *
                AudioProcessingConfig.SAMPLE_RATE /
                YamnetAudioConfig.SAMPLE_RATE_HZ,
        ).toInt()
}
