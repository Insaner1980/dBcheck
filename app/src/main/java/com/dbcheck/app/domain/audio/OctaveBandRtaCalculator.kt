package com.dbcheck.app.domain.audio

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sqrt

enum class RtaResolution(internal val bandsPerOctave: Int, internal val bandNumbers: IntRange) {
    OCTAVE(
        bandsPerOctave = 1,
        bandNumbers = -5..4,
    ),
    THIRD_OCTAVE(
        bandsPerOctave = 3,
        bandNumbers = -17..13,
    ),
}

data class RtaBand(
    val lowerEdgeFrequencyHz: Float,
    val centerFrequencyHz: Float,
    val upperEdgeFrequencyHz: Float,
    val normalizedAmplitude: Float,
)

data class RtaFrame(val bands: List<RtaBand>, val resolution: RtaResolution, val timestamp: Long)

@Singleton
class OctaveBandRtaCalculator
    @Inject
    constructor(private val fftProcessor: FFTProcessor) {
        fun analyze(
            buffer: ShortArray,
            size: Int,
            resolution: RtaResolution = RtaResolution.THIRD_OCTAVE,
            timestamp: Long = System.currentTimeMillis(),
        ): RtaFrame = calculateFromMagnitudes(
                magnitudes = fftProcessor.process(buffer, size),
                sampleRate = AudioProcessingConfig.SAMPLE_RATE,
                resolution = resolution,
                timestamp = timestamp,
            )

        internal fun calculateFromMagnitudes(
            magnitudes: FloatArray,
            sampleRate: Int = AudioProcessingConfig.SAMPLE_RATE,
            resolution: RtaResolution = RtaResolution.THIRD_OCTAVE,
            timestamp: Long = System.currentTimeMillis(),
        ): RtaFrame {
            val templates = rtaBandTemplates(resolution)
            val bandMagnitudes =
                templates.map { band ->
                    bandMagnitude(
                        magnitudes = magnitudes,
                        sampleRate = sampleRate,
                        lowerEdgeFrequencyHz = band.lowerEdgeFrequencyHz,
                        upperEdgeFrequencyHz = band.upperEdgeFrequencyHz,
                    )
                }
            val maxMagnitude = bandMagnitudes.maxOrNull() ?: 0f
            val bands =
                templates.mapIndexed { index, template ->
                    template.copy(
                        normalizedAmplitude =
                            normalizedAmplitude(
                                magnitude = bandMagnitudes[index],
                                maxMagnitude = maxMagnitude,
                            ),
                    )
                }
            return RtaFrame(
                bands = bands,
                resolution = resolution,
                timestamp = timestamp,
            )
        }

        private fun rtaBandTemplates(resolution: RtaResolution): List<RtaBand> =
            resolution.bandNumbers.map { bandNumber ->
                val centerFrequencyHz = centerFrequencyHz(bandNumber, resolution.bandsPerOctave)
                val edgeRatio = OCTAVE_RATIO.pow(1.0 / (2.0 * resolution.bandsPerOctave))
                RtaBand(
                    lowerEdgeFrequencyHz = (centerFrequencyHz / edgeRatio).toFloat(),
                    centerFrequencyHz = centerFrequencyHz.toFloat(),
                    upperEdgeFrequencyHz = (centerFrequencyHz * edgeRatio).toFloat(),
                    normalizedAmplitude = 0f,
                )
            }

        private fun centerFrequencyHz(bandNumber: Int, bandsPerOctave: Int): Double =
            REFERENCE_FREQUENCY_HZ * OCTAVE_RATIO.pow(bandNumber.toDouble() / bandsPerOctave)

        private fun bandMagnitude(
            magnitudes: FloatArray,
            sampleRate: Int,
            lowerEdgeFrequencyHz: Float,
            upperEdgeFrequencyHz: Float,
        ): Float {
            var powerSum = 0.0
            for (bin in magnitudes.indices) {
                val frequency = FFTProcessor.binFrequency(bin, magnitudes.size, sampleRate)
                if (frequency >= lowerEdgeFrequencyHz && frequency < upperEdgeFrequencyHz) {
                    powerSum += magnitudes[bin].toDouble().pow(2)
                }
            }
            return sqrt(powerSum).toFloat()
        }

        private fun normalizedAmplitude(magnitude: Float, maxMagnitude: Float): Float = if (maxMagnitude <= 0f) {
                0f
            } else {
                (magnitude / maxMagnitude).coerceIn(0f, 1f)
            }

        private companion object {
            const val REFERENCE_FREQUENCY_HZ = 1_000.0
            val OCTAVE_RATIO: Double = 10.0.pow(3.0 / 10.0)
        }
    }
