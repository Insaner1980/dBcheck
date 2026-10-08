package com.dbcheck.app.service

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.dbcheck.app.domain.ambient.AmbientSoundGenerator
import com.dbcheck.app.domain.ambient.AmbientSoundPolicy
import com.dbcheck.app.domain.ambient.AmbientSoundPreset
import javax.inject.Inject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

class AmbientSoundPlayer
    @Inject
    constructor() {
        private val lock = Any()
        private var audioTrack: AudioTrack? = null
        private var playbackThread: Thread? = null

        private var running: AtomicBoolean? = null

        @Volatile
        private var paused = false

        fun play(preset: AmbientSoundPreset, volume: Float): Boolean = synchronized(lock) {
            stopLocked()
            val minBufferSize =
                AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                )
            if (minBufferSize <= 0) return@synchronized false

            val normalizedVolume = AmbientSoundPolicy.normalizeVolume(volume)
            val track =
                AudioTrack
                    .Builder()
                    .setAudioAttributes(playbackAudioAttributes())
                    .setAudioFormat(playbackAudioFormat())
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setBufferSizeInBytes(max(minBufferSize, CHUNK_SAMPLES * BYTES_PER_SAMPLE * BUFFER_CHUNKS))
                    .build()
                    .also { builtTrack ->
                        builtTrack.setVolume(normalizedVolume)
                        builtTrack.play()
                    }
            val generator = AmbientSoundGenerator(sampleRate = SAMPLE_RATE)
            val sessionRunning = AtomicBoolean(true)
            running = sessionRunning
            paused = false
            audioTrack = track
            playbackThread =
                Thread {
                    runPlayback(preset, normalizedVolume, track, generator, sessionRunning)
                }.apply {
                    name = "AmbientSoundPlayer"
                    isDaemon = true
                    start()
                }
            true
        }

        private fun runPlayback(
            preset: AmbientSoundPreset,
            normalizedVolume: Float,
            track: AudioTrack,
            generator: AmbientSoundGenerator,
            sessionRunning: AtomicBoolean,
        ) {
            val samples = ShortArray(CHUNK_SAMPLES)
            try {
                while (sessionRunning.get()) {
                    if (paused) {
                        Thread.sleep(PAUSE_SLEEP_MILLIS)
                        continue
                    }
                    generator.generateInto(
                        preset = preset,
                        samples = samples,
                        volume = normalizedVolume,
                    )
                    if (sessionRunning.get()) {
                        val written = track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
                        if (written < 0) sessionRunning.set(false)
                    }
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            } finally {
                sessionRunning.set(false)
                synchronized(lock) {
                    if (running === sessionRunning) stopLocked()
                }
            }
        }

        fun pause() = synchronized(lock) {
            if (running?.get() != true || paused) return@synchronized
            paused = true
            runCatching { audioTrack?.pause() }
        }

        fun resume() = synchronized(lock) {
            if (running?.get() != true || !paused) return@synchronized
            paused = false
            runCatching { audioTrack?.play() }
        }

        fun stop() = synchronized(lock) {
            stopLocked()
        }

        private fun stopLocked() {
            running?.set(false)
            running = null
            paused = false
            playbackThread?.interrupt()
            playbackThread = null
            audioTrack?.let { track ->
                runCatching { track.pause() }
                runCatching { track.flush() }
                runCatching { track.release() }
            }
            audioTrack = null
        }

        private fun playbackAudioAttributes(): AudioAttributes = AudioAttributes
                .Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

        private fun playbackAudioFormat(): AudioFormat = AudioFormat
                .Builder()
                .setSampleRate(SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

        private companion object {
            const val SAMPLE_RATE = 44_100
            const val CHUNK_SAMPLES = 2_048
            const val BYTES_PER_SAMPLE = 2
            const val BUFFER_CHUNKS = 4
            const val PAUSE_SLEEP_MILLIS = 50L
        }
    }
