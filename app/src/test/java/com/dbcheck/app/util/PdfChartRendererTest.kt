package com.dbcheck.app.util

import com.dbcheck.app.domain.report.ReportPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfChartRendererTest {
    @Test
    fun mapTimeSeriesFillsHorizontalRangeAndInvertsDbToY() {
        val points =
            listOf(
                ReportPoint(timestamp = START_TIME, db = 60f),
                ReportPoint(timestamp = START_TIME + 1_000L, db = 90f),
            )

        val mapped =
            PdfChartRenderer.mapTimeSeries(
                points = points,
                width = 300f,
                height = 120f,
                minDb = 50f,
                maxDb = 100f,
            )

        assertEquals(0f, mapped[0].x, 0.01f)
        assertEquals(300f, mapped[1].x, 0.01f)
        assertTrue(mapped[1].y < mapped[0].y)
    }

    @Test
    fun mapTimeSeriesCentersSinglePoint() {
        val mapped =
            PdfChartRenderer.mapTimeSeries(
                points = listOf(ReportPoint(timestamp = START_TIME, db = 75f)),
                width = 300f,
                height = 120f,
                minDb = 50f,
                maxDb = 100f,
            )

        assertEquals(150f, mapped.single().x, 0.01f)
    }

    private companion object {
        const val START_TIME = 1_700_000_000_000L
    }
}
