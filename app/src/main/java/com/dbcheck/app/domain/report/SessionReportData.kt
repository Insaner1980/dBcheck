package com.dbcheck.app.domain.report

import com.dbcheck.app.domain.audio.ResponseTime
import com.dbcheck.app.domain.calibration.OctaveCalibrationOffsets
import com.dbcheck.app.domain.noise.DosimeterStandard
import com.dbcheck.app.domain.session.SessionAudioInputDeviceMetadata
import com.dbcheck.app.domain.session.SessionLocationMetadata
import com.dbcheck.app.domain.session.SessionTimeZoneOffsets

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
    val timeZoneOffsets: SessionTimeZoneOffsets = SessionTimeZoneOffsets(),
    val weighting: String,
    val equivalentLevelLabel: String,
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
    val dbHistogramBuckets: List<DbHistogramBucket> = emptyList(),
    val responseTimeSummary: ReportResponseTimeSummary = ReportResponseTimeSummary(),
    val location: SessionLocationMetadata? = null,
    val audioInputDevice: SessionAudioInputDeviceMetadata? = null,
    val dosimeterStandard: DosimeterStandard? = null,
    val projectedDosePercent: Float? = null,
    val soundTypeSummary: ReportSoundTypeSummary? = null,
    val sleep: ReportSleepSection? = null,
    val octaveCalibrationOffsets: OctaveCalibrationOffsets = OctaveCalibrationOffsets.zero(),
    val octaveBreakdownAvailable: Boolean = false,
)

data class ReportPoint(val timestamp: Long, val db: Float)

data class ReportMeasurement(
    val timestamp: Long,
    val dbWeighted: Float,
    val peakDb: Float = dbWeighted,
    val responseTime: String = ResponseTime.FAST.name,
)

data class ReportResponseTimeSummary(val responseTimes: Set<ResponseTime> = emptySet()) {
    val isMixed: Boolean = responseTimes.size > 1

    fun singleOrNull(): ResponseTime? = responseTimes.singleOrNull()
}

data class ReportSoundEvent(val timestamp: Long, val label: String, val confidence: Float)

data class ReportSoundTypeSummary(val label: String, val confidence: Float)

data class ReportSleepSection(
    val targetDurationMinutes: Int,
    val recordedDurationMs: Long,
    val keepAwakeEnabled: Boolean,
    val peakEventCount: Int?,
    val loudPeriodCount: Int?,
)

data class ReportHeartRateSection(val enabled: Boolean = false, val samples: List<ReportHeartRateSample> = emptyList())

data class ReportHeartRateSample(val timestamp: Long, val beatsPerMinute: Long)

data class PeakEvent(val startTime: Long, val endTime: Long, val peakTime: Long, val maxDb: Float)
