package com.dbcheck.app.domain.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

class YamnetAudioWindowAdapter(
    private val sourceSampleRateHz: Int = AudioProcessingConfig.SAMPLE_RATE,
    private val targetSampleRateHz: Int = YamnetAudioConfig.SAMPLE_RATE_HZ,
    private val windowSizeSamples: Int = YamnetAudioConfig.WINDOW_SIZE_SAMPLES,
) {
    private val window = FloatArray(windowSizeSamples)
    private var bufferedSamples = 0
    private var hasEmittedWindow = false
    private var samplesSinceLastWindow = 0
    private var processedSourceSamples = 0L
    private var nextOutputSourcePosition = 0.0
    private val sourceHistory = FloatArray(ANTI_ALIAS_FILTER_TAP_COUNT - 1)
    private val sourceStep: Double = sourceSampleRateHz.toDouble() / targetSampleRateHz
    private val antiAliasCutoffHz: Double =
        minOf(
            targetSampleRateHz / 2.0,
            YamnetAudioConfig.MEL_MAX_HZ.toDouble(),
        )
    private val antiAliasCoefficientCache =
        arrayOfNulls<DoubleArray>(ANTI_ALIAS_PHASE_BUCKET_COUNT + 1)

    init {
        require(sourceSampleRateHz > 0) { "sourceSampleRateHz must be positive" }
        require(targetSampleRateHz > 0) { "targetSampleRateHz must be positive" }
        require(windowSizeSamples > 0) { "windowSizeSamples must be positive" }
    }

    fun appendPcm16(buffer: ShortArray, size: Int): FloatArray? {
        val safeSize = size.coerceIn(0, buffer.size)
        if (safeSize == 0) return null

        val normalizedSamples = FloatArray(safeSize) { index -> normalizePcm16(buffer[index]) }
        val resampledSamples = resample(normalizedSamples)
        appendFloatSamples(resampledSamples)
        updateSourceHistory(normalizedSamples)
        processedSourceSamples += safeSize
        return windowForEmissionOrNull(resampledSamples.size)
    }

    fun reset() {
        window.fill(0f)
        bufferedSamples = 0
        hasEmittedWindow = false
        samplesSinceLastWindow = 0
        processedSourceSamples = 0L
        nextOutputSourcePosition = 0.0
        sourceHistory.fill(0f)
    }

    private fun windowForEmissionOrNull(appendedSampleCount: Int): FloatArray? {
        if (bufferedSamples < windowSizeSamples) return null
        return if (!hasEmittedWindow) {
            hasEmittedWindow = true
            samplesSinceLastWindow = 0
            window.copyOf()
        } else {
            samplesSinceLastWindow += appendedSampleCount
            if (samplesSinceLastWindow < YamnetAudioConfig.HOP_SIZE_SAMPLES) {
                null
            } else {
                samplesSinceLastWindow %= YamnetAudioConfig.HOP_SIZE_SAMPLES
                window.copyOf()
            }
        }
    }

    private fun resample(samples: FloatArray): FloatArray {
        if (sourceSampleRateHz == targetSampleRateHz) {
            return samples
        }

        return if (sourceSampleRateHz > targetSampleRateHz) {
            resampleWithAntiAlias(samples)
        } else {
            resampleLinearly(samples)
        }
    }

    private fun resampleLinearly(samples: FloatArray): FloatArray {
        val chunkStart = processedSourceSamples
        val chunkEndExclusive = chunkStart + samples.size
        val output = mutableListOf<Float>()

        while (true) {
            val lowerSourceIndex = nextOutputSourcePosition.toLong()
            val upperSourceIndex = lowerSourceIndex + 1
            if (upperSourceIndex >= chunkEndExclusive) break

            val fraction = (nextOutputSourcePosition - lowerSourceIndex).toFloat()
            val lowerSample = sampleAt(lowerSourceIndex, chunkStart, samples)
            val upperSample = sampleAt(upperSourceIndex, chunkStart, samples)

            output += lowerSample + (upperSample - lowerSample) * fraction
            nextOutputSourcePosition += sourceStep
        }

        return output.toFloatArray()
    }

    private fun resampleWithAntiAlias(samples: FloatArray): FloatArray {
        val chunkStart = processedSourceSamples
        val chunkEndExclusive = chunkStart + samples.size
        val output = mutableListOf<Float>()

        while (true) {
            val baseSourceIndex = floor(nextOutputSourcePosition).toLong()
            if (baseSourceIndex >= chunkEndExclusive) break

            output += bandLimitedSampleAt(nextOutputSourcePosition, chunkStart, samples)
            nextOutputSourcePosition += sourceStep
        }

        return output.toFloatArray()
    }

    private fun bandLimitedSampleAt(sourcePosition: Double, chunkStart: Long, samples: FloatArray): Float {
        val baseSourceIndex = floor(sourcePosition).toLong()
        val fraction = sourcePosition - baseSourceIndex
        var weightedSampleSum = 0.0
        var coefficientSum = 0.0
        val coefficients = antiAliasCoefficientsFor(fraction)

        repeat(ANTI_ALIAS_FILTER_TAP_COUNT) { tapIndex ->
            val sourceIndex = baseSourceIndex - tapIndex
            val coefficient = coefficients[tapIndex]
            weightedSampleSum += sampleAt(sourceIndex, chunkStart, samples) * coefficient
            coefficientSum += coefficient
        }

        return if (abs(coefficientSum) > COEFFICIENT_EPSILON) {
            (weightedSampleSum / coefficientSum).toFloat().coerceIn(-1f, 1f)
        } else {
            0f
        }
    }

    private fun antiAliasCoefficientsFor(fraction: Double): DoubleArray {
        val phaseIndex =
            (fraction.coerceIn(0.0, 1.0) * ANTI_ALIAS_PHASE_BUCKET_COUNT)
                .roundToInt()
        return antiAliasCoefficientCache[phaseIndex]
            ?: buildAntiAliasCoefficients(phaseIndex.toDouble() / ANTI_ALIAS_PHASE_BUCKET_COUNT)
                .also { coefficients -> antiAliasCoefficientCache[phaseIndex] = coefficients }
    }

    private fun buildAntiAliasCoefficients(fraction: Double): DoubleArray =
        DoubleArray(ANTI_ALIAS_FILTER_TAP_COUNT) { tapIndex ->
            antiAliasCoefficient(tapIndex, fraction)
        }

    private fun antiAliasCoefficient(tapIndex: Int, fraction: Double): Double {
        val distanceFromCenter = tapIndex - ANTI_ALIAS_FILTER_CENTER + fraction
        val normalizedCutoff = antiAliasCutoffHz / sourceSampleRateHz
        val scaledDistance = 2.0 * normalizedCutoff * distanceFromCenter
        val sincValue =
            if (abs(scaledDistance) < COEFFICIENT_EPSILON) {
                1.0
            } else {
                sin(PI * scaledDistance) / (PI * scaledDistance)
            }
        val window =
            0.54 - 0.46 * cos(
                2.0 * PI * tapIndex / (ANTI_ALIAS_FILTER_TAP_COUNT - 1),
            )
        return 2.0 * normalizedCutoff * sincValue * window
    }

    private fun sampleAt(sourceIndex: Long, chunkStart: Long, samples: FloatArray): Float =
        if (sourceIndex < chunkStart) {
            val historyIndex = sourceHistory.size - (chunkStart - sourceIndex).toInt()
            if (historyIndex in sourceHistory.indices) {
                sourceHistory[historyIndex]
            } else {
                0f
            }
        } else {
            val sampleIndex = (sourceIndex - chunkStart).toInt()
            if (sampleIndex in samples.indices) {
                samples[sampleIndex]
            } else {
                0f
            }
        }

    private fun updateSourceHistory(samples: FloatArray) {
        if (samples.size >= sourceHistory.size) {
            samples.copyInto(
                destination = sourceHistory,
                startIndex = samples.size - sourceHistory.size,
                endIndex = samples.size,
            )
        } else {
            sourceHistory.copyInto(
                destination = sourceHistory,
                startIndex = samples.size,
                endIndex = sourceHistory.size,
            )
            samples.copyInto(sourceHistory, destinationOffset = sourceHistory.size - samples.size)
        }
    }

    private fun appendFloatSamples(samples: FloatArray) {
        val incoming =
            if (samples.size > windowSizeSamples) {
                samples.copyOfRange(samples.size - windowSizeSamples, samples.size)
            } else {
                samples
            }
        val totalSamples = minOf(windowSizeSamples, bufferedSamples + incoming.size)
        val retainedSamples = totalSamples - incoming.size

        if (retainedSamples > 0) {
            val sourceStart = bufferedSamples - retainedSamples
            window.copyInto(
                destination = window,
                destinationOffset = 0,
                startIndex = sourceStart,
                endIndex = bufferedSamples,
            )
        }
        incoming.copyInto(window, destinationOffset = retainedSamples)
        bufferedSamples = totalSamples
    }

    private companion object {
        const val PCM16_POSITIVE_MAX = 32_767f
        const val ANTI_ALIAS_FILTER_TAP_COUNT = 96
        const val ANTI_ALIAS_FILTER_CENTER = (ANTI_ALIAS_FILTER_TAP_COUNT - 1) / 2.0
        const val ANTI_ALIAS_PHASE_BUCKET_COUNT = 1_024
        const val COEFFICIENT_EPSILON = 1e-12

        fun normalizePcm16(sample: Short): Float = (sample / PCM16_POSITIVE_MAX).coerceIn(-1f, 1f)
    }
}
