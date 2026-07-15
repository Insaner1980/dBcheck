package com.dbcheck.app.ui.history.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dbcheck.app.R
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.history.state.HourlyExposureUiState
import com.dbcheck.app.ui.theme.ChartTokens
import com.dbcheck.app.ui.theme.DbCheckTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun Last24HoursChart(
    hourlyAverages: List<HourlyExposureUiState>,
    avgDb: Float,
    maxDb: Float,
    trend: String,
    windowStartMs: Long,
    windowEndMs: Long,
    modifier: Modifier = Modifier,
) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val headerState = last24HoursChartHeaderState(hourlyAverages, avgDb, maxDb, trend)
    val subtitle = last24HoursSubtitle(headerState)
    val chartDescription = last24HoursChartContentDescription(headerState, subtitle)

    DbCheckCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Last24HoursHeader(headerState, subtitle)

            Spacer(Modifier.height(16.dp))

            val lineColor = colors.material.primary
            val fillGradient =
                remember(colors) {
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                colors.material.primary.copy(alpha = ChartTokens.AreaAlpha),
                                colors.material.primary.copy(alpha = 0f),
                            ),
                    )
                }

            if (headerState.hasData) {
                Canvas(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .semantics {
                                contentDescription = chartDescription
                            },
                ) {
                    drawLast24HoursChartData(
                        hourlyAverages = hourlyAverages,
                        lineColor = lineColor,
                        fillGradient = fillGradient,
                        windowStartMs = windowStartMs,
                        windowEndMs = windowEndMs,
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.last_24_hours_no_chart_samples),
                    style = typography.bodyMd,
                    color = colors.material.onSurfaceVariant,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                )
            }

            Spacer(Modifier.height(8.dp))
            Last24HoursXAxisLabels(windowStartMs = windowStartMs, windowEndMs = windowEndMs)
        }
    }
}

@Composable
private fun Last24HoursHeader(headerState: Last24HoursChartHeaderState, subtitle: String) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                stringResource(R.string.last_24_hours_title),
                style = typography.labelMd,
                color = colors.material.onSurfaceVariant,
            )
            Text(subtitle, style = typography.bodyMd, color = colors.material.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(headerState.maxLabel, style = typography.dataXl, color = colors.material.onSurface)
            Text(
                stringResource(R.string.last_24_hours_max_db),
                style = typography.labelSm,
                color = colors.material.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Last24HoursXAxisLabels(windowStartMs: Long, windowEndMs: Long) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val nowLabel = stringResource(R.string.last_24_hours_now)
    val xAxisLabels =
        remember(windowStartMs, windowEndMs, nowLabel) {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            last24HoursXAxisLabels(
                windowStartMs = windowStartMs,
                windowEndMs = windowEndMs,
                nowLabel = nowLabel,
                formatTime = { timestamp -> timeFormat.format(Date(timestamp)) },
            )
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        xAxisLabels.forEach { label ->
            Text(
                text = label,
                style = typography.labelSm,
                color = colors.material.onSurfaceVariant,
            )
        }
    }
}

internal data class Last24HoursChartHeaderState(
    val avgDb: Int,
    val trend: String,
    val maxLabel: String,
    val hasData: Boolean,
)

@Composable
internal fun last24HoursSubtitle(state: Last24HoursChartHeaderState): String = if (state.hasData) {
    stringResource(R.string.last_24_hours_average_subtitle, state.avgDb, state.trend)
} else {
    stringResource(R.string.last_24_hours_empty_subtitle)
}

@Composable
internal fun last24HoursChartContentDescription(state: Last24HoursChartHeaderState, subtitle: String): String =
    if (state.hasData) {
        stringResource(R.string.a11y_last_24_hours_chart_with_data, subtitle, state.maxLabel)
    } else {
        stringResource(R.string.a11y_last_24_hours_chart_empty)
    }

internal fun last24HoursChartHeaderState(
    hourlyAverages: List<HourlyExposureUiState>,
    avgDb: Float,
    maxDb: Float,
    trend: String,
): Last24HoursChartHeaderState = if (hourlyAverages.isEmpty()) {
        Last24HoursChartHeaderState(
            avgDb = 0,
            trend = trend,
            maxLabel = "--",
            hasData = false,
        )
    } else {
        Last24HoursChartHeaderState(
            avgDb = avgDb.toInt(),
            trend = trend,
            maxLabel = maxDb.toInt().toString(),
            hasData = true,
        )
    }

private fun DrawScope.drawLast24HoursChartData(
    hourlyAverages: List<HourlyExposureUiState>,
    lineColor: Color,
    fillGradient: Brush,
    windowStartMs: Long,
    windowEndMs: Long,
) {
    if (hourlyAverages.isEmpty()) return

    val geometry =
        last24HoursChartGeometry(
            hourlyAverages = hourlyAverages,
            width = size.width,
            height = size.height,
            windowStartMs = windowStartMs,
            windowEndMs = windowEndMs,
        )
    val linePath = Path()
    val fillPath = Path()

    geometry.points.forEachIndexed { index, point ->
        if (index == 0) {
            linePath.moveTo(point.x, point.y)
            fillPath.moveTo(point.x, size.height)
            fillPath.lineTo(point.x, point.y)
        } else {
            linePath.lineTo(point.x, point.y)
            fillPath.lineTo(point.x, point.y)
        }
    }

    if (geometry.drawFilledArea) {
        fillPath.lineTo(size.width, size.height)
        fillPath.close()
        drawPath(path = fillPath, brush = fillGradient)
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = ChartTokens.LineWidth.toPx(), cap = StrokeCap.Round),
        )
    } else {
        geometry.points.singleOrNull()?.let { point ->
            drawCircle(color = lineColor, radius = ChartTokens.PointRadius.toPx(), center = point)
        }
    }
}

internal fun last24HoursXAxisLabels(
    windowStartMs: Long,
    windowEndMs: Long,
    nowLabel: String,
    formatTime: (Long) -> String,
): List<String> {
    val windowDurationMs = (windowEndMs - windowStartMs).coerceAtLeast(1L)
    return listOf(0L, 1L, 2L, 3L).map { index ->
        formatTime(windowStartMs + windowDurationMs * index / X_AXIS_INTERVAL_COUNT)
    } + nowLabel
}

private const val X_AXIS_INTERVAL_COUNT = 4L
