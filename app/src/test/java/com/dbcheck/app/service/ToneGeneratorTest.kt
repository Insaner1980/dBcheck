package com.dbcheck.app.service

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ToneGeneratorTest {
    private val track = mockk<AudioTrack>(relaxed = true)

    @Before
    fun setUp() {
        mockkConstructor(AudioAttributes.Builder::class, AudioFormat.Builder::class, AudioTrack.Builder::class)
        stubAudioTrackBuilders(track)
        every { track.write(any<ShortArray>(), any(), any()) } answers { thirdArg<Int>() }
    }

    @After
    fun tearDown() {
        unmockkConstructor(AudioAttributes.Builder::class, AudioFormat.Builder::class, AudioTrack.Builder::class)
    }

    @Test
    fun playFailureReleasesTrackBeforeRethrowing() {
        every { track.play() } throws IllegalStateException("play failed")
        val generator = ToneGenerator()

        assertThrows(IllegalStateException::class.java) { generator.playTone(1_000f, -30f) }
        generator.stop()

        verify(exactly = 1) { track.release() }
    }

    @Test
    fun incompleteWriteReleasesTrackWithoutPlaying() {
        every { track.write(any<ShortArray>(), any(), any()) } returns AudioTrack.ERROR_DEAD_OBJECT

        assertThrows(IllegalStateException::class.java) { ToneGenerator().playTone(1_000f, -30f) }

        verify(exactly = 1) { track.release() }
        verify(exactly = 0) { track.play() }
    }

    @Test
    fun stopFailureStillReleasesTrackAndClearsOwnership() {
        every { track.playState } returns AudioTrack.PLAYSTATE_PLAYING
        every { track.stop() } throws IllegalStateException("stop failed")
        val generator = ToneGenerator()
        generator.playTone(1_000f, -30f)

        assertThrows(IllegalStateException::class.java) { generator.stop() }
        generator.stop()

        verify(exactly = 1) { track.release() }
    }

    @Test
    fun pcmBufferUsesBoundedRelativeAmplitudeAndFadeSilently() {
        var captured = shortArrayOf()
        every { track.write(any<ShortArray>(), 0, any()) } answers {
            captured = firstArg()
            captured.size
        }
        every { track.play() } just Runs
        val generator = ToneGenerator()

        generator.playTone(1_000f, -30f)
        generator.stop()

        assertEquals(66_150, captured.size)
        assertEquals(0, captured.first().toInt())
        assertTrue(captured.all { kotlin.math.abs(it.toInt()) <= 1_036 })
        assertTrue(captured.sliceArray(3_000..4_000).any { kotlin.math.abs(it.toInt()) > 1_000 })
        assertTrue(kotlin.math.abs(captured.last().toInt()) <= 1)
        verify { anyConstructed<AudioFormat.Builder>().setChannelMask(AudioFormat.CHANNEL_OUT_MONO) }
    }
}
