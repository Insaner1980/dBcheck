package com.dbcheck.app.domain.sleep

import com.dbcheck.app.domain.report.ReportPoint
import com.dbcheck.app.testSessionReportData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SleepInsightsCalculatorTest {
    @Test
    fun analyzesLoudPeriodsAsNotableEvents() {
        val report =
            testSessionReportData(
                timeSeries =
                    listOf(
                        ReportPoint(timestamp = START_TIME, db = 60f),
                        ReportPoint(timestamp = START_TIME + 60_000L, db = 86f),
                        ReportPoint(timestamp = START_TIME + 120_000L, db = 88f),
                        ReportPoint(timestamp = START_TIME + 180_000L, db = 70f),
                        ReportPoint(timestamp = START_TIME + 240_000L, db = 91f),
                    ),
            )

        val insights = SleepInsightsCalculator.analyze(report)

        assertEquals(SleepInsightsAvailability.Available, insights.availability)
        assertEquals(2, insights.notableEvents.size)
        assertEquals(START_TIME + 60_000L, insights.notableEvents.first().startTime)
        assertEquals(START_TIME + 120_000L, insights.notableEvents.first().endTime)
        assertEquals(88f, insights.notableEvents.first().maxDb, 0.001f)
        assertEquals(60_000L, insights.notableEvents.first().durationMs)
        assertEquals(91f, insights.loudestEvent?.maxDb ?: 0f, 0.001f)
    }

    @Test
    fun missingMeasurementsKeepInsightCountsUnavailable() {
        val insights = SleepInsightsCalculator.analyze(testSessionReportData(timeSeries = emptyList()))

        assertEquals(SleepInsightsAvailability.MissingMeasurements, insights.availability)
        assertEquals(emptyList<SleepNotableEventSummary>(), insights.notableEvents)
        assertNull(insights.loudestEvent)
    }

    private companion object {
        const val START_TIME = 1_700_000_000_000L
    }
}
