package com.dbcheck.app.service

import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.domain.audio.DecibelReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoiseAlertEvaluatorTest {
    @Test
    fun exposureAlertTriggersOnceAfterConfiguredThresholdForThirtyMinutes() {
        val evaluator = NoiseAlertEvaluator()
        evaluator.reset(sessionStartTimeMs = 1_000L)

        assertTrue(
            evaluator.evaluate(
                reading = reading(timestamp = 30.minutesAfterStart()),
                stats = SessionStats(avgDb = 84.9f),
                preferences = UserPreferences(exposureAlertsEnabled = true, notificationThreshold = 85),
            ).isEmpty(),
        )

        val exposureDecision =
            listOf(NoiseAlertDecision.Exposure(avgDb = 85f, durationMinutes = 30))
        assertEquals(
            exposureDecision,
            evaluator.evaluate(
                reading = reading(timestamp = 30.minutesAfterStart(), weightedDb = 85f),
                stats = SessionStats(avgDb = 85f),
                preferences = UserPreferences(exposureAlertsEnabled = true, notificationThreshold = 85),
            ),
        )
        evaluator.markDelivered(exposureDecision.single())

        assertTrue(
            evaluator.evaluate(
                reading = reading(timestamp = 31.minutesAfterStart(), weightedDb = 90f),
                stats = SessionStats(avgDb = 90f),
                preferences = UserPreferences(exposureAlertsEnabled = true, notificationThreshold = 85),
            ).isEmpty(),
        )
    }

    @Test
    fun exposureAlertDoesNotTriggerBeforeThirtyMinutes() {
        val evaluator = NoiseAlertEvaluator()
        evaluator.reset(sessionStartTimeMs = 1_000L)

        assertTrue(
            evaluator.evaluate(
                reading = reading(timestamp = 29.minutesAfterStart(), weightedDb = 95f),
                stats = SessionStats(avgDb = 95f),
                preferences = UserPreferences(exposureAlertsEnabled = true, notificationThreshold = 85),
            ).isEmpty(),
        )
    }

    @Test
    fun exposureAlertRetriesUntilDeliveryIsConfirmed() {
        val evaluator = NoiseAlertEvaluator()
        evaluator.reset(sessionStartTimeMs = 1_000L)

        assertEquals(
            listOf(NoiseAlertDecision.Exposure(avgDb = 85f, durationMinutes = 30)),
            evaluator.evaluate(
                reading = reading(timestamp = 30.minutesAfterStart(), weightedDb = 85f),
                stats = SessionStats(avgDb = 85f),
                preferences = UserPreferences(exposureAlertsEnabled = true, notificationThreshold = 85),
            ),
        )

        val retriedDecision =
            listOf(NoiseAlertDecision.Exposure(avgDb = 86f, durationMinutes = 31))
        assertEquals(
            retriedDecision,
            evaluator.evaluate(
                reading = reading(timestamp = 31.minutesAfterStart(), weightedDb = 86f),
                stats = SessionStats(avgDb = 86f),
                preferences = UserPreferences(exposureAlertsEnabled = true, notificationThreshold = 85),
            ),
        )
        evaluator.markDelivered(retriedDecision.single())

        assertTrue(
            evaluator.evaluate(
                reading = reading(timestamp = 32.minutesAfterStart(), weightedDb = 90f),
                stats = SessionStats(avgDb = 90f),
                preferences = UserPreferences(exposureAlertsEnabled = true, notificationThreshold = 85),
            ).isEmpty(),
        )
    }

    @Test
    fun peakWarningTriggersOnceAfterDeliveryIsConfirmed() {
        val evaluator = NoiseAlertEvaluator()
        evaluator.reset(sessionStartTimeMs = 1_000L)

        val peakDecision =
            listOf(NoiseAlertDecision.Peak(peakDb = 120f))
        assertEquals(
            peakDecision,
            evaluator.evaluate(
                reading = reading(timestamp = 2_000L, instantDb = 120f),
                stats = SessionStats(),
                preferences = UserPreferences(peakWarningsEnabled = true),
            ),
        )
        evaluator.markDelivered(peakDecision.single())

        assertTrue(
            evaluator.evaluate(
                reading = reading(timestamp = 3_000L, instantDb = 125f),
                stats = SessionStats(),
                preferences = UserPreferences(peakWarningsEnabled = true),
            ).isEmpty(),
        )
    }

    @Test
    fun peakWarningRetriesUntilDeliveryIsConfirmed() {
        val evaluator = NoiseAlertEvaluator()
        evaluator.reset(sessionStartTimeMs = 1_000L)

        assertEquals(
            listOf(NoiseAlertDecision.Peak(peakDb = 120f)),
            evaluator.evaluate(
                reading = reading(timestamp = 2_000L, peakDb = 120f),
                stats = SessionStats(),
                preferences = UserPreferences(peakWarningsEnabled = true),
            ),
        )

        val retriedDecision = listOf(NoiseAlertDecision.Peak(peakDb = 125f))
        assertEquals(
            retriedDecision,
            evaluator.evaluate(
                reading = reading(timestamp = 3_000L, peakDb = 125f),
                stats = SessionStats(),
                preferences = UserPreferences(peakWarningsEnabled = true),
            ),
        )
        evaluator.markDelivered(retriedDecision.single())

        assertTrue(
            evaluator.evaluate(
                reading = reading(timestamp = 4_000L, peakDb = 130f),
                stats = SessionStats(),
                preferences = UserPreferences(peakWarningsEnabled = true),
            ).isEmpty(),
        )
    }

    @Test
    fun disabledPreferencesSuppressAlerts() {
        val evaluator = NoiseAlertEvaluator()
        evaluator.reset(sessionStartTimeMs = 1_000L)

        assertTrue(
            evaluator.evaluate(
                reading = reading(timestamp = 30.minutesAfterStart(), instantDb = 125f, weightedDb = 95f),
                stats = SessionStats(avgDb = 95f),
                preferences =
                    UserPreferences(
                        exposureAlertsEnabled = false,
                        peakWarningsEnabled = false,
                        notificationThreshold = 85,
                    ),
            ).isEmpty(),
        )
    }

    private fun Int.minutesAfterStart(): Long = 1_000L + this * 60_000L

    private fun reading(
        timestamp: Long,
        instantDb: Float = 80f,
        weightedDb: Float = instantDb,
        peakDb: Float = instantDb,
    ) = DecibelReading(
        instantDb = instantDb,
        weightedDb = weightedDb,
        timestamp = timestamp,
        peakAmplitude = 0.5f,
        peakDb = peakDb,
    )
}
