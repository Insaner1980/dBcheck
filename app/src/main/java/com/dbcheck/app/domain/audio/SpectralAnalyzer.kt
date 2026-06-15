package com.dbcheck.app.domain.audio

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sqrt

data class SpectralBand(
    val startFrequencyHz: Float,
    val endFrequencyHz: Float,
    val centerFrequencyHz: Float,
    val normalizedAmplitude: Float,
)

enum class SpectralBandwidth {
    UNKNOWN,
    NARROW,
    MEDIUM,
    WIDE,
}

data class SpectralFrame(
    val bands: List<SpectralBand>,
    val dominantFrequencyHz: Float,
    val bandwidth: SpectralBandwidth,
    val timestamp: Long,
)

@Singleton
class SpectralAnalyzer
    @Inject
    constructor(private val fftProcessor: FFTProcessor) {
        fun analyze(buffer: ShortArray, size: Int, timestamp: Long = System.currentTimeMillis()): SpectralFrame {
            val magnitudes = fftProcessor.process(buffer, size)
            return if (magnitudes.isEmpty() || magnitudes.all { it == 0f }) {
                idleFrame(timestamp)
            } else {
                liveFrameOrIdle(magnitudes, timestamp)
            }
        }

        private fun liveFrameOrIdle(magnitudes: FloatArray, timestamp: Long): SpectralFrame {
            val bandMagnitudes = calculateBandMagnitudes(magnitudes)
            val maxMagnitude = bandMagnitudes.maxOrNull() ?: 0f
            return if (maxMagnitude <= 0f) {
                idleFrame(timestamp)
            } else {
                val bands = buildBands(bandMagnitudes, maxMagnitude)
                SpectralFrame(
                    bands = bands,
                    dominantFrequencyHz =
                        fftProcessor.findDominantFrequency(
                            magnitudes = magnitudes,
                            sampleRate = AudioProcessingConfig.SAMPLE_RATE,
                        ),
                    bandwidth = classifyBandwidth(bands),
                    timestamp = timestamp,
                )
            }
        }

        private fun buildBands(bandMagnitudes: List<Float>, maxMagnitude: Float): List<SpectralBand> =
            bandMagnitudes.mapIndexed { index, magnitude ->
                val start = bandEdge(index)
                val end = bandEdge(index + 1)
                SpectralBand(
                    startFrequencyHz = start,
                    endFrequencyHz = end,
                    centerFrequencyHz = sqrt(start * end),
                    normalizedAmplitude = (magnitude / maxMagnitude).coerceIn(0f, 1f),
                )
            }

        private fun calculateBandMagnitudes(magnitudes: FloatArray): List<Float> = List(BAND_COUNT) { bandIndex ->
                val start = bandEdge(bandIndex)
                val end = bandEdge(bandIndex + 1)
                magnitudes
                    .indices
                    .asSequence()
                    .filter { bin ->
                        val frequency = binFrequency(bin, magnitudes.size)
                        frequency >= start && frequency < end
                    }.map { magnitudes[it] }
                    .maxOrNull()
                    ?: 0f
            }

        private fun classifyBandwidth(bands: List<SpectralBand>): SpectralBandwidth {
            val activeBands = bands.count { it.normalizedAmplitude >= ACTIVE_BAND_THRESHOLD }
            return when {
                activeBands == 0 -> SpectralBandwidth.UNKNOWN
                activeBands <= 3 -> SpectralBandwidth.NARROW
                activeBands <= 8 -> SpectralBandwidth.MEDIUM
                else -> SpectralBandwidth.WIDE
            }
        }

        private fun idleFrame(timestamp: Long): SpectralFrame = SpectralFrame(
                bands =
                    List(BAND_COUNT) { index ->
                        val start = bandEdge(index)
                        val end = bandEdge(index + 1)
                        SpectralBand(
                            startFrequencyHz = start,
                            endFrequencyHz = end,
                            centerFrequencyHz = sqrt(start * end),
                            normalizedAmplitude = 0f,
                        )
                    },
                dominantFrequencyHz = 0f,
                bandwidth = SpectralBandwidth.UNKNOWN,
                timestamp = timestamp,
            )

        private fun binFrequency(bin: Int, magnitudeCount: Int): Float = FFTProcessor.binFrequency(bin, magnitudeCount)

        private fun bandEdge(index: Int): Float {
            val exponent = index.toFloat() / BAND_COUNT
            return MIN_FREQUENCY_HZ * FREQUENCY_RATIO.pow(exponent)
        }

        companion object {
            const val BAND_COUNT = 24
            private const val MIN_FREQUENCY_HZ = 20f
            private const val MAX_FREQUENCY_HZ = 20_000f
            private const val ACTIVE_BAND_THRESHOLD = 0.5f
            private const val FREQUENCY_RATIO = MAX_FREQUENCY_HZ / MIN_FREQUENCY_HZ
        }
    }
