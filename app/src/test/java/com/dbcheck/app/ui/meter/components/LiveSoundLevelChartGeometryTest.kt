package com.dbcheck.app.ui.meter.components

import com.dbcheck.app.ui.meter.state.LiveChartPointUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveSoundLevelChartGeometryTest {
    @Test
    fun mapsPointsToThirtySecondWindowAndZeroToOneThirtyDbAxis() {
        val geometry =
            liveSoundLevelChartGeometry(
                points =
                    listOf(
                        LiveChartPointUiState(timestampMs = 10_000L, db = 0f),
                        LiveChartPointUiState(timestampMs = 25_000L, db = 65f),
                        LiveChartPointUiState(timestampMs = 40_000L, db = 130f),
                    ),
                width = 300f,
                height = 130f,
            )

        assertEquals(0f, geometry.points[0].x, 0.001f)
        assertEquals(150f, geometry.points[1].x, 0.001f)
        assertEquals(300f, geometry.points[2].x, 0.001f)
        assertEquals(130f, geometry.points[0].y, 0.001f)
        assertEquals(65f, geometry.points[1].y, 0.001f)
        assertEquals(0f, geometry.points[2].y, 0.001f)
        assertTrue(geometry.drawLine)
    }

    @Test
    fun clampsDbValuesAndCalculatesThresholdLine() {
        val geometry =
            liveSoundLevelChartGeometry(
                points =
                    listOf(
                        LiveChartPointUiState(timestampMs = 1_000L, db = -12f),
                        LiveChartPointUiState(timestampMs = 2_000L, db = 140f),
                    ),
                width = 300f,
                height = 130f,
            )

        assertEquals(130f, geometry.points[0].y, 0.001f)
        assertEquals(0f, geometry.points[1].y, 0.001f)
        assertEquals(45f, geometry.thresholdY, 0.001f)
    }

    @Test
    fun marksPointsAtOrAboveThresholdAsPeakMarkers() {
        val geometry =
            liveSoundLevelChartGeometry(
                points =
                    listOf(
                        LiveChartPointUiState(timestampMs = 1_000L, db = 84.9f),
                        LiveChartPointUiState(timestampMs = 2_000L, db = 85f),
                        LiveChartPointUiState(timestampMs = 3_000L, db = 91f),
                    ),
                width = 300f,
                height = 130f,
            )

        assertEquals(2, geometry.peakMarkers.size)
    }

    @Test
    fun emptyAndSinglePointGeometryDoNotDrawLine() {
        assertFalse(
            liveSoundLevelChartGeometry(
                points = emptyList(),
                width = 300f,
                height = 130f,
            ).drawLine,
        )
        assertFalse(
            liveSoundLevelChartGeometry(
                points = listOf(LiveChartPointUiState(timestampMs = 1_000L, db = 70f)),
                width = 300f,
                height = 130f,
            ).drawLine,
        )
    }

    @Test
    fun chartStateSeparatesEmptyActiveAndPaused() {
        assertEquals(
            LiveSoundLevelChartVisualState.Empty,
            liveSoundLevelChartState(points = emptyList(), isRecording = false).visualState,
        )
        assertEquals(
            LiveSoundLevelChartVisualState.Active,
            liveSoundLevelChartState(
                points = listOf(LiveChartPointUiState(timestampMs = 1_000L, db = 70f)),
                isRecording = true,
            ).visualState,
        )
        assertEquals(
            LiveSoundLevelChartVisualState.Paused,
            liveSoundLevelChartState(
                points = listOf(LiveChartPointUiState(timestampMs = 1_000L, db = 70f)),
                isRecording = false,
            ).visualState,
        )
    }
}
