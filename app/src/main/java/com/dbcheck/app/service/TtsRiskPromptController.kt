package com.dbcheck.app.service

import com.dbcheck.app.domain.voice.TtsRiskPromptEvaluation
import com.dbcheck.app.domain.voice.TtsRiskPromptEvaluator
import com.dbcheck.app.domain.voice.TtsRiskPromptPolicy
import com.dbcheck.app.domain.voice.TtsRiskPromptRiskEvent
import javax.inject.Inject
import javax.inject.Singleton

fun interface TtsPromptPlayer {
    fun speak(text: String): Boolean
}

enum class TtsRiskPromptPlaybackResult {
    SkippedDisabled,
    SkippedFreeUser,
    SkippedMissingHearingBaseline,
    SkippedMissingSoundDetection,
    SkippedNotRiskEvent,
    CoolingDown,
    PlaybackFailed,
    Played,
}

@Singleton
class TtsRiskPromptController(
    private val player: TtsPromptPlayer,
    private val evaluator: TtsRiskPromptEvaluator = TtsRiskPromptEvaluator(TtsRiskPromptPolicy.Default),
) {
    @Inject
    constructor(player: TtsPromptPlayer) : this(
        player = player,
        evaluator = TtsRiskPromptEvaluator(TtsRiskPromptPolicy.Default),
    )

    fun reset() {
        evaluator.reset()
    }

    fun onRiskEvent(
        riskEvent: TtsRiskPromptRiskEvent,
        timestampMs: Long,
        isEnabled: Boolean,
        isProUser: Boolean,
        hasHearingBaseline: Boolean,
        soundDetectionAvailable: Boolean,
        promptMessage: String,
    ): TtsRiskPromptPlaybackResult = when (
            val evaluation =
                evaluator.evaluate(
                    riskEvent = riskEvent,
                    timestampMs = timestampMs,
                    isEnabled = isEnabled,
                    isProUser = isProUser,
                    hasHearingBaseline = hasHearingBaseline,
                    soundDetectionAvailable = soundDetectionAvailable,
                )
        ) {
            TtsRiskPromptEvaluation.Disabled -> TtsRiskPromptPlaybackResult.SkippedDisabled

            TtsRiskPromptEvaluation.FreeUser -> TtsRiskPromptPlaybackResult.SkippedFreeUser

            TtsRiskPromptEvaluation.MissingHearingBaseline -> TtsRiskPromptPlaybackResult.SkippedMissingHearingBaseline

            TtsRiskPromptEvaluation.MissingSoundDetection -> TtsRiskPromptPlaybackResult.SkippedMissingSoundDetection

            TtsRiskPromptEvaluation.NotRiskEvent -> TtsRiskPromptPlaybackResult.SkippedNotRiskEvent

            is TtsRiskPromptEvaluation.CoolingDown -> TtsRiskPromptPlaybackResult.CoolingDown

            TtsRiskPromptEvaluation.Trigger ->
                if (player.speak(promptMessage)) {
                    TtsRiskPromptPlaybackResult.Played
                } else {
                    TtsRiskPromptPlaybackResult.PlaybackFailed
                }
        }
}
