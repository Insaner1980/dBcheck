package com.dbcheck.app.domain.audio

import com.dbcheck.app.domain.calibration.OctaveCalibrationOffsets
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

    ;

    fun centerFrequenciesHz(): List<Float> =
        bandNumbers.map { bandNumber -> centerFrequencyHz(bandNumber, bandsPerOctave).toFloat() }

    internal companion object {
        const val REFERENCE_FREQUENCY_HZ = 1_000.0
        val OCTAVE_RATIO: Double = 10.0.pow(3.0 / 10.0)

        fun centerFrequencyHz(bandNumber: Int, bandsPerOctave: Int): Double =
            REFERENCE_FREQUENCY_HZ * OCTAVE_RATIO.pow(bandNumber.toDouble() / bandsPerOctave)
    }
}

data class RtaBand(
    val lowerEdgeFrequencyHz: Float,
    val centerFrequencyHz: Float,
    val upperEdgeFrequencyHz: Float,
    val normalizedAmplitude: Float,
    val calibrationOffsetDb: Float = 0f,
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
            octaveCalibrationOffsets: OctaveCalibrationOffsets = OctaveCalibrationOffsets.zero(),
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
            val calibratedBandMagnitudes =
                templates.mapIndexed { index, template ->
                    bandMagnitudes[index] * template.calibrationGain(resolution, octaveCalibrationOffsets)
                }
            val maxMagnitude = calibratedBandMagnitudes.maxOrNull() ?: 0f
            val bands =
                templates.mapIndexed { index, template ->
                    val calibrationOffsetDb = template.calibrationOffsetDb(resolution, octaveCalibrationOffsets)
                    template.copy(
                        normalizedAmplitude =
                            normalizedAmplitude(
                                magnitude = calibratedBandMagnitudes[index],
                                maxMagnitude = maxMagnitude,
                            ),
                        calibrationOffsetDb = calibrationOffsetDb,
                    )
                }
            return RtaFrame(
                bands = bands,
                resolution = resolution,
                timestamp = timestamp,
            )
        }

        private fun rtaBandTemplates(resolution: RtaResolution): List<RtaBand> =
            resolution.centerFrequenciesHz().map { centerFrequencyHz ->
                val edgeRatio = OCTAVE_RATIO.pow(1.0 / (2.0 * resolution.bandsPerOctave))
                RtaBand(
                    lowerEdgeFrequencyHz = (centerFrequencyHz / edgeRatio).toFloat(),
                    centerFrequencyHz = centerFrequencyHz,
                    upperEdgeFrequencyHz = (centerFrequencyHz * edgeRatio).toFloat(),
                    normalizedAmplitude = 0f,
                )
            }

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

        private fun RtaBand.calibrationGain(
            resolution: RtaResolution,
            octaveCalibrationOffsets: OctaveCalibrationOffsets,
        ): Float = 10.0.pow(calibrationOffsetDb(resolution, octaveCalibrationOffsets).toDouble() / 20.0).toFloat()

        private fun RtaBand.calibrationOffsetDb(
            resolution: RtaResolution,
            octaveCalibrationOffsets: OctaveCalibrationOffsets,
        ): Float = if (resolution == RtaResolution.OCTAVE) {
            octaveCalibrationOffsets.offsetFor(centerFrequencyHz)
        } else {
            0f
        }

        private companion object {
            val OCTAVE_RATIO: Double = RtaResolution.OCTAVE_RATIO
        }
    }
