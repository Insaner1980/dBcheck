package com.dbcheck.app.domain.analytics

import com.dbcheck.app.domain.noise.DecibelMath
import com.dbcheck.app.domain.noise.NoiseLevel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DailyExposurePoint(val dayStartMs: Long, val laeqDb: Float?, val maxDb: Float?)

data class MonthlyExposureTrend(
    val points: List<DailyExposurePoint>,
    val laeqDb: Float,
    val loudestDb: Float?,
    val measurementCount: Int,
)

data class YearlyExposureReport(
    val totalSessions: Int,
    val laeqDb: Float,
    val loudestDayStartMs: Long?,
    val loudestDb: Float?,
    val measurementCount: Int,
    val zoneDistribution: List<ExposureNoiseZonePercent>,
)

data class ExposureNoiseZonePercent(val zone: ExposureNoiseZone, val percent: Int)

data class WeightedExposureMeasurement(val timestamp: Long, val dbWeighted: Float)

data class HourlyExposureAverage(val hour: Int, val avgDb: Float, val maxDb: Float, val sampleCount: Int = 1)

data class DailyExposureAverage(val dayStartMs: Long, val avgDb: Float, val maxDb: Float, val sampleCount: Int = 1)

data class EnvironmentExposureMixCounts(
    val quietCount: Long = 0,
    val moderateCount: Long = 0,
    val loudCount: Long = 0,
    val criticalCount: Long = 0,
    val totalCount: Long = 0,
)

enum class ExposureNoiseZone { QUIET, MODERATE, LOUD, CRITICAL }

object ExposureAnalyticsCalculator {
    fun calculateLaeq(measurements: List<WeightedExposureMeasurement>): Float = DecibelMath.energyAverageDb(
        measurements.map { it.dbWeighted },
    ) ?: 0f

    fun buildMonthlyTrend(
        measurements: List<WeightedExposureMeasurement>,
        nowMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): MonthlyExposureTrend {
        val startDate = localDate(nowMs, zoneId).minusDays(MONTHLY_DAY_COUNT - 1L)
        val endMs = nowMs
        val startMs = startDate.toStartMs(zoneId)
        val includedMeasurements = measurements.inRange(startMs, endMs)
        val measurementsByDay = includedMeasurements.groupBy { measurement -> measurement.dayStartMs(zoneId) }
        val points =
            List(MONTHLY_DAY_COUNT) { index ->
                val dayStartMs = startDate.plusDays(index.toLong()).toStartMs(zoneId)
                val dayMeasurements = measurementsByDay[dayStartMs].orEmpty()
                DailyExposurePoint(
                    dayStartMs = dayStartMs,
                    laeqDb = dayMeasurements.takeIf { it.isNotEmpty() }?.let(::calculateLaeq),
                    maxDb = dayMeasurements.maxOfOrNull { it.dbWeighted },
                )
            }

        return MonthlyExposureTrend(
            points = points,
            laeqDb = calculateLaeq(includedMeasurements),
            loudestDb = includedMeasurements.maxOfOrNull { it.dbWeighted },
            measurementCount = includedMeasurements.size,
        )
    }

    fun buildYearlyReport(
        measurements: List<WeightedExposureMeasurement>,
        completedSessionCount: Int,
        nowMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): YearlyExposureReport {
        val startMs = rollingYearStartMs(nowMs, zoneId)
        val includedMeasurements = measurements.inRange(startMs, nowMs)
        val loudestMeasurement = includedMeasurements.maxByOrNull { it.dbWeighted }

        return YearlyExposureReport(
            totalSessions = completedSessionCount,
            laeqDb = calculateLaeq(includedMeasurements),
            loudestDayStartMs = loudestMeasurement?.dayStartMs(zoneId),
            loudestDb = loudestMeasurement?.dbWeighted,
            measurementCount = includedMeasurements.size,
            zoneDistribution = zoneDistribution(includedMeasurements),
        )
    }

    fun rollingMonthStartMs(nowMs: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long = localDate(nowMs, zoneId)
            .minusDays(MONTHLY_DAY_COUNT - 1L)
            .toStartMs(zoneId)

    fun rollingYearStartMs(nowMs: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long = localDate(nowMs, zoneId)
            .minusMonths(YEARLY_MONTH_COUNT)
            .toStartMs(zoneId)

    private fun zoneDistribution(measurements: List<WeightedExposureMeasurement>): List<ExposureNoiseZonePercent> {
        val zoneCounts =
            listOf(
                ExposureNoiseZone.QUIET to measurements.count { it.dbWeighted < NoiseLevel.QUIET.maxDb },
                ExposureNoiseZone.MODERATE to
                    measurements.count {
                        it.dbWeighted >= NoiseLevel.QUIET.maxDb &&
                            it.dbWeighted < NoiseLevel.NORMAL.maxDb
                    },
                ExposureNoiseZone.LOUD to
                    measurements.count {
                        it.dbWeighted >= NoiseLevel.NORMAL.maxDb &&
                            it.dbWeighted < NoiseLevel.ELEVATED.maxDb
                    },
                ExposureNoiseZone.CRITICAL to measurements.count { it.dbWeighted >= NoiseLevel.ELEVATED.maxDb },
            )
        val total = measurements.size
        if (total <= 0) {
            return zoneCounts.map { (zone, _) -> ExposureNoiseZonePercent(zone = zone, percent = 0) }
        }

        val roundedRows =
            zoneCounts.mapIndexed { index, (zone, count) ->
                val rawPercent = count * PERCENT_TOTAL / total.toDouble()
                RoundedZonePercent(
                    index = index,
                    zone = zone,
                    percent = rawPercent.toInt(),
                    remainder = rawPercent - rawPercent.toInt(),
                )
            }
        val missingPercent = PERCENT_TOTAL - roundedRows.sumOf { it.percent }
        val incrementedIndexes =
            roundedRows
                .sortedWith(compareByDescending<RoundedZonePercent> { it.remainder }.thenBy { it.index })
                .take(missingPercent)
                .map { it.index }
                .toSet()

        return roundedRows.map { row ->
            ExposureNoiseZonePercent(
                zone = row.zone,
                percent = row.percent + if (row.index in incrementedIndexes) 1 else 0,
            )
        }
    }

    private fun List<WeightedExposureMeasurement>.inRange(
        startMs: Long,
        endMs: Long,
    ): List<WeightedExposureMeasurement> =
        filter { measurement -> measurement.timestamp >= startMs && measurement.timestamp <= endMs }

    private fun localDate(timestampMs: Long, zoneId: ZoneId): LocalDate =
        Instant.ofEpochMilli(timestampMs).atZone(zoneId).toLocalDate()

    private fun LocalDate.toStartMs(zoneId: ZoneId): Long = atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

    private fun WeightedExposureMeasurement.dayStartMs(zoneId: ZoneId): Long =
        localDate(timestamp, zoneId).toStartMs(zoneId)

    private data class RoundedZonePercent(
        val index: Int,
        val zone: ExposureNoiseZone,
        val percent: Int,
        val remainder: Double,
    )

    private const val MONTHLY_DAY_COUNT = 30
    private const val YEARLY_MONTH_COUNT = 12L
    private const val PERCENT_TOTAL = 100
}
