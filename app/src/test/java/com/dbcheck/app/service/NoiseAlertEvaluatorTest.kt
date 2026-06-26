package com.dbcheck.app.service

import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.domain.audio.DecibelReading
import com.dbcheck.app.domain.noise.DosimeterStandard
import com.dbcheck.app.domain.noise.NoiseNotificationSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

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
            listOf(
                NoiseAlertDecision.Exposure(
                    avgDb = 85f,
                    durationMinutes = 30,
                    trigger = NoiseExposureAlertTrigger.AVERAGE_DURATION,
                ),
            )
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
    fun exposureAlertRetriesAfterCooldownUntilDeliveryIsConfirmed() {
        val evaluator = NoiseAlertEvaluator()
        evaluator.reset(sessionStartTimeMs = 1_000L)

        val firstDecision = listOf(
            NoiseAlertDecision.Exposure(
                avgDb = 85f,
                durationMinutes = 30,
                trigger = NoiseExposureAlertTrigger.AVERAGE_DURATION,
            ),
        )
        assertEquals(
            firstDecision,
            evaluator.evaluate(
                reading = reading(timestamp = 30.minutesAfterStart(), weightedDb = 85f),
                stats = SessionStats(avgDb = 85f),
                preferences = UserPreferences(exposureAlertsEnabled = true, notificationThreshold = 85),
            ),
        )

        assertTrue(
            evaluator.evaluate(
                reading = reading(timestamp = 31.minutesAfterStart(), weightedDb = 86f),
                stats = SessionStats(avgDb = 86f),
                preferences = UserPreferences(exposureAlertsEnabled = true, notificationThreshold = 85),
            ).isEmpty(),
        )
        val retriedDecision =
            listOf(
                NoiseAlertDecision.Exposure(
                    avgDb = 86f,
                    durationMinutes = 60,
                    trigger = NoiseExposureAlertTrigger.AVERAGE_DURATION,
                ),
            )
        assertEquals(
            retriedDecision,
            evaluator.evaluate(
                reading = reading(timestamp = 60.minutesAfterStart(), weightedDb = 86f),
                stats = SessionStats(avgDb = 86f),
                preferences = UserPreferences(exposureAlertsEnabled = true, notificationThreshold = 85),
            ),
        )
        evaluator.markDelivered(retriedDecision.single())

        assertTrue(
            evaluator.evaluate(
                reading = reading(timestamp = 90.minutesAfterStart(), weightedDb = 90f),
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
    fun peakWarningRetriesAfterCooldownUntilDeliveryIsConfirmed() {
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

        assertTrue(
            evaluator.evaluate(
                reading = reading(timestamp = 3_000L, peakDb = 125f),
                stats = SessionStats(),
                preferences = UserPreferences(peakWarningsEnabled = true),
            ).isEmpty(),
        )
        val retriedDecision = listOf(NoiseAlertDecision.Peak(peakDb = 125f))
        assertEquals(
            retriedDecision,
            evaluator.evaluate(
                reading = reading(timestamp = 2_000L + 30L * 60_000L, peakDb = 125f),
                stats = SessionStats(),
                preferences = UserPreferences(peakWarningsEnabled = true),
            ),
        )
        evaluator.markDelivered(retriedDecision.single())

        assertTrue(
            evaluator.evaluate(
                reading = reading(timestamp = 2_000L + 60L * 60_000L, peakDb = 130f),
                stats = SessionStats(),
                preferences = UserPreferences(peakWarningsEnabled = true),
            ).isEmpty(),
        )
    }

    @Test
    fun projectedDoseAlertTriggersBeforeAverageDurationWindow() {
        val evaluator = NoiseAlertEvaluator()
        evaluator.reset(sessionStartTimeMs = 1_000L)

        assertEquals(
            listOf(
                NoiseAlertDecision.Exposure(
                    avgDb = 82f,
                    durationMinutes = 5,
                    trigger = NoiseExposureAlertTrigger.PROJECTED_DOSE,
                ),
            ),
            evaluator.evaluate(
                reading = reading(timestamp = 5.minutesAfterStart(), weightedDb = 82f),
                stats = SessionStats(avgDb = 82f),
                liveExposure =
                    LiveExposureState(
                        standard = DosimeterStandard.NIOSH_REL,
                        laeqDb = 82f,
                        durationMs = 5L * 60_000L,
                        dosePercent = 2f,
                        projectedDosePercent = 100f,
                        sampleCount = 8,
                    ),
                preferences = UserPreferences(exposureAlertsEnabled = true, notificationThreshold = 85),
            ),
        )
    }

    @Test
    fun doseAlertTriggersWhenActualDoseReachesLimit() {
        val evaluator = NoiseAlertEvaluator()
        evaluator.reset(sessionStartTimeMs = 1_000L)

        assertEquals(
            listOf(
                NoiseAlertDecision.Exposure(
                    avgDb = 88f,
                    durationMinutes = 20,
                    trigger = NoiseExposureAlertTrigger.DOSE,
                ),
            ),
            evaluator.evaluate(
                reading = reading(timestamp = 20.minutesAfterStart(), weightedDb = 88f),
                stats = SessionStats(avgDb = 88f),
                liveExposure =
                    LiveExposureState(
                        standard = DosimeterStandard.NIOSH_REL,
                        laeqDb = 88f,
                        durationMs = 20L * 60_000L,
                        dosePercent = 100f,
                        projectedDosePercent = 100f,
                        sampleCount = 32,
                    ),
                preferences = UserPreferences(exposureAlertsEnabled = true, notificationThreshold = 95),
            ),
        )
    }

    @Test
    fun notificationScheduleSuppressesExposureAndPeakAlertsOutsideActiveWindow() {
        val evaluator = NoiseAlertEvaluator(zoneId = ZoneId.of("UTC"))
        val start = Instant.parse("2026-06-22T08:00:00Z").toEpochMilli()
        val outsideSchedule = Instant.parse("2026-06-22T18:00:00Z").toEpochMilli()
        evaluator.reset(sessionStartTimeMs = start)

        assertTrue(
            evaluator.evaluate(
                reading = reading(timestamp = outsideSchedule, peakDb = 125f, weightedDb = 95f),
                stats = SessionStats(avgDb = 95f),
                liveExposure =
                    LiveExposureState(
                        dosePercent = 100f,
                        projectedDosePercent = 100f,
                        sampleCount = 12,
                    ),
                preferences =
                    UserPreferences(
                        exposureAlertsEnabled = true,
                        peakWarningsEnabled = true,
                        notificationThreshold = 85,
                        notificationSchedule =
                            NoiseNotificationSchedule(
                                activeDays = setOf(DayOfWeek.MONDAY),
                                startMinuteOfDay = 9 * 60,
                                endMinuteOfDay = 17 * 60,
                            ),
                    ),
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
