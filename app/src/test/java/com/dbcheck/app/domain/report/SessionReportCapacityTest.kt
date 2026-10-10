package com.dbcheck.app.domain.report

import com.dbcheck.app.domain.session.Session
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionReportCapacityTest {
    @Test
    fun eightHourOneSecondSessionRetainsAllReportPoints() {
        val start = 1_700_000_000_000L
        val measurements = List(28_800) { ReportMeasurement(start + it * 1_000L, 70f, 90f) }
        val session = Session(
            id = 1L, startTime = start, endTime = start + 28_800_000L,
            minDb = 70f, avgDb = 70f, maxDb = 70f, peakDb = 90f,
            name = null, emoji = null, tags = emptyList(), isActive = false, frequencyWeighting = "A",
        )
        val runtime = Runtime.getRuntime()
        val heapBefore = runtime.totalMemory() - runtime.freeMemory()
        val report = SessionReportCalculator.build(session, measurements, generatedAtMs = session.endTime!!)
        val heapAfter = runtime.totalMemory() - runtime.freeMemory()
        println("Report fixture: 28800 rows; observed heap delta=${heapAfter - heapBefore} bytes")
        assertEquals(28_800, report.measurementCount)
        assertEquals(28_800, report.timeSeries.size)
        assertEquals(start, report.timeSeries.first().timestamp)
        assertEquals(start + 28_799_000L, report.timeSeries.last().timestamp)
        assertEquals(70f, report.laeqDb, 0f)
    }
}
