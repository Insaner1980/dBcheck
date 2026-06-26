package com.dbcheck.app

import com.dbcheck.app.domain.calibration.OctaveCalibrationOffsets
import com.dbcheck.app.domain.noise.DosimeterStandard
import com.dbcheck.app.domain.report.DbHistogramBucket
import com.dbcheck.app.domain.report.PeakEvent
import com.dbcheck.app.domain.report.ReportPoint
import com.dbcheck.app.domain.report.ReportResponseTimeSummary
import com.dbcheck.app.domain.report.ReportSleepSection
import com.dbcheck.app.domain.report.ReportSoundTypeSummary
import com.dbcheck.app.domain.report.SessionReportData
import com.dbcheck.app.domain.session.SessionAudioInputDeviceMetadata
import com.dbcheck.app.domain.session.SessionLocationMetadata

internal fun testSessionReportData(
    sessionId: Long = 7L,
    sessionName: String = "Session",
    sessionCustomName: String? = null,
    sessionEmoji: String? = null,
    sessionTags: List<String> = emptyList(),
    startTime: Long = 1_700_000_000_000L,
    endTime: Long = 1_700_000_060_000L,
    generatedAtMs: Long = endTime,
    durationMs: Long = (endTime - startTime).coerceAtLeast(0L),
    weighting: String = "A",
    equivalentLevelLabel: String = "LAeq",
    minDb: Float = 60f,
    maxDb: Float = 80f,
    laeqDb: Float = 70f,
    lcPeakDb: Float = 90f,
    twaDb: Float? = null,
    dosePercent: Float? = null,
    aWeightedExposureMetricsAvailable: Boolean = true,
    measurementCount: Int = 0,
    timeSeries: List<ReportPoint> = emptyList(),
    peakEvents: List<PeakEvent> = emptyList(),
    dbHistogramBuckets: List<DbHistogramBucket> = emptyList(),
    responseTimeSummary: ReportResponseTimeSummary = ReportResponseTimeSummary(),
    location: SessionLocationMetadata? = null,
    audioInputDevice: SessionAudioInputDeviceMetadata? = null,
    dosimeterStandard: DosimeterStandard? = null,
    projectedDosePercent: Float? = null,
    soundTypeSummary: ReportSoundTypeSummary? = null,
    sleep: ReportSleepSection? = null,
    octaveCalibrationOffsets: OctaveCalibrationOffsets = OctaveCalibrationOffsets.zero(),
): SessionReportData = SessionReportData(
    sessionId = sessionId,
    sessionName = sessionName,
    sessionCustomName = sessionCustomName,
    sessionEmoji = sessionEmoji,
    sessionTags = sessionTags,
    startTime = startTime,
    endTime = endTime,
    generatedAtMs = generatedAtMs,
    durationMs = durationMs,
    weighting = weighting,
    equivalentLevelLabel = equivalentLevelLabel,
    minDb = minDb,
    maxDb = maxDb,
    laeqDb = laeqDb,
    lcPeakDb = lcPeakDb,
    twaDb = twaDb,
    dosePercent = dosePercent,
    aWeightedExposureMetricsAvailable = aWeightedExposureMetricsAvailable,
    measurementCount = measurementCount,
    timeSeries = timeSeries,
    peakEvents = peakEvents,
    dbHistogramBuckets = dbHistogramBuckets,
    responseTimeSummary = responseTimeSummary,
    location = location,
    audioInputDevice = audioInputDevice,
    dosimeterStandard = dosimeterStandard,
    projectedDosePercent = projectedDosePercent,
    soundTypeSummary = soundTypeSummary,
    sleep = sleep,
    octaveCalibrationOffsets = octaveCalibrationOffsets,
)
