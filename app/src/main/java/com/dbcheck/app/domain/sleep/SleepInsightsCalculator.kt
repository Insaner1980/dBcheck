package com.dbcheck.app.domain.sleep

import com.dbcheck.app.domain.noise.NoiseLevel
import com.dbcheck.app.domain.report.ReportPoint
import com.dbcheck.app.domain.report.SessionReportData

data class SleepInsightsSummary(
    val availability: SleepInsightsAvailability,
    val notableEvents: List<SleepNotableEventSummary>,
) {
    val loudestEvent: SleepNotableEventSummary? = notableEvents.maxByOrNull { it.maxDb }
}

enum class SleepInsightsAvailability {
    Available,
    MissingMeasurements,
}

data class SleepNotableEventSummary(val startTime: Long, val endTime: Long, val maxDb: Float, val durationMs: Long)

object SleepInsightsCalculator {
    fun analyze(report: SessionReportData): SleepInsightsSummary {
        if (report.timeSeries.isEmpty()) {
            return SleepInsightsSummary(
                availability = SleepInsightsAvailability.MissingMeasurements,
                notableEvents = emptyList(),
            )
        }
        return SleepInsightsSummary(
            availability = SleepInsightsAvailability.Available,
            notableEvents = report.timeSeries.loudPeriods(),
        )
    }

    private fun List<ReportPoint>.loudPeriods(): List<SleepNotableEventSummary> {
        val periods = mutableListOf<SleepNotableEventSummary>()
        var activeStart: Long? = null
        var activeEnd = 0L
        var activeMax = 0f

        fun finishActive() {
            val start = activeStart ?: return
            periods +=
                SleepNotableEventSummary(
                    startTime = start,
                    endTime = activeEnd,
                    maxDb = activeMax,
                    durationMs = (activeEnd - start).coerceAtLeast(0L),
                )
            activeStart = null
            activeEnd = 0L
            activeMax = 0f
        }

        forEach { point ->
            val isLoud = point.db >= NoiseLevel.ELEVATED.maxDb
            if (isLoud) {
                if (activeStart == null) {
                    activeStart = point.timestamp
                    activeMax = point.db
                }
                activeEnd = point.timestamp
                activeMax = maxOf(activeMax, point.db)
            } else {
                finishActive()
            }
        }
        finishActive()
        return periods
    }
}
