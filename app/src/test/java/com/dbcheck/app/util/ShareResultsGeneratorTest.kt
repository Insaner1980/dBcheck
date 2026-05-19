package com.dbcheck.app.util

import com.dbcheck.app.domain.report.SessionReportData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareResultsGeneratorTest {
    @Test
    fun sessionStatsShareContentContainsActionTypeStatsAndFormattedDuration() {
        val content =
            buildSessionStatsShareContent(
                text = "I measured 72 dB avg (peak: 91 dB) in my 1:05 session with dBcheck",
            )

        assertEquals("android.intent.action.SEND", content.action)
        assertEquals("text/plain", content.type)
        assertTrue(content.text.contains("72 dB avg"))
        assertTrue(content.text.contains("peak: 91 dB"))
        assertTrue(content.text.contains("1:05 session"))
    }

    @Test
    fun shareTextEllipsizesToAvailableWidth() {
        val text = "Very long session name that cannot fit"

        val ellipsized =
            ellipsizeShareText(
                text = text,
                maxWidth = 16f,
                measureText = { value -> value.length.toFloat() },
            )

        assertEquals("Very long ses...", ellipsized)
        assertTrue(ellipsized.length <= 16)
    }

    @Test
    fun sessionReportShareFileNameUsesBoundedSlugAndSessionId() {
        val report =
            sessionReportData(
                sessionId = 42L,
                sessionName =
                    "Workshop main hall with a very long descriptive name " +
                        "that would otherwise produce an oversized file name",
            )

        val fileName = buildSessionReportShareFileName(report)

        assertTrue(fileName.startsWith("session_report_42_"))
        assertTrue(fileName.endsWith(".png"))
        assertTrue(fileName.length <= 74)
    }

    private fun sessionReportData(sessionId: Long = 1L, sessionName: String = "Session"): SessionReportData =
        SessionReportData(
            sessionId = sessionId,
            sessionName = sessionName,
            sessionCustomName = sessionName,
            sessionEmoji = null,
            sessionTags = emptyList(),
            startTime = 1_700_000_000_000L,
            endTime = 1_700_000_065_000L,
            generatedAtMs = 1_700_000_065_000L,
            durationMs = 65_000L,
            weighting = "A",
            minDb = 50f,
            maxDb = 80f,
            laeqDb = 72.6f,
            lcPeakDb = 91.4f,
            twaDb = 65f,
            dosePercent = 12f,
            aWeightedExposureMetricsAvailable = true,
            measurementCount = 0,
            timeSeries = emptyList(),
            peakEvents = emptyList(),
        )
}
