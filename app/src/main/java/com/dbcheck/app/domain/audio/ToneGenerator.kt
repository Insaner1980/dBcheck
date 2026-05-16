package com.dbcheck.app.domain.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.sin

@Singleton
class ToneGenerator internal constructor(private val audioTrackFactory: ToneAudioTrackFactory) {
    @Inject
    constructor() : this(androidToneAudioTrackFactory)

    companion object {
        private const val DURATION_MS = 1500
        private const val BYTES_PER_PCM16_SAMPLE = 2
        private const val STEREO_CHANNEL_COUNT = 2
        private const val RIGHT_CHANNEL_OFFSET = 1
    }

    private var audioTrack: ToneAudioTrack? = null

    fun playTone(
        frequencyHz: Float,
        amplitudeDb: Float,
        outputChannel: ToneOutputChannel = ToneOutputChannel.BOTH,
    ) {
        stop()

        val numFrames = AudioProcessingConfig.SAMPLE_RATE * DURATION_MS / 1000
        val samples = ShortArray(numFrames * STEREO_CHANNEL_COUNT)

        val amplitude =
            Math
                .pow(10.0, amplitudeDb / 20.0)
                .toFloat()
                .coerceIn(0f, 1f) * Short.MAX_VALUE

        val fadeLength = AudioProcessingConfig.SAMPLE_RATE / 20
        for (frameIndex in 0 until numFrames) {
            val rawSample = sin(2.0 * PI * frequencyHz * frameIndex / AudioProcessingConfig.SAMPLE_RATE)
            val fadeFactor =
                when {
                    frameIndex < fadeLength -> frameIndex.toFloat() / fadeLength
                    frameIndex > numFrames - fadeLength -> (numFrames - frameIndex).toFloat() / fadeLength
                    else -> 1f
                }
            val sample =
                (rawSample * amplitude * fadeFactor)
                    .toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    .toShort()
            val leftSample = if (outputChannel != ToneOutputChannel.RIGHT) sample else 0.toShort()
            val rightSample = if (outputChannel != ToneOutputChannel.LEFT) sample else 0.toShort()
            val sampleIndex = frameIndex * STEREO_CHANNEL_COUNT
            samples[sampleIndex] = leftSample
            samples[sampleIndex + RIGHT_CHANNEL_OFFSET] = rightSample
        }

        val bufferSize = samples.size * BYTES_PER_PCM16_SAMPLE

        val track = audioTrackFactory(bufferSize)
        var releaseOnFailure = true
        try {
            val writtenSamples = track.write(samples, 0, samples.size)
            check(writtenSamples == samples.size) {
                "Tone buffer write failed: wrote $writtenSamples of ${samples.size} samples"
            }
            track.play()
            audioTrack = track
            releaseOnFailure = false
        } finally {
            if (releaseOnFailure) {
                track.release()
            }
        }
    }

    fun stop() {
        val track = audioTrack ?: return
        audioTrack = null
        try {
            if (track.isPlaying) {
                track.stop()
            }
        } finally {
            track.release()
        }
    }
}

enum class ToneOutputChannel {
    LEFT,
    RIGHT,
    BOTH,
}

internal typealias ToneAudioTrackFactory = (Int) -> ToneAudioTrack

internal interface ToneAudioTrack {
    val isPlaying: Boolean

    fun write(audioData: ShortArray, offsetInShorts: Int, sizeInShorts: Int): Int

    fun play()

    fun stop()

    fun release()
}

private val androidToneAudioTrackFactory: ToneAudioTrackFactory = { bufferSizeInBytes ->
    AndroidToneAudioTrack(
        AudioTrack
            .Builder()
            .setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            ).setAudioFormat(
                AudioFormat
                    .Builder()
                    .setSampleRate(AudioProcessingConfig.SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build(),
            ).setBufferSizeInBytes(bufferSizeInBytes)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build(),
    )
}

private class AndroidToneAudioTrack(private val audioTrack: AudioTrack) : ToneAudioTrack {
    override val isPlaying: Boolean
        get() = audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING

    override fun write(audioData: ShortArray, offsetInShorts: Int, sizeInShorts: Int): Int =
        audioTrack.write(audioData, offsetInShorts, sizeInShorts)

    override fun play() {
        audioTrack.play()
    }

    override fun stop() {
        audioTrack.stop()
    }

    override fun release() {
        audioTrack.release()
    }
}
