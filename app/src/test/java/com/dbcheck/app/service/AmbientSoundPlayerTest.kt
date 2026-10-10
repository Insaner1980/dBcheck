package com.dbcheck.app.service

import android.media.AudioTrack
import android.media.AudioAttributes
import android.media.AudioFormat
import com.dbcheck.app.domain.ambient.AmbientSoundGenerator
import com.dbcheck.app.domain.ambient.AmbientSoundPreset
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class AmbientSoundPlayerTest {
    @Test
    fun writeExceptionReleasesTrackAndReportsStoppedWithoutEscapingTheWorker() {
        val track = mockk<AudioTrack>(relaxed = true)
        every { track.write(any<ShortArray>(), any(), any(), AudioTrack.WRITE_BLOCKING) } throws
            IllegalStateException("track no longer initialized")

        runPlayback(track)

        verify(exactly = 1) { track.release() }
    }

    @Test
    fun startupFailureReleasesTheConstructedTrack() {
        val track = mockk<AudioTrack>(relaxed = true)
        mockkStatic(AudioTrack::class)
        mockkConstructor(AudioAttributes.Builder::class, AudioFormat.Builder::class, AudioTrack.Builder::class)
        try {
            every { AudioTrack.getMinBufferSize(any(), any(), any()) } returns 4_096
            stubAudioTrackBuilders(track)
            every { track.play() } throws IllegalStateException("track start failed")
            val player = AmbientSoundPlayer()

            assertThrows(IllegalStateException::class.java) { player.play(AmbientSoundPreset.WHITE_NOISE, 0.35f) }
            player.stop()

            verify(exactly = 1) { track.release() }
        } finally {
            unmockkStatic(AudioTrack::class)
            unmockkConstructor(AudioAttributes.Builder::class, AudioFormat.Builder::class, AudioTrack.Builder::class)
        }
    }

    @Test
    fun shortWriteKeepsTheRemainderOfTheSameBuffer() {
        val writes = mutableListOf<Pair<Int, Int>>()
        val track = mockk<AudioTrack>(relaxed = true)
        every { track.write(any<ShortArray>(), any(), any(), AudioTrack.WRITE_BLOCKING) } answers {
            writes += secondArg<Int>() to thirdArg<Int>()
            when (writes.size) {
                1 -> 1_024
                2 -> 1_024
                else -> AudioTrack.ERROR_DEAD_OBJECT
            }
        }

        runPlayback(track)

        assertEquals(listOf(0 to 2_048, 1_024 to 1_024, 0 to 2_048), writes)
        verify(exactly = 1) { track.release() }
    }

    @Test
    fun zeroWriteStopsWithoutGeneratingAnotherBuffer() {
        val track = mockk<AudioTrack>(relaxed = true)
        every { track.write(any<ShortArray>(), any(), any(), AudioTrack.WRITE_BLOCKING) } returnsMany
            listOf(0, AudioTrack.ERROR_DEAD_OBJECT)

        runPlayback(track)

        verify(exactly = 1) { track.write(any<ShortArray>(), any(), any(), AudioTrack.WRITE_BLOCKING) }
        verify(exactly = 1) { track.release() }
    }

    private fun runPlayback(track: AudioTrack) {
        val player = AmbientSoundPlayer()
        val running = AtomicBoolean(true)
        var stopped = 0
        listOf("running" to running, "audioTrack" to track).forEach { (name, value) ->
            AmbientSoundPlayer::class.java.getDeclaredField(name).apply {
                isAccessible = true
                set(player, value)
            }
        }
        AmbientSoundPlayer::class.java.getDeclaredMethod(
            "runPlayback",
            AmbientSoundPreset::class.java,
            Float::class.javaPrimitiveType,
            AudioTrack::class.java,
            AmbientSoundGenerator::class.java,
            AtomicBoolean::class.java,
            kotlin.jvm.functions.Function0::class.java,
        ).apply { isAccessible = true }
            .invoke(
                player,
                AmbientSoundPreset.WHITE_NOISE,
                0.35f,
                track,
                AmbientSoundGenerator(seed = 1L),
                running,
                { stopped++ },
            )
        assertEquals(1, stopped)
    }
}
