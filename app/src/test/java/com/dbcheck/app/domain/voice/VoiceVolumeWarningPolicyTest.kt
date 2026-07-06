package com.dbcheck.app.domain.voice

import com.dbcheck.app.domain.audio.SoundClassification
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceVolumeWarningPolicyTest {
    private val policy =
        VoiceVolumeWarningPolicy(
            thresholdAboveBaselineDb = 8f,
            requiredDurationMs = 3_000L,
            cooldownMs = 60_000L,
        )

    @Test
    fun triggersAfterSustainedSpeechAboveBaseline() {
        val evaluator = VoiceVolumeWarningEvaluator(policy)

        evaluator.onClassification(SoundClassification(label = "Speech", confidence = 0.82f))

        assertEquals(
            VoiceVolumeWarningEvaluation.Waiting(
                thresholdDb = 68f,
                elapsedAboveThresholdMs = 0L,
                remainingDurationMs = 3_000L,
            ),
            evaluator.evaluate(
                weightedDb = 70f,
                timestampMs = 1_000L,
                baselineDb = 60f,
            ),
        )
        assertEquals(
            VoiceVolumeWarningEvaluation.Trigger(
                thresholdDb = 68f,
                baselineDb = 60f,
                currentDb = 70f,
                sustainedDurationMs = 3_000L,
            ),
            evaluator.evaluate(
                weightedDb = 70f,
                timestampMs = 4_000L,
                baselineDb = 60f,
            ),
        )
    }

    @Test
    fun nonSpeechClassificationSuppressesAndResetsSustainedWarningWindow() {
        val evaluator = VoiceVolumeWarningEvaluator(policy)

        evaluator.onClassification(SoundClassification(label = "Speech", confidence = 0.82f))
        evaluator.evaluate(weightedDb = 70f, timestampMs = 1_000L, baselineDb = 60f)

        evaluator.onClassification(SoundClassification(label = "Music", confidence = 0.91f))
        assertEquals(
            VoiceVolumeWarningEvaluation.NotSpeech,
            evaluator.evaluate(weightedDb = 80f, timestampMs = 3_500L, baselineDb = 60f),
        )

        evaluator.onClassification(SoundClassification(label = "Speech", confidence = 0.82f))
        assertEquals(
            VoiceVolumeWarningEvaluation.Waiting(
                thresholdDb = 68f,
                elapsedAboveThresholdMs = 0L,
                remainingDurationMs = 3_000L,
            ),
            evaluator.evaluate(weightedDb = 80f, timestampMs = 5_000L, baselineDb = 60f),
        )
    }

    @Test
    fun missingBaselineDoesNotTriggerWarning() {
        val evaluator = VoiceVolumeWarningEvaluator(policy)

        evaluator.onClassification(SoundClassification(label = "Speech", confidence = 0.82f))

        assertEquals(
            VoiceVolumeWarningEvaluation.MissingBaseline,
            evaluator.evaluate(weightedDb = 90f, timestampMs = 1_000L, baselineDb = null),
        )
    }

    @Test
    fun cooldownRequiresANewSustainedWindowBeforeNextTrigger() {
        val evaluator = VoiceVolumeWarningEvaluator(policy)

        evaluator.onClassification(SoundClassification(label = "Speech", confidence = 0.82f))
        evaluator.evaluate(weightedDb = 70f, timestampMs = 1_000L, baselineDb = 60f)
        evaluator.evaluate(weightedDb = 70f, timestampMs = 4_000L, baselineDb = 60f)

        assertEquals(
            VoiceVolumeWarningEvaluation.CoolingDown(
                thresholdDb = 68f,
                remainingCooldownMs = 30_000L,
            ),
            evaluator.evaluate(weightedDb = 70f, timestampMs = 34_000L, baselineDb = 60f),
        )
        assertEquals(
            VoiceVolumeWarningEvaluation.Waiting(
                thresholdDb = 68f,
                elapsedAboveThresholdMs = 0L,
                remainingDurationMs = 3_000L,
            ),
            evaluator.evaluate(weightedDb = 70f, timestampMs = 64_000L, baselineDb = 60f),
        )
    }
}
