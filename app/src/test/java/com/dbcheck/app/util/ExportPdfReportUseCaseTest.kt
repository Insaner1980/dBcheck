package com.dbcheck.app.util

import com.dbcheck.app.domain.audio.ResponseTime
import com.dbcheck.app.domain.calibration.OctaveCalibrationOffsets
import com.dbcheck.app.domain.noise.DosimeterStandard
import com.dbcheck.app.domain.report.ReportResponseTimeSummary
import com.dbcheck.app.domain.report.ReportSleepSection
import com.dbcheck.app.domain.report.ReportSoundTypeSummary
import com.dbcheck.app.domain.session.SessionAudioInputDeviceMetadata
import com.dbcheck.app.domain.session.SessionLocationMetadata
import com.dbcheck.app.testSessionReportData
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
                    testSessionReportData(
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
                "Audio input" to "N/A",
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
                    testSessionReportData(
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
                report = testSessionReportData(responseTimeSummary = ReportResponseTimeSummary()),
                metadata = PdfReportExportMetadata(calibrationOffsetDb = null),
            )

        assertTrue(mixed.rows.contains("Response time" to "Mixed"))
        assertTrue(unavailable.rows.contains("Response time" to "N/A"))
        assertTrue(unavailable.rows.contains("Calibration offset" to "N/A"))
    }

    @Test
    fun reportContextContentIncludesRoutedAudioInputDeviceWhenAvailable() {
        val content =
            PdfReportContextFormatter.content(
                context = testStringContext(),
                report =
                    testSessionReportData(
                        audioInputDevice =
                            SessionAudioInputDeviceMetadata(
                                selectedDeviceId = 12,
                                selectedDeviceName = "USB-C microphone",
                                routedDeviceName = "USB-C microphone",
                            ),
                    ),
                metadata = PdfReportExportMetadata(calibrationOffsetDb = null),
            )

        assertTrue(content.rows.contains("Audio input" to "USB-C microphone"))
    }

    @Test
    fun upstreamFieldsContentUsesReadyValuesAndUnavailableFallbacks() {
        val content =
            PdfReportUpstreamFieldsFormatter.content(
                context = testStringContext(),
                report =
                    testSessionReportData(
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
                "Sleep target" to "N/A",
                "Sleep recorded" to "N/A",
                "Sleep keep awake" to "N/A",
                "Sleep loud periods" to "N/A",
                "Sleep peak events" to "N/A",
            ),
            content.rows,
        )
    }

    @Test
    fun upstreamFieldsContentIncludesSleepReportFieldsWhenAvailable() {
        val content =
            PdfReportUpstreamFieldsFormatter.content(
                context = testStringContext(),
                report =
                    testSessionReportData(
                        sleep =
                            ReportSleepSection(
                                targetDurationMinutes = 480,
                                recordedDurationMs = 5 * 60_000L,
                                keepAwakeEnabled = true,
                                loudPeriodCount = 2,
                                peakEventCount = 1,
                            ),
                    ),
            )

        assertTrue(content.rows.contains("Sleep target" to "8:00:00"))
        assertTrue(content.rows.contains("Sleep recorded" to "5:00"))
        assertTrue(content.rows.contains("Sleep keep awake" to "Enabled"))
        assertTrue(content.rows.contains("Sleep loud periods" to "2"))
        assertTrue(content.rows.contains("Sleep peak events" to "1"))
    }

    @Test
    fun upstreamFieldsContentCanReadReportOctaveCalibrationOffsets() {
        val content =
            PdfReportUpstreamFieldsFormatter.content(
                context = testStringContext(),
                report =
                    testSessionReportData(
                        responseTimeSummary = ReportResponseTimeSummary(),
                        octaveCalibrationOffsets =
                            OctaveCalibrationOffsets.zero()
                                .withOffset(centerFrequencyHz = 1_000f, offsetDb = 2f),
                    ),
            )

        assertTrue(content.rows.contains("Octave breakdown" to "Available"))
    }
}
