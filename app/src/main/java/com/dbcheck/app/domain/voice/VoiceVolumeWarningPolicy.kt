package com.dbcheck.app.domain.voice

import com.dbcheck.app.domain.audio.SoundClassification

data class VoiceVolumeWarningPolicy(
    val thresholdAboveBaselineDb: Float = DEFAULT_THRESHOLD_ABOVE_BASELINE_DB,
    val requiredDurationMs: Long = DEFAULT_REQUIRED_DURATION_MS,
    val cooldownMs: Long = DEFAULT_COOLDOWN_MS,
    val speechLabels: Set<String> = DEFAULT_SPEECH_LABELS,
) {
    init {
        require(thresholdAboveBaselineDb.isFinite()) { "thresholdAboveBaselineDb must be finite" }
        require(requiredDurationMs >= 0L) { "requiredDurationMs must be non-negative" }
        require(cooldownMs >= 0L) { "cooldownMs must be non-negative" }
        require(speechLabels.isNotEmpty()) { "speechLabels must not be empty" }
    }

    fun thresholdDb(baselineDb: Float): Float = baselineDb + thresholdAboveBaselineDb

    companion object {
        const val DEFAULT_THRESHOLD_ABOVE_BASELINE_DB = 8f
        const val DEFAULT_REQUIRED_DURATION_MS = 3_000L
        const val DEFAULT_COOLDOWN_MS = 60_000L

        private val DEFAULT_SPEECH_LABELS = setOf("speech")

        val Default = VoiceVolumeWarningPolicy()
    }
}

sealed interface VoiceVolumeWarningEvaluation {
    data object MissingBaseline : VoiceVolumeWarningEvaluation

    data object NotSpeech : VoiceVolumeWarningEvaluation

    data class BelowThreshold(val thresholdDb: Float) : VoiceVolumeWarningEvaluation

    data class Waiting(val thresholdDb: Float, val elapsedAboveThresholdMs: Long, val remainingDurationMs: Long) :
        VoiceVolumeWarningEvaluation

    data class CoolingDown(val thresholdDb: Float, val remainingCooldownMs: Long) : VoiceVolumeWarningEvaluation

    data class Trigger(
        val thresholdDb: Float,
        val baselineDb: Float,
        val currentDb: Float,
        val sustainedDurationMs: Long,
    ) : VoiceVolumeWarningEvaluation
}

class VoiceVolumeWarningEvaluator(private val policy: VoiceVolumeWarningPolicy = VoiceVolumeWarningPolicy.Default) {
    private var speechActive = false
    private var aboveThresholdSinceMs: Long? = null
    private var lastTriggeredAtMs: Long? = null

    fun onClassification(classification: SoundClassification?) {
        speechActive = classification?.label?.trim()?.lowercase() in policy.speechLabels
        if (!speechActive) {
            aboveThresholdSinceMs = null
        }
    }

    fun reset() {
        speechActive = false
        aboveThresholdSinceMs = null
        lastTriggeredAtMs = null
    }

    fun evaluate(weightedDb: Float, timestampMs: Long, baselineDb: Float?): VoiceVolumeWarningEvaluation {
        val baseline = baselineDb?.takeIf { it.isFinite() }
        return when {
            baseline == null || timestampMs <= 0L -> missingBaseline()
            !speechActive || !weightedDb.isFinite() -> notSpeech()
            else -> speechEvaluation(weightedDb = weightedDb, timestampMs = timestampMs, baseline = baseline)
        }
    }

    private fun missingBaseline(): VoiceVolumeWarningEvaluation {
        aboveThresholdSinceMs = null
        return VoiceVolumeWarningEvaluation.MissingBaseline
    }

    private fun notSpeech(): VoiceVolumeWarningEvaluation {
        aboveThresholdSinceMs = null
        return VoiceVolumeWarningEvaluation.NotSpeech
    }

    private fun speechEvaluation(weightedDb: Float, timestampMs: Long, baseline: Float): VoiceVolumeWarningEvaluation {
        val thresholdDb = policy.thresholdDb(baseline)
        return when {
            weightedDb < thresholdDb -> belowThreshold(thresholdDb)

            else -> cooldownEvaluation(timestampMs, thresholdDb)
                ?: aboveThresholdEvaluation(
                    weightedDb = weightedDb,
                    timestampMs = timestampMs,
                    baseline = baseline,
                    thresholdDb = thresholdDb,
                )
        }
    }

    private fun belowThreshold(thresholdDb: Float): VoiceVolumeWarningEvaluation {
        aboveThresholdSinceMs = null
        return VoiceVolumeWarningEvaluation.BelowThreshold(thresholdDb = thresholdDb)
    }

    private fun cooldownEvaluation(timestampMs: Long, thresholdDb: Float): VoiceVolumeWarningEvaluation? =
        lastTriggeredAtMs
            ?.let { lastTriggerTimeMs -> (timestampMs - lastTriggerTimeMs).coerceAtLeast(0L) }
            ?.takeIf { elapsedSinceTriggerMs -> elapsedSinceTriggerMs < policy.cooldownMs }
            ?.let { elapsedSinceTriggerMs ->
                aboveThresholdSinceMs = null
                VoiceVolumeWarningEvaluation.CoolingDown(
                    thresholdDb = thresholdDb,
                    remainingCooldownMs = policy.cooldownMs - elapsedSinceTriggerMs,
                )
            }

    private fun aboveThresholdEvaluation(
        weightedDb: Float,
        timestampMs: Long,
        baseline: Float,
        thresholdDb: Float,
    ): VoiceVolumeWarningEvaluation {
        val currentAboveThresholdSinceMs = aboveThresholdSinceMs ?: timestampMs
        aboveThresholdSinceMs = currentAboveThresholdSinceMs
        val elapsedAboveThresholdMs = (timestampMs - currentAboveThresholdSinceMs).coerceAtLeast(0L)
        return if (elapsedAboveThresholdMs >= policy.requiredDurationMs) {
            lastTriggeredAtMs = timestampMs
            aboveThresholdSinceMs = null
            VoiceVolumeWarningEvaluation.Trigger(
                thresholdDb = thresholdDb,
                baselineDb = baseline,
                currentDb = weightedDb,
                sustainedDurationMs = elapsedAboveThresholdMs,
            )
        } else {
            VoiceVolumeWarningEvaluation.Waiting(
                thresholdDb = thresholdDb,
                elapsedAboveThresholdMs = elapsedAboveThresholdMs,
                remainingDurationMs = policy.requiredDurationMs - elapsedAboveThresholdMs,
            )
        }
    }
}
