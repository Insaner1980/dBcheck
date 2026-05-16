package com.dbcheck.app.domain.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ToneGeneratorTest {
    @Test
    fun playToneReleasesTrackWhenStaticBufferWriteIsIncomplete() {
        val track = FakeToneAudioTrack(writeResult = EXPECTED_STEREO_SAMPLE_COUNT - 1)
        val generator = ToneGenerator(fakeToneAudioTrackFactory(track))

        val error =
            assertThrows(IllegalStateException::class.java) {
                generator.playTone(
                    frequencyHz = 1000f,
                    amplitudeDb = -30f,
                    outputChannel = ToneOutputChannel.BOTH,
                )
            }

        assertEquals("Tone buffer write failed: wrote 132299 of 132300 samples", error.message)
        assertFalse(track.playCalled)
        assertEquals(1, track.releaseCount)
    }

    @Test
    fun playToneWritesStereoBufferWithOnlyLeftChannelAudible() {
        val track = FakeToneAudioTrack()
        val generator = ToneGenerator(fakeToneAudioTrackFactory(track))

        generator.playTone(
            frequencyHz = 1000f,
            amplitudeDb = -30f,
            outputChannel = ToneOutputChannel.LEFT,
        )

        assertEquals(EXPECTED_STEREO_SAMPLE_COUNT, track.lastAudioData.size)
        val nonSilentFrameIndex =
            (0 until EXPECTED_SAMPLE_FRAMES)
                .first { frameIndex -> track.lastAudioData[frameIndex * STEREO_CHANNEL_COUNT] != 0.toShort() }
        assertTrue(track.lastAudioData[nonSilentFrameIndex * STEREO_CHANNEL_COUNT] != 0.toShort())
        assertEquals(0, track.lastAudioData[nonSilentFrameIndex * STEREO_CHANNEL_COUNT + RIGHT_CHANNEL_OFFSET].toInt())
    }

    @Test
    fun playToneWritesStereoBufferWithOnlyRightChannelAudible() {
        val track = FakeToneAudioTrack()
        val generator = ToneGenerator(fakeToneAudioTrackFactory(track))

        generator.playTone(
            frequencyHz = 1000f,
            amplitudeDb = -30f,
            outputChannel = ToneOutputChannel.RIGHT,
        )

        assertEquals(EXPECTED_STEREO_SAMPLE_COUNT, track.lastAudioData.size)
        val nonSilentFrameIndex =
            (0 until EXPECTED_SAMPLE_FRAMES)
                .first { frameIndex ->
                    track.lastAudioData[frameIndex * STEREO_CHANNEL_COUNT + RIGHT_CHANNEL_OFFSET] != 0.toShort()
                }
        assertEquals(0, track.lastAudioData[nonSilentFrameIndex * STEREO_CHANNEL_COUNT].toInt())
        assertTrue(
            track.lastAudioData[nonSilentFrameIndex * STEREO_CHANNEL_COUNT + RIGHT_CHANNEL_OFFSET] != 0.toShort(),
        )
    }

    @Test
    fun stopReleasesTrackEvenWhenStopFails() {
        val track = FakeToneAudioTrack(throwOnStop = true)
        val generator = ToneGenerator(fakeToneAudioTrackFactory(track))

        generator.playTone(
            frequencyHz = 1000f,
            amplitudeDb = -30f,
            outputChannel = ToneOutputChannel.BOTH,
        )
        val error = assertThrows(IllegalStateException::class.java) { generator.stop() }

        assertEquals("Stop failed", error.message)
        assertEquals(1, track.stopCount)
        assertEquals(1, track.releaseCount)
    }

    private fun fakeToneAudioTrackFactory(track: FakeToneAudioTrack): ToneAudioTrackFactory = { bufferSizeInBytes ->
        assertEquals(EXPECTED_STEREO_SAMPLE_COUNT * BYTES_PER_PCM16_SAMPLE, bufferSizeInBytes)
        track
    }

    private class FakeToneAudioTrack(
        private val writeResult: Int = EXPECTED_STEREO_SAMPLE_COUNT,
        private val throwOnStop: Boolean = false,
    ) : ToneAudioTrack {
        lateinit var lastAudioData: ShortArray
            private set
        var playCalled = false
            private set
        var stopCount = 0
            private set
        var releaseCount = 0
            private set

        override val isPlaying: Boolean
            get() = playCalled

        override fun write(audioData: ShortArray, offsetInShorts: Int, sizeInShorts: Int): Int {
            assertEquals(0, offsetInShorts)
            assertEquals(EXPECTED_STEREO_SAMPLE_COUNT, sizeInShorts)
            assertEquals(EXPECTED_STEREO_SAMPLE_COUNT, audioData.size)
            lastAudioData = audioData.copyOf()
            return writeResult
        }

        override fun play() {
            playCalled = true
        }

        override fun stop() {
            stopCount++
            if (throwOnStop) throw IllegalStateException("Stop failed")
        }

        override fun release() {
            releaseCount++
        }
    }

    private companion object {
        const val STEREO_CHANNEL_COUNT = 2
        const val EXPECTED_SAMPLE_FRAMES = 66_150
        const val EXPECTED_STEREO_SAMPLE_COUNT = EXPECTED_SAMPLE_FRAMES * STEREO_CHANNEL_COUNT
        const val BYTES_PER_PCM16_SAMPLE = 2
        const val RIGHT_CHANNEL_OFFSET = 1
    }
}
