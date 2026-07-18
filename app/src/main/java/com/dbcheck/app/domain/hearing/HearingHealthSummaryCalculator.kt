package com.dbcheck.app.domain.hearing

import com.dbcheck.app.domain.analytics.DailyExposureAverage
import com.dbcheck.app.domain.noise.DecibelMath
import com.dbcheck.app.domain.noise.NoiseLevel
import java.time.Instant
import java.time.ZoneId

data class HearingHealthSummary(
    val weeklyAverageDb: Float,
    val healthStatus: HearingHealthStatus,
    val todayVsWeekPercent: Int?,
)

enum class HearingHealthStatus { SAFE, WARNING, DANGER }

object HearingHealthSummaryCalculator {
    fun calculate(dailyAverages: List<DailyExposureAverage>, nowMs: Long, zoneId: ZoneId): HearingHealthSummary? {
        val usableDailyAverages = dailyAverages.filter { it.sampleCount > 0 }
        val totalSampleCount = usableDailyAverages.sumOf { it.sampleCount }
        val weeklyAverageDb =
            if (totalSampleCount > 0) {
                DecibelMath.energyAverageDb(
                    totalEnergy = usableDailyAverages.sumOf { dailyAverage ->
                        DecibelMath.energyFromDb(dailyAverage.avgDb) * dailyAverage.sampleCount
                    },
                    count = totalSampleCount,
                )
            } else {
                null
            }

        return weeklyAverageDb?.let { averageDb ->
            HearingHealthSummary(
                weeklyAverageDb = averageDb,
                healthStatus = healthStatusFor(averageDb),
                todayVsWeekPercent = todayVsWeekPercent(usableDailyAverages, averageDb, nowMs, zoneId),
            )
        }
    }

    private fun healthStatusFor(weeklyAverageDb: Float): HearingHealthStatus = when {
        weeklyAverageDb < NoiseLevel.NORMAL.maxDb -> HearingHealthStatus.SAFE
        weeklyAverageDb < NoiseLevel.ELEVATED.maxDb -> HearingHealthStatus.WARNING
        else -> HearingHealthStatus.DANGER
    }

    private fun todayVsWeekPercent(
        dailyAverages: List<DailyExposureAverage>,
        weeklyAverageDb: Float,
        nowMs: Long,
        zoneId: ZoneId,
    ): Int? {
        val todayAverageDb =
            dailyAverages
                .firstOrNull { dailyAverage ->
                    dailyAverage.dayStartMs == dayStartMs(nowMs, zoneId)
                }?.avgDb
                ?: return null
        return if (weeklyAverageDb > 0f) {
            ((todayAverageDb - weeklyAverageDb) / weeklyAverageDb * PERCENT_TOTAL).toInt()
        } else {
            0
        }
    }

    private fun dayStartMs(timestampMs: Long, zoneId: ZoneId): Long = Instant
        .ofEpochMilli(timestampMs)
        .atZone(zoneId)
        .toLocalDate()
        .atStartOfDay(zoneId)
        .toInstant()
        .toEpochMilli()

    private const val PERCENT_TOTAL = 100
}
