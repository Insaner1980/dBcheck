package com.dbcheck.app.domain.sleep

import com.dbcheck.app.domain.report.DbHistogramBucket
import com.dbcheck.app.domain.report.PeakEvent
import com.dbcheck.app.domain.report.ReportPoint
import com.dbcheck.app.testSessionReportData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SleepResultsCalculatorTest {
    @Test
    fun buildsSummaryFromSleepMetadataAndSessionReport() {
        val sleepSession =
            SleepSession(
                sessionId = 42L,
                targetDurationMinutes = 480,
                keepAwakeEnabled = true,
                createdAt = START_TIME,
            )
        val histogram =
            listOf(
                DbHistogramBucket(minDb = 60, maxDb = 70, sampleCount = 2, percent = 40),
                DbHistogramBucket(minDb = 80, maxDb = 90, sampleCount = 3, percent = 60),
            )
        val report =
            testSessionReportData(
                sessionId = 42L,
                durationMs = 5 * 60_000L,
                equivalentLevelLabel = "LAeq",
                laeqDb = 71.4f,
                maxDb = 91.2f,
                lcPeakDb = 110.5f,
                timeSeries =
                    listOf(
                        ReportPoint(timestamp = START_TIME, db = 60f),
                        ReportPoint(timestamp = START_TIME + 60_000L, db = 86f),
                        ReportPoint(timestamp = START_TIME + 120_000L, db = 87f),
                        ReportPoint(timestamp = START_TIME + 180_000L, db = 72f),
                        ReportPoint(timestamp = START_TIME + 240_000L, db = 90f),
                    ),
                peakEvents =
                    listOf(
                        PeakEvent(
                            startTime = START_TIME + 60_000L,
                            endTime = START_TIME + 120_000L,
                            peakTime = START_TIME + 120_000L,
                            maxDb = 87f,
                        ),
                        PeakEvent(
                            startTime = START_TIME + 240_000L,
                            endTime = START_TIME + 240_000L,
                            peakTime = START_TIME + 240_000L,
                            maxDb = 90f,
                        ),
                    ),
                dbHistogramBuckets = histogram,
            )

        val summary = SleepResultsCalculator.build(sleepSession = sleepSession, report = report)

        assertEquals(480, summary.targetDurationMinutes)
        assertEquals(5 * 60_000L, summary.recordedDurationMs)
        assertEquals("LAeq", summary.equivalentLevelLabel)
        assertEquals(71.4f, summary.equivalentLevelDb, 0.001f)
        assertEquals(91.2f, summary.maxDb, 0.001f)
        assertEquals(110.5f, summary.lcPeakDb, 0.001f)
        assertEquals(2, summary.peakEventCount)
        assertEquals(2, summary.loudPeriodCount)
        assertEquals(5, summary.sampleCount)
        assertEquals(SleepInsightsAvailability.Available, summary.insights.availability)
        assertEquals(2, summary.insights.notableEvents.size)
        assertSame(histogram, summary.histogramBuckets)
    }

    @Test
    fun missingMeasurementSeriesDoesNotReportZeroSleepInsightCounts() {
        val sleepSession =
            SleepSession(
                sessionId = 42L,
                targetDurationMinutes = 480,
                keepAwakeEnabled = false,
                createdAt = START_TIME,
            )

        val summary =
            SleepResultsCalculator.build(
                sleepSession = sleepSession,
                report = testSessionReportData(sessionId = 42L, timeSeries = emptyList(), peakEvents = emptyList()),
            )

        assertEquals(SleepInsightsAvailability.MissingMeasurements, summary.insights.availability)
        assertEquals(null, summary.peakEventCount)
        assertEquals(null, summary.loudPeriodCount)
        assertEquals(null, summary.sampleCount)
    }

    private companion object {
        const val START_TIME = 1_700_000_000_000L
    }
}
