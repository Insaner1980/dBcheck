package com.dbcheck.app.data.repository

import com.dbcheck.app.domain.analytics.WeightedExposureMeasurement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

class MeasurementBucketAveragesTest {
    @Test
    fun hourlyAveragesUseElapsedEnergyWithinEachHour() {
        val points = listOf(point(0, 60f), point(1_000, 70f), point(2_000, 70f),
            point(HOUR_MS, 80f, 2), point(HOUR_MS + 1_000, 80f, 2))
        val averages = MeasurementBucketAverages.hourly(points)
        assertEquals(listOf(0, 1), averages.map { it.hour })
        assertEquals(67.40363f, averages[0].avgDb, 0.0001f)
        assertEquals(70f, averages[0].maxDb, 0f)
        assertEquals(2_000L, averages[0].durationMs)
        assertEquals(80f, averages[1].avgDb, 0f)
    }

    @Test
    fun dailyAveragesUseElapsedEnergyWithinEachDay() {
        val points = listOf(point(0, 60f), point(1_000, 70f), point(2_000, 70f),
            point(DAY_MS, 80f, 2), point(DAY_MS + 1_000, 80f, 2))
        val averages = MeasurementBucketAverages.daily(points, ZoneId.of("UTC"))
        assertEquals(listOf(0L, DAY_MS), averages.map { it.dayStartMs })
        assertEquals(67.40363f, averages[0].avgDb, 0.0001f)
        assertEquals(80f, averages[1].avgDb, 0f)
    }

    @Test
    fun dailyAveragesUseLocalDayStartForSystemZone() {
        val original = TimeZone.getDefault()
        val zone = ZoneId.of("Europe/Helsinki")
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(zone))
            val midnight = LocalDate.of(2026, 5, 14).atStartOfDay(zone).toInstant().toEpochMilli()
            val average = MeasurementBucketAverages.daily(listOf(
                point(midnight + 30_000, 65f), point(midnight + 31_000, 65f),
            )).single()
            assertEquals(midnight, average.dayStartMs)
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test
    fun hourlyAveragesWeightForcedRowsByElapsedTimeWithoutExtraEndpointDuration() {
        val average = MeasurementBucketAverages.hourly(listOf(
            point(0, 60f), point(100, 90f), point(1_000, 60f),
        )).single()
        assertEquals(89.54291f, average.avgDb, 0.0001f)
        assertEquals(1_000L, average.durationMs)
    }

    @Test
    fun crossingMidnightSplitsTheSameSessionInsteadOfAssigningTheIntervalToOneBucket() {
        val midnight = Instant.parse("2026-05-15T00:00:00Z").toEpochMilli()
        val points = listOf(point(midnight - 500, 60f), point(midnight + 500, 80f), point(midnight + 1_500, 60f))
        val hourly = MeasurementBucketAverages.hourly(points)
        val daily = MeasurementBucketAverages.daily(points, ZoneId.of("UTC"))
        assertEquals(listOf(23, 0), hourly.map { it.hour })
        assertEquals(listOf(500L, 1_500L), hourly.map { it.durationMs })
        assertEquals(listOf(500L, 1_500L), daily.map { it.durationMs })
        assertEquals(60f, hourly[0].avgDb, 0f)
        assertEquals(78.26075f, hourly[1].avgDb, 0.0001f)
    }

    @Test
    fun hourlyAveragesExposeLocalHourStartAndOnlyObservedSpan() {
        val zone = ZoneId.of("Europe/Helsinki")
        val start = LocalDate.of(2026, 5, 14).atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
        val average = MeasurementBucketAverages.hourly(listOf(
            point(start, 60f), point(start + 60_000, 62f), point(start + 120_000, 64f),
        ), zone).single()
        assertEquals(start, average.hourStartMs)
        assertEquals(120_000L, average.durationMs)
    }

    @Test
    fun emptySingleAndDuplicateOnlyObservationsHaveNoDuration() {
        assertTrue(MeasurementBucketAverages.hourly(emptyList()).isEmpty())
        assertTrue(MeasurementBucketAverages.hourly(listOf(point(0, 60f))).isEmpty())
        assertTrue(MeasurementBucketAverages.hourly(listOf(point(0, 60f), point(0, 90f))).isEmpty())
        val average = MeasurementBucketAverages.hourly(listOf(point(0, 60f), point(0, 90f), point(1_000, 60f))).single()
        assertEquals(1_000L, average.durationMs)
        assertEquals(90f, average.avgDb, 0f)
    }

    @Test
    fun daylightSavingRepeatedHoursRemainSeparateAndPreserveDuration() {
        val start = Instant.parse("2026-10-25T00:30:00Z").toEpochMilli()
        val averages = MeasurementBucketAverages.hourly(listOf(point(start, 60f), point(start + HOUR_MS, 60f)),
            ZoneId.of("Europe/Helsinki"))
        assertEquals(listOf(3, 3), averages.map { it.hour })
        assertEquals(listOf(HOUR_MS / 2, HOUR_MS / 2), averages.map { it.durationMs })
        assertEquals(HOUR_MS, averages[1].hourStartMs - averages[0].hourStartMs)
    }

    @Test(timeout = 5_000L)
    fun partialHourRollbackPreservesCoverageAndTerminalMaximum() {
        val start = Instant.parse("2026-04-04T14:00:00Z").toEpochMilli()
        val averages = MeasurementBucketAverages.hourly(listOf(
            point(start, 60f), point(start + 75 * 60_000, 90f),
        ), ZoneId.of("Australia/Lord_Howe"))
        assertEquals(75 * 60_000L, averages.sumOf { it.durationMs })
        assertEquals(90f, averages.single().maxDb, 0f)
        assertEquals(60f, averages.single().avgDb, 0f)
    }

    private fun point(timestamp: Long, db: Float, sessionId: Long = 1L) =
        WeightedExposureMeasurement(timestamp, db, sessionId, "A")

    private companion object {
        const val HOUR_MS = 3_600_000L
        const val DAY_MS = 24 * HOUR_MS
    }
}
