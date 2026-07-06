package com.dbcheck.app.domain.voice

data class TtsRiskPromptPolicy(val cooldownMs: Long = DEFAULT_COOLDOWN_MS) {
    init {
        require(cooldownMs >= 0L) { "cooldownMs must be non-negative" }
    }

    companion object {
        const val DEFAULT_COOLDOWN_MS = 30L * 60_000L

        val Default = TtsRiskPromptPolicy()
    }
}

enum class TtsRiskPromptRiskEvent {
    AverageDuration,
    DosimeterDose,
    ProjectedDose,
    Peak,
}

sealed interface TtsRiskPromptEvaluation {
    data object Disabled : TtsRiskPromptEvaluation

    data object FreeUser : TtsRiskPromptEvaluation

    data object MissingHearingBaseline : TtsRiskPromptEvaluation

    data object MissingSoundDetection : TtsRiskPromptEvaluation

    data object NotRiskEvent : TtsRiskPromptEvaluation

    data class CoolingDown(val remainingCooldownMs: Long) : TtsRiskPromptEvaluation

    data object Trigger : TtsRiskPromptEvaluation
}

class TtsRiskPromptEvaluator(private val policy: TtsRiskPromptPolicy = TtsRiskPromptPolicy.Default) {
    private var lastTriggeredAtMs: Long? = null

    fun reset() {
        lastTriggeredAtMs = null
    }

    fun evaluate(
        riskEvent: TtsRiskPromptRiskEvent,
        timestampMs: Long,
        isEnabled: Boolean,
        isProUser: Boolean,
        hasHearingBaseline: Boolean,
        soundDetectionAvailable: Boolean,
    ): TtsRiskPromptEvaluation = guardEvaluation(
            riskEvent = riskEvent,
            timestampMs = timestampMs,
            isEnabled = isEnabled,
            isProUser = isProUser,
            hasHearingBaseline = hasHearingBaseline,
            soundDetectionAvailable = soundDetectionAvailable,
        ) ?: cooldownEvaluation(timestampMs) ?: trigger(timestampMs)

    private fun guardEvaluation(
        riskEvent: TtsRiskPromptRiskEvent,
        timestampMs: Long,
        isEnabled: Boolean,
        isProUser: Boolean,
        hasHearingBaseline: Boolean,
        soundDetectionAvailable: Boolean,
    ): TtsRiskPromptEvaluation? = when {
            !isProUser -> TtsRiskPromptEvaluation.FreeUser
            !isEnabled -> TtsRiskPromptEvaluation.Disabled
            !hasHearingBaseline -> TtsRiskPromptEvaluation.MissingHearingBaseline
            !soundDetectionAvailable -> TtsRiskPromptEvaluation.MissingSoundDetection
            !riskEvent.isDosimeterRiskEvent || timestampMs <= 0L -> TtsRiskPromptEvaluation.NotRiskEvent
            else -> null
        }

    private fun cooldownEvaluation(timestampMs: Long): TtsRiskPromptEvaluation? = lastTriggeredAtMs
            ?.let { lastTriggerTimeMs -> (timestampMs - lastTriggerTimeMs).coerceAtLeast(0L) }
            ?.takeIf { elapsedSinceTriggerMs -> elapsedSinceTriggerMs < policy.cooldownMs }
            ?.let { elapsedSinceTriggerMs ->
                TtsRiskPromptEvaluation.CoolingDown(
                    remainingCooldownMs = policy.cooldownMs - elapsedSinceTriggerMs,
                )
            }

    private fun trigger(timestampMs: Long): TtsRiskPromptEvaluation {
        lastTriggeredAtMs = timestampMs
        return TtsRiskPromptEvaluation.Trigger
    }

    private val TtsRiskPromptRiskEvent.isDosimeterRiskEvent: Boolean
        get() = this == TtsRiskPromptRiskEvent.DosimeterDose || this == TtsRiskPromptRiskEvent.ProjectedDose
}
