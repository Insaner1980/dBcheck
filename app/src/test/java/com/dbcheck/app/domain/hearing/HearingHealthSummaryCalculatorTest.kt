package com.dbcheck.app.domain.hearing

import com.dbcheck.app.domain.analytics.DailyExposureAverage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HearingHealthSummaryCalculatorTest {
    private val zoneId = ZoneId.of("UTC")
    private val nowMs = Instant.parse("2026-07-18T12:00:00Z").toEpochMilli()

    @Test
    fun calculateReturnsNullWhenThereAreNoUsableSamples() {
        val summary =
            HearingHealthSummaryCalculator.calculate(
                dailyAverages = listOf(dailyAverage(dayOffset = 0, avgDb = 60f, sampleCount = 0)),
                nowMs = nowMs,
                zoneId = zoneId,
            )

        assertNull(summary)
    }

    @Test
    fun calculateUsesSampleCountsToWeightWeeklyEnergyAverage() {
        val summary =
            HearingHealthSummaryCalculator.calculate(
                dailyAverages =
                    listOf(
                        dailyAverage(dayOffset = -1, avgDb = 60f, sampleCount = 1),
                        dailyAverage(dayOffset = 0, avgDb = 70f, sampleCount = 9),
                    ),
                nowMs = nowMs,
                zoneId = zoneId,
            )

        requireNotNull(summary)
        assertEquals(69.59041f, summary.weeklyAverageDb, 0.0001f)
    }

    @Test
    fun calculateClassifiesAverageBelowNormalMaximumAsSafe() {
        val summary = calculateForAverage(69.999f)

        assertEquals(HearingHealthStatus.SAFE, summary.healthStatus)
    }

    @Test
    fun calculateClassifiesNormalMaximumAsWarning() {
        val summary = calculateForAverage(70f)

        assertEquals(HearingHealthStatus.WARNING, summary.healthStatus)
    }

    @Test
    fun calculateClassifiesElevatedMaximumAsDanger() {
        val summary = calculateForAverage(85f)

        assertEquals(HearingHealthStatus.DANGER, summary.healthStatus)
    }

    @Test
    fun calculateComparesCurrentDayWithWeeklyAverageUsingTruncation() {
        val summary =
            HearingHealthSummaryCalculator.calculate(
                dailyAverages =
                    listOf(
                        dailyAverage(dayOffset = -1, avgDb = 60f),
                        dailyAverage(dayOffset = 0, avgDb = 70f),
                    ),
                nowMs = nowMs,
                zoneId = zoneId,
            )

        requireNotNull(summary)
        assertEquals(3, summary.todayVsWeekPercent)
    }

    @Test
    fun calculateReturnsZeroTodayComparisonWhenThereIsNoCurrentDayAverage() {
        val summary =
            HearingHealthSummaryCalculator.calculate(
                dailyAverages = listOf(dailyAverage(dayOffset = -1, avgDb = 80f)),
                nowMs = nowMs,
                zoneId = zoneId,
            )

        requireNotNull(summary)
        assertEquals(0, summary.todayVsWeekPercent)
    }

    private fun calculateForAverage(averageDb: Float): HearingHealthSummary = requireNotNull(
        HearingHealthSummaryCalculator.calculate(
            dailyAverages = listOf(dailyAverage(dayOffset = 0, avgDb = averageDb)),
            nowMs = nowMs,
            zoneId = zoneId,
        ),
    )

    private fun dailyAverage(dayOffset: Long, avgDb: Float, sampleCount: Int = 1): DailyExposureAverage =
        DailyExposureAverage(
            dayStartMs = LocalDate.of(2026, 7, 18).plusDays(dayOffset).atStartOfDay(zoneId).toInstant().toEpochMilli(),
            avgDb = avgDb,
            maxDb = avgDb,
            sampleCount = sampleCount,
        )
}
