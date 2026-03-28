package com.dbcheck.app.domain.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.sin

@Singleton
class ToneGenerator
    @Inject
    constructor() {
        companion object {
            private const val SAMPLE_RATE = 44100
            private const val DURATION_MS = 1500
        }

        private var audioTrack: AudioTrack? = null

        fun playTone(
            frequencyHz: Float,
            amplitudeDb: Float,
        ) {
            stop()

            val numSamples = SAMPLE_RATE * DURATION_MS / 1000
            val samples = ShortArray(numSamples)

            // Convert dB to linear amplitude (0 dB = max amplitude)
            val amplitude =
                Math
                    .pow(10.0, amplitudeDb / 20.0)
                    .toFloat()
                    .coerceIn(0f, 1f) * Short.MAX_VALUE

            // Generate sine wave with fade in/out
            val fadeLength = SAMPLE_RATE / 20 // 50ms fade
            for (i in samples.indices) {
                val rawSample = sin(2.0 * PI * frequencyHz * i / SAMPLE_RATE)
                val fadeFactor =
                    when {
                        i < fadeLength -> i.toFloat() / fadeLength
                        i > numSamples - fadeLength -> (numSamples - i).toFloat() / fadeLength
                        else -> 1f
                    }
                samples[i] =
                    (rawSample * amplitude * fadeFactor)
                        .toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        .toShort()
            }

            val bufferSize = samples.size * 2 // 2 bytes per short

            audioTrack =
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
                            .setSampleRate(SAMPLE_RATE)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    ).setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                    .apply {
                        write(samples, 0, samples.size)
                        play()
                    }
        }

        fun stop() {
            audioTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
            audioTrack = null
        }
    }
