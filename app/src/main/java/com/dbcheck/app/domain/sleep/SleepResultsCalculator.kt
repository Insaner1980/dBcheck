package com.dbcheck.app.domain.sleep

import com.dbcheck.app.domain.report.DbHistogramBucket
import com.dbcheck.app.domain.report.SessionReportData

data class SleepResultsSummary(
    val targetDurationMinutes: Int,
    val recordedDurationMs: Long,
    val keepAwakeEnabled: Boolean,
    val equivalentLevelLabel: String,
    val equivalentLevelDb: Float,
    val maxDb: Float,
    val lcPeakDb: Float,
    val peakEventCount: Int?,
    val loudPeriodCount: Int?,
    val sampleCount: Int?,
    val insights: SleepInsightsSummary,
    val histogramBuckets: List<DbHistogramBucket>,
)

object SleepResultsCalculator {
    fun build(sleepSession: SleepSession, report: SessionReportData): SleepResultsSummary {
        val insights = SleepInsightsCalculator.analyze(report)
        val measurementsAvailable = insights.availability == SleepInsightsAvailability.Available
        return SleepResultsSummary(
            targetDurationMinutes = sleepSession.targetDurationMinutes,
            recordedDurationMs = report.durationMs,
            keepAwakeEnabled = sleepSession.keepAwakeEnabled,
            equivalentLevelLabel = report.equivalentLevelLabel,
            equivalentLevelDb = report.laeqDb,
            maxDb = report.maxDb,
            lcPeakDb = report.lcPeakDb,
            peakEventCount = report.peakEvents.size.takeIf { measurementsAvailable },
            loudPeriodCount = insights.notableEvents.size.takeIf { measurementsAvailable },
            sampleCount = report.timeSeries.size.takeIf { measurementsAvailable },
            insights = insights,
            histogramBuckets = report.dbHistogramBuckets,
        )
    }
}
