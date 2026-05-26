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
                        HourlyExposureUiState(hour = 14, avgDb = 60f, maxDb = 61f, hourStartMs = WINDOW_START_MS),
                        HourlyExposureUiState(
                            hour = 15,
                            avgDb = 70f,
                            maxDb = 72f,
                            hourStartMs = WINDOW_START_MS + HOUR_MS,
                        ),
                    ),
                width = 240f,
                height = 100f,
                windowStartMs = WINDOW_START_MS,
                windowEndMs = WINDOW_END_MS,
            )

        assertEquals(2, geometry.points.size)
        assertTrue(geometry.drawFilledArea)
    }

    @Test
    fun sparseHourlyBucketsUseRollingWindowTimePositions() {
        val geometry =
            last24HoursChartGeometry(
                hourlyAverages =
                    listOf(
                        HourlyExposureUiState(hour = 0, avgDb = 60f, maxDb = 61f, hourStartMs = WINDOW_START_MS),
                        HourlyExposureUiState(
                            hour = 20,
                            avgDb = 70f,
                            maxDb = 72f,
                            hourStartMs = WINDOW_START_MS + 20 * HOUR_MS,
                        ),
                    ),
                width = 240f,
                height = 100f,
                windowStartMs = WINDOW_START_MS,
                windowEndMs = WINDOW_END_MS,
            )

        assertEquals(0f, geometry.points[0].x, 0.001f)
        assertEquals(200f, geometry.points[1].x, 0.001f)
    }

    @Test
    fun xAxisLabelsComeFromRollingWindow() {
        val labels =
            last24HoursXAxisLabels(
                windowStartMs = WINDOW_START_MS,
                windowEndMs = WINDOW_END_MS,
                nowLabel = "NOW",
                formatTime = { timestamp -> "${(timestamp - WINDOW_START_MS) / HOUR_MS}:00" },
            )

        assertEquals(listOf("0:00", "6:00", "12:00", "18:00", "NOW"), labels)
    }

    @Test
    fun emptyChartStateDoesNotReportZeroDecibels() {
        val state =
            last24HoursChartHeaderState(
                hourlyAverages = emptyList(),
                avgDb = 0f,
                maxDb = 0f,
                trend = "Stable",
            )

        assertEquals(0, state.avgDb)
        assertEquals("Stable", state.trend)
        assertEquals("--", state.maxLabel)
        assertFalse(state.hasData)
    }

    private companion object {
        const val HOUR_MS = 60L * 60L * 1_000L
        const val WINDOW_START_MS = 1_700_000_000_000L
        const val WINDOW_END_MS = WINDOW_START_MS + 24 * HOUR_MS
    }
}
