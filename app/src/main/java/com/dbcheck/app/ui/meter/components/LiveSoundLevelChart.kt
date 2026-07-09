package com.dbcheck.app.ui.meter.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dbcheck.app.R
import com.dbcheck.app.ui.meter.state.LiveChartPointUiState
import com.dbcheck.app.ui.theme.ChartTokens
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun LiveSoundLevelChart(points: List<LiveChartPointUiState>, isRecording: Boolean, modifier: Modifier = Modifier) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val chartState = liveSoundLevelChartState(points = points, isRecording = isRecording)
    val contentDescription = liveSoundLevelChartContentDescription(chartState)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(LIVE_CHART_HEIGHT_DP),
        contentAlignment = Alignment.Center,
    ) {
        LiveSoundLevelChartCanvas(
            points = points,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
        )

        if (chartState.visualState == LiveSoundLevelChartVisualState.Empty) {
            Text(
                text = stringResource(R.string.meter_live_chart_empty),
                style = typography.bodyMd,
                color = colors.material.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        } else if (chartState.visualState == LiveSoundLevelChartVisualState.Paused) {
            Text(
                text = stringResource(R.string.meter_live_chart_paused),
                style = typography.labelSm,
                color = colors.material.onSurfaceVariant,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

@Composable
private fun LiveSoundLevelChartCanvas(
    points: List<LiveChartPointUiState>,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = DbCheckTheme.colorScheme
    val gridColor = colors.ghostBorder
    val thresholdColor = colors.material.error.copy(alpha = 0.72f)
    val lineColor = colors.material.primary
    val markerColor = colors.material.error
    val quietPointColor = colors.primaryDim

    Canvas(
        modifier =
            modifier.semantics {
                this.contentDescription = contentDescription
            },
    ) {
        val geometry =
            liveSoundLevelChartGeometry(
                points = points,
                width = size.width,
                height = size.height,
            )

        repeat(LIVE_CHART_GRID_LINE_COUNT) { index ->
            val y = size.height * index / (LIVE_CHART_GRID_LINE_COUNT - 1)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = ChartTokens.GridLineWidth.toPx(),
            )
        }

        drawLine(
            color = thresholdColor,
            start = Offset(0f, geometry.thresholdY),
            end = Offset(size.width, geometry.thresholdY),
            strokeWidth = ChartTokens.ThresholdLineWidth.toPx(),
            pathEffect = PathEffect.dashPathEffect(ChartTokens.ThresholdDashPattern),
        )

        if (geometry.drawLine) {
            val path = Path()
            geometry.points.forEachIndexed { index, point ->
                if (index == 0) {
                    path.moveTo(point.x, point.y)
                } else {
                    path.lineTo(point.x, point.y)
                }
            }
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = ChartTokens.LiveLineWidth.toPx(), cap = StrokeCap.Round),
            )
        }

        geometry.points.singleOrNull()?.let { point ->
            drawPointMarker(point = point, color = quietPointColor)
        }

        geometry.peakMarkers.forEach { point ->
            drawPointMarker(point = point, color = markerColor)
        }
    }
}

private fun DrawScope.drawPointMarker(point: Offset, color: Color) {
    drawCircle(color = color, radius = ChartTokens.PointRadius.toPx(), center = point)
}

@Composable
internal fun liveSoundLevelChartContentDescription(state: LiveSoundLevelChartState): String = when (state.visualState) {
    LiveSoundLevelChartVisualState.Empty -> stringResource(R.string.a11y_meter_live_chart_empty)

    LiveSoundLevelChartVisualState.Active ->
        pluralStringResource(
            R.plurals.a11y_meter_live_chart_active,
            state.sampleCount,
            state.sampleCount,
            state.latestDb,
            state.maxDb,
        )

    LiveSoundLevelChartVisualState.Paused ->
        pluralStringResource(
            R.plurals.a11y_meter_live_chart_paused,
            state.sampleCount,
            state.sampleCount,
            state.latestDb,
            state.maxDb,
        )
}

internal data class LiveSoundLevelChartState(
    val visualState: LiveSoundLevelChartVisualState,
    val sampleCount: Int,
    val latestDb: Int,
    val maxDb: Int,
)

internal enum class LiveSoundLevelChartVisualState {
    Empty,
    Active,
    Paused,
}

internal fun liveSoundLevelChartState(
    points: List<LiveChartPointUiState>,
    isRecording: Boolean,
): LiveSoundLevelChartState {
    if (points.isEmpty()) {
        return LiveSoundLevelChartState(
            visualState = LiveSoundLevelChartVisualState.Empty,
            sampleCount = 0,
            latestDb = 0,
            maxDb = 0,
        )
    }

    return LiveSoundLevelChartState(
        visualState =
            if (isRecording) {
                LiveSoundLevelChartVisualState.Active
            } else {
                LiveSoundLevelChartVisualState.Paused
            },
        sampleCount = points.size,
        latestDb = points.last().db.toInt(),
        maxDb = points.maxOf { it.db }.toInt(),
    )
}

private val LIVE_CHART_HEIGHT_DP = 116.dp
private const val LIVE_CHART_GRID_LINE_COUNT = 4
