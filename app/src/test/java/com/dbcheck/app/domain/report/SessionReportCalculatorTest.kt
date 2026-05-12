package com.dbcheck.app.domain.report

import com.dbcheck.app.domain.session.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionReportCalculatorTest {
    @Test
    fun buildReportDataCalculatesNioshMetricsFromEnergyAverage() {
        val startTime = 1_700_000_000_000L
        val endTime = startTime + FOUR_HOURS_MS
        val measurements =
            listOf(
                measurement(startTime, 88f),
                measurement(startTime + ONE_HOUR_MS, 88f),
                measurement(startTime + TWO_HOURS_MS, 88f),
                measurement(startTime + THREE_HOURS_MS, 88f),
            )

        val report =
            SessionReportCalculator.build(
                session = session(startTime = startTime, endTime = endTime, peakDb = 101f),
                measurements = measurements,
                generatedAtMs = endTime,
            )

        assertEquals(88f, report.laeqDb, 0.1f)
        assertEquals(100f, report.dosePercent, 0.5f)
        assertEquals(85f, report.twaDb, 0.2f)
        assertEquals(101f, report.lcPeakDb, 0.1f)
        assertEquals(FOUR_HOURS_MS, report.durationMs)
        assertEquals("Workshop", report.sessionName)
        assertEquals("🎧", report.sessionEmoji)
        assertEquals(listOf("Work", "Music"), report.sessionTags)
    }

    @Test
    fun buildReportDataGroupsContiguousPeakEventsAboveThreshold() {
        val startTime = 1_700_000_000_000L
        val measurements =
            listOf(
                measurement(startTime, 80f),
                measurement(startTime + 1_000L, 86f),
                measurement(startTime + 2_000L, 90f),
                measurement(startTime + 3_000L, 84f),
                measurement(startTime + 4_000L, 87f),
                measurement(startTime + 5_000L, 82f),
            )

        val report =
            SessionReportCalculator.build(
                session = session(startTime = startTime, endTime = startTime + 5_000L),
                measurements = measurements,
                generatedAtMs = startTime + 5_000L,
            )

        assertEquals(2, report.peakEvents.size)
        assertEquals(startTime + 1_000L, report.peakEvents[0].startTime)
        assertEquals(startTime + 2_000L, report.peakEvents[0].endTime)
        assertEquals(startTime + 2_000L, report.peakEvents[0].peakTime)
        assertEquals(90f, report.peakEvents[0].maxDb, 0.1f)
        assertEquals(startTime + 4_000L, report.peakEvents[1].startTime)
        assertEquals(87f, report.peakEvents[1].maxDb, 0.1f)
    }

    @Test
    fun buildReportDataFallsBackToSessionSummaryWithoutMeasurements() {
        val startTime = 1_700_000_000_000L
        val report =
            SessionReportCalculator.build(
                session =
                    session(
                        startTime = startTime,
                        endTime = startTime + 20 * 60 * 1_000L,
                        avgDb = 72f,
                        maxDb = 76f,
                        peakDb = 89f,
                    ),
                measurements = emptyList(),
                generatedAtMs = startTime + 20 * 60 * 1_000L,
            )

        assertEquals(72f, report.laeqDb, 0.1f)
        assertEquals(76f, report.maxDb, 0.1f)
        assertEquals(89f, report.lcPeakDb, 0.1f)
        assertEquals(0, report.measurementCount)
        assertTrue(report.timeSeries.isEmpty())
        assertTrue(report.peakEvents.isEmpty())
    }

    @Test
    fun buildReportDataUsesDefaultNameWhenSessionHasNoCustomName() {
        val startTime = 1_700_000_000_000L
        val report =
            SessionReportCalculator.build(
                session =
                    session(
                        startTime = startTime,
                        endTime = startTime + 1_000L,
                        name = null,
                        tags = emptyList(),
                    ),
                measurements = emptyList(),
                generatedAtMs = startTime + 1_000L,
            )

        assertEquals("Session $startTime", report.sessionName)
        assertEquals(emptyList<String>(), report.sessionTags)
    }

    private fun session(
        startTime: Long,
        endTime: Long?,
        avgDb: Float = 88f,
        maxDb: Float = 90f,
        peakDb: Float = 95f,
        name: String? = "Workshop",
        tags: List<String> = listOf("Work", "Music"),
    ) = Session(
        id = 42L,
        startTime = startTime,
        endTime = endTime,
        minDb = 60f,
        avgDb = avgDb,
        maxDb = maxDb,
        peakDb = peakDb,
        name = name,
        emoji = "🎧",
        tags = tags,
        isActive = false,
        frequencyWeighting = "A",
    )

    private fun measurement(
        timestamp: Long,
        weightedDb: Float,
    ) = ReportMeasurement(
        timestamp = timestamp,
        dbWeighted = weightedDb,
    )

    private companion object {
        const val ONE_HOUR_MS = 60 * 60 * 1_000L
        const val TWO_HOURS_MS = 2 * ONE_HOUR_MS
        const val THREE_HOURS_MS = 3 * ONE_HOUR_MS
        const val FOUR_HOURS_MS = 4 * ONE_HOUR_MS
    }
}
