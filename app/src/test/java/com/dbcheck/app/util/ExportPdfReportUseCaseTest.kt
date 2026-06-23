package com.dbcheck.app.util

import com.dbcheck.app.domain.audio.ResponseTime
import com.dbcheck.app.domain.calibration.OctaveCalibrationOffsets
import com.dbcheck.app.domain.noise.DosimeterStandard
import com.dbcheck.app.domain.report.ReportResponseTimeSummary
import com.dbcheck.app.domain.report.ReportSoundTypeSummary
import com.dbcheck.app.domain.report.SessionReportData
import com.dbcheck.app.domain.session.SessionLocationMetadata
import com.dbcheck.app.testStringContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportPdfReportUseCaseTest {
    @Test
    fun reportContextContentIncludesAvailableDeviceAppCalibrationAndResponseTimeFields() {
        val content =
            PdfReportContextFormatter.content(
                context = testStringContext(),
                report =
                    report(
                        responseTimeSummary =
                            ReportResponseTimeSummary(
                                responseTimes = setOf(ResponseTime.SLOW),
                            ),
                    ),
                metadata =
                    PdfReportExportMetadata(
                        deviceInfo =
                            PdfReportDeviceInfo(
                                manufacturer = "Google",
                                model = "Pixel 9 Pro",
                                androidRelease = "16",
                                sdkInt = 36,
                            ),
                        appVersionName = "2.4.0",
                        calibrationOffsetDb = 2.5f,
                    ),
            )

        assertEquals(
            listOf(
                "Device" to "Google Pixel 9 Pro - Android 16 (API 36)",
                "App version" to "2.4.0",
                "Calibration offset" to "+2.5 dB (current setting)",
                "Response time" to "Slow",
            ),
            content.rows,
        )
        assertEquals(
            "dBcheck is not a calibrated Class 1/2 sound level meter.",
            content.disclaimer,
        )
    }

    @Test
    fun reportContextContentMarksMixedOrUnavailableResponseTime() {
        val mixed =
            PdfReportContextFormatter.content(
                context = testStringContext(),
                report =
                    report(
                        responseTimeSummary =
                            ReportResponseTimeSummary(
                                responseTimes = setOf(ResponseTime.FAST, ResponseTime.IMPULSE),
                            ),
                    ),
                metadata = PdfReportExportMetadata(calibrationOffsetDb = null),
            )
        val unavailable =
            PdfReportContextFormatter.content(
                context = testStringContext(),
                report = report(responseTimeSummary = ReportResponseTimeSummary()),
                metadata = PdfReportExportMetadata(calibrationOffsetDb = null),
            )

        assertTrue(mixed.rows.contains("Response time" to "Mixed"))
        assertTrue(unavailable.rows.contains("Response time" to "N/A"))
        assertTrue(unavailable.rows.contains("Calibration offset" to "N/A"))
    }

    @Test
    fun upstreamFieldsContentUsesReadyValuesAndUnavailableFallbacks() {
        val content =
            PdfReportUpstreamFieldsFormatter.content(
                context = testStringContext(),
                report =
                    report(
                        responseTimeSummary = ReportResponseTimeSummary(),
                        location =
                            SessionLocationMetadata(
                                latitude = 60.1699,
                                longitude = 24.9384,
                                accuracyMeters = 18.5f,
                                capturedAt = 1_700_000_001_000L,
                            ),
                        dosimeterStandard = DosimeterStandard.NIOSH_REL,
                        projectedDosePercent = 200f,
                        soundTypeSummary = ReportSoundTypeSummary(label = "Speech", confidence = 0.82f),
                    ),
            )

        assertEquals(
            listOf(
                "Location" to "60.16990, 24.93840 (accuracy 18.5 m)",
                "Dosimeter standard" to "NIOSH REL",
                "Projected dose" to "200.0%",
                "Octave breakdown" to "N/A",
                "Sound type" to "Speech (82%)",
            ),
            content.rows,
        )
    }

    @Test
    fun upstreamFieldsContentCanReadReportOctaveCalibrationOffsets() {
        val content =
            PdfReportUpstreamFieldsFormatter.content(
                context = testStringContext(),
                report =
                    report(
                        responseTimeSummary = ReportResponseTimeSummary(),
                        octaveCalibrationOffsets =
                            OctaveCalibrationOffsets.zero()
                                .withOffset(centerFrequencyHz = 1_000f, offsetDb = 2f),
                    ),
            )

        assertTrue(content.rows.contains("Octave breakdown" to "Available"))
    }

    private fun report(
        responseTimeSummary: ReportResponseTimeSummary,
        location: SessionLocationMetadata? = null,
        dosimeterStandard: DosimeterStandard? = null,
        projectedDosePercent: Float? = null,
        soundTypeSummary: ReportSoundTypeSummary? = null,
        octaveCalibrationOffsets: OctaveCalibrationOffsets = OctaveCalibrationOffsets.zero(),
    ): SessionReportData = SessionReportData(
        sessionId = 7L,
        sessionName = "Session",
        sessionCustomName = null,
        sessionEmoji = null,
        sessionTags = emptyList(),
        startTime = 1_700_000_000_000L,
        endTime = 1_700_000_060_000L,
        generatedAtMs = 1_700_000_060_000L,
        durationMs = 60_000L,
        weighting = "A",
        equivalentLevelLabel = "LAeq",
        minDb = 60f,
        maxDb = 80f,
        laeqDb = 70f,
        lcPeakDb = 90f,
        twaDb = null,
        dosePercent = null,
        aWeightedExposureMetricsAvailable = true,
        measurementCount = 0,
        timeSeries = emptyList(),
        peakEvents = emptyList(),
        responseTimeSummary = responseTimeSummary,
        location = location,
        dosimeterStandard = dosimeterStandard,
        projectedDosePercent = projectedDosePercent,
        soundTypeSummary = soundTypeSummary,
        octaveCalibrationOffsets = octaveCalibrationOffsets,
    )
}
