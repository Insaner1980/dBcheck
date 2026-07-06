package com.dbcheck.app.domain.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class TtsRiskPromptPolicyTest {
    @Test
    fun doseRiskWithBaselineAndSoundDetectionTriggersPrompt() {
        val evaluator = TtsRiskPromptEvaluator(TtsRiskPromptPolicy(cooldownMs = 60_000L))

        val result =
            evaluator.evaluate(
                riskEvent = TtsRiskPromptRiskEvent.DosimeterDose,
                timestampMs = 10_000L,
                isEnabled = true,
                isProUser = true,
                hasHearingBaseline = true,
                soundDetectionAvailable = true,
            )

        assertEquals(
            TtsRiskPromptEvaluation.Trigger,
            result,
        )
    }

    @Test
    fun missingHearingBaselineBlocksPrompt() {
        val evaluator = TtsRiskPromptEvaluator()

        val result =
            evaluator.evaluate(
                riskEvent = TtsRiskPromptRiskEvent.DosimeterDose,
                timestampMs = 10_000L,
                isEnabled = true,
                isProUser = true,
                hasHearingBaseline = false,
                soundDetectionAvailable = true,
            )

        assertEquals(TtsRiskPromptEvaluation.MissingHearingBaseline, result)
    }

    @Test
    fun missingSoundDetectionBlocksPrompt() {
        val evaluator = TtsRiskPromptEvaluator()

        val result =
            evaluator.evaluate(
                riskEvent = TtsRiskPromptRiskEvent.DosimeterDose,
                timestampMs = 10_000L,
                isEnabled = true,
                isProUser = true,
                hasHearingBaseline = true,
                soundDetectionAvailable = false,
            )

        assertEquals(TtsRiskPromptEvaluation.MissingSoundDetection, result)
    }

    @Test
    fun averageDurationAlertIsNotTtsRiskEvent() {
        val evaluator = TtsRiskPromptEvaluator()

        val result =
            evaluator.evaluate(
                riskEvent = TtsRiskPromptRiskEvent.AverageDuration,
                timestampMs = 10_000L,
                isEnabled = true,
                isProUser = true,
                hasHearingBaseline = true,
                soundDetectionAvailable = true,
            )

        assertEquals(TtsRiskPromptEvaluation.NotRiskEvent, result)
    }

    @Test
    fun cooldownPreventsRepeatedPrompt() {
        val evaluator = TtsRiskPromptEvaluator(TtsRiskPromptPolicy(cooldownMs = 60_000L))
        evaluator.evaluate(
            riskEvent = TtsRiskPromptRiskEvent.ProjectedDose,
            timestampMs = 10_000L,
            isEnabled = true,
            isProUser = true,
            hasHearingBaseline = true,
            soundDetectionAvailable = true,
        )

        val result =
            evaluator.evaluate(
                riskEvent = TtsRiskPromptRiskEvent.ProjectedDose,
                timestampMs = 20_000L,
                isEnabled = true,
                isProUser = true,
                hasHearingBaseline = true,
                soundDetectionAvailable = true,
            )

        assertEquals(TtsRiskPromptEvaluation.CoolingDown(remainingCooldownMs = 50_000L), result)
    }
}
