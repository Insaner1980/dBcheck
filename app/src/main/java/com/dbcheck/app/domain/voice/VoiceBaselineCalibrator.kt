package com.dbcheck.app.domain.voice

import com.dbcheck.app.domain.audio.SoundClassification
import com.dbcheck.app.domain.noise.DecibelMath

data class VoiceBaselineCapture(val levelDb: Float, val sampleCount: Int, val capturedAtMs: Long)

class VoiceBaselineCalibrator(
    private val minSampleCount: Int = MIN_SAMPLE_COUNT,
    private val speechLabels: Set<String> = DEFAULT_SPEECH_LABELS,
) {
    private var speechActive = false
    private var totalEnergy = 0.0
    private var sampleCount = 0

    fun onClassification(classification: SoundClassification?) {
        speechActive = classification?.label?.trim()?.lowercase() in speechLabels
    }

    fun onReading(weightedDb: Float) {
        if (!speechActive || !weightedDb.isFinite()) return

        totalEnergy += DecibelMath.energyFromDb(weightedDb)
        sampleCount += 1
    }

    fun capture(capturedAtMs: Long): VoiceBaselineCapture? = if (sampleCount < minSampleCount || capturedAtMs <= 0L) {
            null
        } else {
            DecibelMath.energyAverageDb(totalEnergy, sampleCount)?.let { levelDb ->
                VoiceBaselineCapture(
                    levelDb = levelDb,
                    sampleCount = sampleCount,
                    capturedAtMs = capturedAtMs,
                )
            }
        }

    fun reset() {
        speechActive = false
        totalEnergy = 0.0
        sampleCount = 0
    }

    private companion object {
        const val MIN_SAMPLE_COUNT = 3
        val DEFAULT_SPEECH_LABELS = setOf("speech")
    }
}
