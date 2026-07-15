package com.dbcheck.app.ui.meter.components

import androidx.compose.ui.geometry.Offset
import com.dbcheck.app.domain.noise.SoundLevelDisplayScale
import com.dbcheck.app.ui.meter.state.LiveChartPointUiState
import com.dbcheck.app.ui.meter.state.METER_LIVE_CHART_WINDOW_MS

internal data class LiveSoundLevelChartGeometry(
    val points: List<Offset>,
    val peakMarkers: List<Offset>,
    val thresholdY: Float,
    val drawLine: Boolean,
)

internal fun liveSoundLevelChartGeometry(
    points: List<LiveChartPointUiState>,
    width: Float,
    height: Float,
    windowMs: Long = METER_LIVE_CHART_WINDOW_MS,
): LiveSoundLevelChartGeometry {
    val latestTimestampMs = points.maxOfOrNull { it.timestampMs } ?: 0L
    val windowStartMs = latestTimestampMs - windowMs
    val windowDurationMs = windowMs.coerceAtLeast(1L).toDouble()
    val offsets =
        points.map { point ->
            Offset(
                x =
                    (((point.timestampMs - windowStartMs) / windowDurationMs).coerceIn(0.0, 1.0) * width)
                        .toFloat(),
                y = yForDb(point.db, height),
            )
        }

    return LiveSoundLevelChartGeometry(
        points = offsets,
        peakMarkers =
            offsets.filterIndexed { index, _ ->
                points[index].db >= LIVE_CHART_THRESHOLD_DB
            },
        thresholdY = yForDb(LIVE_CHART_THRESHOLD_DB, height),
        drawLine = offsets.size >= 2,
    )
}

private fun yForDb(db: Float, height: Float): Float = height - SoundLevelDisplayScale.positionForDb(db) * height

internal const val LIVE_CHART_THRESHOLD_DB = 85f
