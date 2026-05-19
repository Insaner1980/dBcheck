package com.dbcheck.app.ui.history.components

import com.dbcheck.app.ui.history.state.HourlyExposureUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Last24HoursChartTest {
    @Test
    fun singlePointGeometryDoesNotCreateFilledArea() {
        val geometry =
            last24HoursChartGeometry(
                hourlyAverages = listOf(HourlyExposureUiState(hour = 14, avgDb = 70f, maxDb = 72f)),
                width = 240f,
                height = 100f,
            )

        assertEquals(1, geometry.points.size)
        assertFalse(geometry.drawFilledArea)
    }

    @Test
    fun multiPointGeometryCreatesFilledArea() {
        val geometry =
            last24HoursChartGeometry(
                hourlyAverages =
                    listOf(
                        HourlyExposureUiState(hour = 14, avgDb = 60f, maxDb = 61f),
                        HourlyExposureUiState(hour = 15, avgDb = 70f, maxDb = 72f),
                    ),
                width = 240f,
                height = 100f,
            )

        assertEquals(2, geometry.points.size)
        assertTrue(geometry.drawFilledArea)
    }

    @Test
    fun emptyChartStateDoesNotReportZeroDecibels() {
        val state =
            last24HoursChartHeaderState(
                hourlyAverages = emptyList(),
                avgDb = 0f,
                peakDb = 0f,
                trend = "Stable",
            )

        assertEquals(0, state.avgDb)
        assertEquals("Stable", state.trend)
        assertEquals("--", state.peakLabel)
        assertFalse(state.hasData)
    }
}
