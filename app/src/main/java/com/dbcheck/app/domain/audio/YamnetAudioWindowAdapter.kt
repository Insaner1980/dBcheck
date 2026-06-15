package com.dbcheck.app.domain.audio

class YamnetAudioWindowAdapter(
    private val sourceSampleRateHz: Int = AudioProcessingConfig.SAMPLE_RATE,
    private val targetSampleRateHz: Int = YamnetAudioConfig.SAMPLE_RATE_HZ,
    private val windowSizeSamples: Int = YamnetAudioConfig.WINDOW_SIZE_SAMPLES,
) {
    private val window = FloatArray(windowSizeSamples)
    private var bufferedSamples = 0
    private var processedSourceSamples = 0L
    private var nextOutputSourcePosition = 0.0
    private var previousSourceSample: Float? = null
    private val sourceStep: Double = sourceSampleRateHz.toDouble() / targetSampleRateHz

    init {
        require(sourceSampleRateHz > 0) { "sourceSampleRateHz must be positive" }
        require(targetSampleRateHz > 0) { "targetSampleRateHz must be positive" }
        require(windowSizeSamples > 0) { "windowSizeSamples must be positive" }
    }

    fun appendPcm16(buffer: ShortArray, size: Int): FloatArray? {
        val safeSize = size.coerceIn(0, buffer.size)
        if (safeSize == 0) return latestWindowOrNull()

        appendFloatSamples(resamplePcm16(buffer, safeSize))
        previousSourceSample = normalizePcm16(buffer[safeSize - 1])
        processedSourceSamples += safeSize
        return latestWindowOrNull()
    }

    fun reset() {
        window.fill(0f)
        bufferedSamples = 0
        processedSourceSamples = 0L
        nextOutputSourcePosition = 0.0
        previousSourceSample = null
    }

    private fun latestWindowOrNull(): FloatArray? =
        if (bufferedSamples == windowSizeSamples) {
            window.copyOf()
        } else {
            null
        }

    private fun resamplePcm16(buffer: ShortArray, size: Int): FloatArray {
        if (sourceSampleRateHz == targetSampleRateHz) {
            return FloatArray(size) { index -> normalizePcm16(buffer[index]) }
        }

        val chunkStart = processedSourceSamples
        val chunkEndExclusive = chunkStart + size
        val output = mutableListOf<Float>()

        while (true) {
            val lowerSourceIndex = nextOutputSourcePosition.toLong()
            val upperSourceIndex = lowerSourceIndex + 1
            if (upperSourceIndex >= chunkEndExclusive) break

            val fraction = (nextOutputSourcePosition - lowerSourceIndex).toFloat()
            val lowerSample = sampleAt(lowerSourceIndex, chunkStart, buffer)
            val upperSample = sampleAt(upperSourceIndex, chunkStart, buffer)

            output += lowerSample + (upperSample - lowerSample) * fraction
            nextOutputSourcePosition += sourceStep
        }

        return output.toFloatArray()
    }

    private fun sampleAt(
        sourceIndex: Long,
        chunkStart: Long,
        buffer: ShortArray,
    ): Float =
        if (sourceIndex < chunkStart) {
            previousSourceSample ?: 0f
        } else {
            normalizePcm16(buffer[(sourceIndex - chunkStart).toInt()])
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

        fun normalizePcm16(sample: Short): Float =
            (sample / PCM16_POSITIVE_MAX).coerceIn(-1f, 1f)
    }
}
