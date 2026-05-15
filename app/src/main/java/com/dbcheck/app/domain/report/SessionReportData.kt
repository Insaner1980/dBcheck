package com.dbcheck.app.domain.report

data class SessionReportData(
    val sessionId: Long,
    val sessionName: String,
    val sessionCustomName: String?,
    val sessionEmoji: String?,
    val sessionTags: List<String>,
    val startTime: Long,
    val endTime: Long,
    val generatedAtMs: Long,
    val durationMs: Long,
    val weighting: String,
    val minDb: Float,
    val maxDb: Float,
    val laeqDb: Float,
    val lcPeakDb: Float,
    val twaDb: Float?,
    val dosePercent: Float?,
    val aWeightedExposureMetricsAvailable: Boolean,
    val measurementCount: Int,
    val timeSeries: List<ReportPoint>,
    val peakEvents: List<PeakEvent>,
)

data class ReportPoint(
    val timestamp: Long,
    val db: Float,
)

data class ReportMeasurement(
    val timestamp: Long,
    val dbWeighted: Float,
    val peakDb: Float = dbWeighted,
)

data class ReportHeartRateSection(val enabled: Boolean = false, val samples: List<ReportHeartRateSample> = emptyList())

data class ReportHeartRateSample(val timestamp: Long, val beatsPerMinute: Long)

data class PeakEvent(
    val startTime: Long,
    val endTime: Long,
    val peakTime: Long,
    val maxDb: Float,
)
