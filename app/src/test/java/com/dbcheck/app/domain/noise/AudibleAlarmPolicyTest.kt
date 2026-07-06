package com.dbcheck.app.domain.noise

import org.junit.Assert.assertEquals
import org.junit.Test

class AudibleAlarmPolicyTest {
    @Test
    fun defaultPolicyOwnsThresholdDurationAndCooldown() {
        val policy = AudibleAlarmPolicy.Default

        assertEquals(90f, policy.thresholdDb, 0.001f)
        assertEquals(30_000L, policy.requiredDurationMs)
        assertEquals(5L * 60_000L, policy.cooldownMs)
    }

    @Test
    fun sustainedThresholdTriggersOnlyAfterRequiredDuration() {
        val evaluator = AudibleAlarmEvaluator()
        val policy = testPolicy()

        assertWaitingAt(evaluator, policy, timestampMs = 1_000L, elapsedMs = 0L, remainingMs = 10_000L)
        assertEquals(
            waitingEvaluation(elapsedAboveThresholdMs = 9_999L, remainingDurationMs = 1L),
            evaluator.evaluate(weightedDb = 91f, timestampMs = 10_999L, policy = policy),
        )
        assertEquals(
            AudibleAlarmEvaluation.Trigger(thresholdDb = 90f, sustainedDurationMs = 10_000L),
            evaluator.evaluate(weightedDb = 91f, timestampMs = 11_000L, policy = policy),
        )
    }

    @Test
    fun belowThresholdResetsRequiredDurationWindow() {
        val evaluator = AudibleAlarmEvaluator()
        val policy = testPolicy()

        assertWaitingAt(evaluator, policy, timestampMs = 1_000L, elapsedMs = 0L, remainingMs = 10_000L)
        assertEquals(
            AudibleAlarmEvaluation.BelowThreshold(thresholdDb = 90f),
            evaluator.evaluate(weightedDb = 89.9f, timestampMs = 5_000L, policy = policy),
        )
        assertEquals(
            waitingEvaluation(elapsedAboveThresholdMs = 0L, remainingDurationMs = 10_000L),
            evaluator.evaluate(weightedDb = 91f, timestampMs = 9_000L, policy = policy),
        )
        assertEquals(
            waitingEvaluation(elapsedAboveThresholdMs = 9_999L, remainingDurationMs = 1L),
            evaluator.evaluate(weightedDb = 91f, timestampMs = 18_999L, policy = policy),
        )
        assertEquals(
            AudibleAlarmEvaluation.Trigger(thresholdDb = 90f, sustainedDurationMs = 10_000L),
            evaluator.evaluate(weightedDb = 91f, timestampMs = 19_000L, policy = policy),
        )
    }

    @Test
    fun cooldownSuppressesRepeatedTriggersAndRequiresANewDurationWindow() {
        val evaluator = AudibleAlarmEvaluator()
        val policy = testPolicy()

        assertWaitingAt(evaluator, policy, timestampMs = 1_000L, elapsedMs = 0L, remainingMs = 10_000L)
        assertEquals(
            AudibleAlarmEvaluation.Trigger(thresholdDb = 90f, sustainedDurationMs = 10_000L),
            evaluator.evaluate(weightedDb = 91f, timestampMs = 11_000L, policy = policy),
        )
        assertEquals(
            AudibleAlarmEvaluation.CoolingDown(thresholdDb = 90f, remainingCooldownMs = 50_000L),
            evaluator.evaluate(weightedDb = 91f, timestampMs = 21_000L, policy = policy),
        )
        assertEquals(
            waitingEvaluation(elapsedAboveThresholdMs = 0L, remainingDurationMs = 10_000L),
            evaluator.evaluate(weightedDb = 91f, timestampMs = 71_000L, policy = policy),
        )
        assertEquals(
            AudibleAlarmEvaluation.Trigger(thresholdDb = 90f, sustainedDurationMs = 10_000L),
            evaluator.evaluate(weightedDb = 91f, timestampMs = 81_000L, policy = policy),
        )
    }

    @Test
    fun resetClearsWindowAndCooldown() {
        val evaluator = AudibleAlarmEvaluator()
        val policy = testPolicy()

        evaluator.evaluate(weightedDb = 91f, timestampMs = 1_000L, policy = policy)
        evaluator.evaluate(weightedDb = 91f, timestampMs = 11_000L, policy = policy)
        evaluator.reset()

        assertEquals(
            waitingEvaluation(elapsedAboveThresholdMs = 0L, remainingDurationMs = 10_000L),
            evaluator.evaluate(weightedDb = 91f, timestampMs = 12_000L, policy = policy),
        )
    }

    private fun testPolicy(): AudibleAlarmPolicy =
        AudibleAlarmPolicy(thresholdDb = 90f, requiredDurationMs = 10_000L, cooldownMs = 60_000L)

    private fun assertWaitingAt(
        evaluator: AudibleAlarmEvaluator,
        policy: AudibleAlarmPolicy,
        timestampMs: Long,
        elapsedMs: Long,
        remainingMs: Long,
    ) {
        assertEquals(
            waitingEvaluation(elapsedAboveThresholdMs = elapsedMs, remainingDurationMs = remainingMs),
            evaluator.evaluate(weightedDb = 91f, timestampMs = timestampMs, policy = policy),
        )
    }

    private fun waitingEvaluation(elapsedAboveThresholdMs: Long, remainingDurationMs: Long): AudibleAlarmEvaluation =
        AudibleAlarmEvaluation.Waiting(
            thresholdDb = 90f,
            elapsedAboveThresholdMs = elapsedAboveThresholdMs,
            remainingDurationMs = remainingDurationMs,
        )
}
