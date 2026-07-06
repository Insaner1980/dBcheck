package com.dbcheck.app.domain.noise

data class AudibleAlarmPolicy(
    val thresholdDb: Float = DEFAULT_THRESHOLD_DB,
    val requiredDurationMs: Long = DEFAULT_REQUIRED_DURATION_MS,
    val cooldownMs: Long = DEFAULT_COOLDOWN_MS,
) {
    init {
        require(thresholdDb.isFinite()) { "thresholdDb must be finite" }
        require(requiredDurationMs >= 0L) { "requiredDurationMs must be non-negative" }
        require(cooldownMs >= 0L) { "cooldownMs must be non-negative" }
    }

    companion object {
        const val DEFAULT_THRESHOLD_DB = 90f
        const val DEFAULT_REQUIRED_DURATION_MS = 30_000L
        const val DEFAULT_COOLDOWN_MS = 5L * 60_000L

        val Default = AudibleAlarmPolicy()
    }
}

sealed interface AudibleAlarmEvaluation {
    val thresholdDb: Float

    data class BelowThreshold(override val thresholdDb: Float) : AudibleAlarmEvaluation

    data class Waiting(
        override val thresholdDb: Float,
        val elapsedAboveThresholdMs: Long,
        val remainingDurationMs: Long,
    ) : AudibleAlarmEvaluation

    data class CoolingDown(override val thresholdDb: Float, val remainingCooldownMs: Long) : AudibleAlarmEvaluation

    data class Trigger(override val thresholdDb: Float, val sustainedDurationMs: Long) : AudibleAlarmEvaluation
}

class AudibleAlarmEvaluator {
    private var aboveThresholdSinceMs: Long? = null
    private var lastTriggeredAtMs: Long? = null

    fun reset() {
        aboveThresholdSinceMs = null
        lastTriggeredAtMs = null
    }

    fun evaluate(
        weightedDb: Float,
        timestampMs: Long,
        policy: AudibleAlarmPolicy = AudibleAlarmPolicy.Default,
    ): AudibleAlarmEvaluation = if (weightedDb < policy.thresholdDb) {
            belowThreshold(policy)
        } else {
            cooldownEvaluation(timestampMs, policy) ?: aboveThresholdEvaluation(timestampMs, policy)
        }

    private fun belowThreshold(policy: AudibleAlarmPolicy): AudibleAlarmEvaluation {
        aboveThresholdSinceMs = null
        return AudibleAlarmEvaluation.BelowThreshold(thresholdDb = policy.thresholdDb)
    }

    private fun cooldownEvaluation(timestampMs: Long, policy: AudibleAlarmPolicy): AudibleAlarmEvaluation? =
        lastTriggeredAtMs
            ?.let { lastTriggerTimeMs -> (timestampMs - lastTriggerTimeMs).coerceAtLeast(0L) }
            ?.takeIf { elapsedSinceTriggerMs -> elapsedSinceTriggerMs < policy.cooldownMs }
            ?.let { elapsedSinceTriggerMs ->
                aboveThresholdSinceMs = null
                AudibleAlarmEvaluation.CoolingDown(
                    thresholdDb = policy.thresholdDb,
                    remainingCooldownMs = policy.cooldownMs - elapsedSinceTriggerMs,
                )
            }

    private fun aboveThresholdEvaluation(timestampMs: Long, policy: AudibleAlarmPolicy): AudibleAlarmEvaluation {
        val currentAboveThresholdSinceMs = aboveThresholdSinceMs ?: timestampMs
        aboveThresholdSinceMs = currentAboveThresholdSinceMs
        val elapsedAboveThresholdMs = (timestampMs - currentAboveThresholdSinceMs).coerceAtLeast(0L)
        return if (elapsedAboveThresholdMs >= policy.requiredDurationMs) {
            lastTriggeredAtMs = timestampMs
            aboveThresholdSinceMs = null
            AudibleAlarmEvaluation.Trigger(
                thresholdDb = policy.thresholdDb,
                sustainedDurationMs = elapsedAboveThresholdMs,
            )
        } else {
            AudibleAlarmEvaluation.Waiting(
                thresholdDb = policy.thresholdDb,
                elapsedAboveThresholdMs = elapsedAboveThresholdMs,
                remainingDurationMs = policy.requiredDurationMs - elapsedAboveThresholdMs,
            )
        }
    }
}
