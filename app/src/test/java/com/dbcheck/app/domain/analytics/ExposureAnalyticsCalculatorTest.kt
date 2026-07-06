package com.dbcheck.app.domain.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.pow

class ExposureAnalyticsCalculatorTest {
    private val zoneId: ZoneId = ZoneId.of("Europe/Helsinki")
    private val nowMs: Long =
        LocalDate
            .of(2026, 5, 9)
            .atTime(12, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

    @Test
    fun calculatesLaeqUsingEnergyAverage() {
        val laeq =
            ExposureAnalyticsCalculator.calculateLaeq(
                listOf(
                    point("2026-05-09", 60f),
                    point("2026-05-09", 70f),
                ),
            )

        val expected = 10.0 * kotlin.math.log10((10.0.pow(6.0) + 10.0.pow(7.0)) / 2.0)
        assertEquals(expected.toFloat(), laeq, 0.001f)
    }

    @Test
    fun monthlyTrendUsesRollingThirtyLocalDaysAndLeavesEmptyDaysNull() {
        val oldPoint = point("2026-04-09", 90f)
        val firstIncludedDay = point("2026-04-10", 60f)
        val todayFirstSample = point("2026-05-09", 70f)
        val todaySecondSample = point("2026-05-09", 80f)

        val trend =
            ExposureAnalyticsCalculator.buildMonthlyTrend(
                measurements =
                    listOf(
                        oldPoint,
                        firstIncludedDay,
                        todayFirstSample,
                        todaySecondSample,
                    ),
                nowMs = nowMs,
                zoneId = zoneId,
            )

        assertEquals(30, trend.points.size)
        assertEquals(dayStart("2026-04-10"), trend.points.first().dayStartMs)
        assertEquals(dayStart("2026-05-09"), trend.points.last().dayStartMs)
        assertEquals(60f, trend.points.first().laeqDb ?: 0f, 0.001f)
        assertNull(trend.points[1].laeqDb)
        val expectedTodayLaeq = 10.0 * kotlin.math.log10((10.0.pow(7.0) + 10.0.pow(8.0)) / 2.0)
        assertEquals(expectedTodayLaeq, trend.points.last().laeqDb?.toDouble() ?: 0.0, 0.001)
        assertEquals(80f, trend.loudestDb ?: 0f, 0.001f)
    }

    @Test
    fun yearlyReportUsesRollingTwelveMonthsAndCompletedSessionCount() {
        val report =
            ExposureAnalyticsCalculator.buildYearlyReport(
                measurements =
                    listOf(
                        point("2025-05-08", 120f),
                        point("2025-05-09", 62f),
                        point("2026-01-15", 88f),
                        point("2026-05-09", 72f),
                    ),
                completedSessionCount = 42,
                nowMs = nowMs,
                zoneId = zoneId,
            )

        assertEquals(42, report.totalSessions)
        assertEquals(88f, report.loudestDb ?: 0f, 0.001f)
        assertEquals(dayStart("2026-01-15"), report.loudestDayStartMs)
        assertEquals(3, report.measurementCount)
    }

    @Test
    fun noiseZoneDistributionPercentagesSumToOneHundred() {
        val report =
            ExposureAnalyticsCalculator.buildYearlyReport(
                measurements =
                    listOf(
                        point("2026-01-01", 35f),
                        point("2026-01-02", 55f),
                        point("2026-01-03", 75f),
                    ),
                completedSessionCount = 3,
                nowMs = nowMs,
                zoneId = zoneId,
            )

        assertEquals(100, report.zoneDistribution.sumOf { it.percent })
        assertEquals(
            listOf(
                ExposureNoiseZone.QUIET to 34,
                ExposureNoiseZone.MODERATE to 33,
                ExposureNoiseZone.LOUD to 33,
                ExposureNoiseZone.CRITICAL to 0,
            ),
            report.zoneDistribution.map { it.zone to it.percent },
        )
    }

    @Test
    fun environmentMixCountsUseNoiseLevelBoundaries() {
        val counts =
            ExposureAnalyticsCalculator.buildEnvironmentMixCounts(
                listOf(39.9f, 40f, 69.9f, 70f, 84.9f, 85f),
            )

        assertEquals(
            EnvironmentExposureMixCounts(
                quietCount = 1,
                moderateCount = 2,
                loudCount = 2,
                criticalCount = 1,
                totalCount = 6,
            ),
            counts,
        )
    }

    @Test
    fun environmentMixPercentagesRoundToOneHundredInStableCategoryOrder() {
        val percentages =
            ExposureAnalyticsCalculator.environmentMixPercentages(
                EnvironmentExposureMixCounts(
                    quietCount = 1,
                    moderateCount = 1,
                    loudCount = 1,
                    criticalCount = 0,
                    totalCount = 3,
                ),
            )

        assertEquals(
            listOf(
                ExposureNoiseZone.QUIET to 34,
                ExposureNoiseZone.MODERATE to 33,
                ExposureNoiseZone.LOUD to 33,
                ExposureNoiseZone.CRITICAL to 0,
            ),
            percentages.map { it.zone to it.percent },
        )
    }

    private fun point(date: String, db: Float): WeightedExposureMeasurement = WeightedExposureMeasurement(
            timestamp =
                LocalDate
                    .parse(date)
                    .atTime(12, 0)
                    .atZone(zoneId)
                    .toInstant()
                    .toEpochMilli(),
            dbWeighted = db,
        )

    private fun dayStart(date: String): Long = LocalDate
            .parse(date)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
}
