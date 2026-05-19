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
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun Last24HoursChart(
    hourlyAverages: List<HourlyExposureUiState>,
    avgDb: Float,
    peakDb: Float,
    trend: String,
    modifier: Modifier = Modifier,
) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val headerState = last24HoursChartHeaderState(hourlyAverages, avgDb, peakDb, trend)
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
                                colors.material.primary.copy(alpha = 0.3f),
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
                    drawLast24HoursChartData(hourlyAverages, lineColor, fillGradient)
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
            Last24HoursXAxisLabels()
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
            Text(headerState.peakLabel, style = typography.dataXl, color = colors.material.onSurface)
            Text(
                stringResource(R.string.last_24_hours_peak_db),
                style = typography.labelSm,
                color = colors.material.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Last24HoursXAxisLabels() {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val xAxisLabels =
        listOf(
            "00:00",
            "06:00",
            "12:00",
            "18:00",
            stringResource(R.string.last_24_hours_now),
        )

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
    val peakLabel: String,
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
        stringResource(R.string.a11y_last_24_hours_chart_with_data, subtitle, state.peakLabel)
    } else {
        stringResource(R.string.a11y_last_24_hours_chart_empty)
    }

internal fun last24HoursChartHeaderState(
    hourlyAverages: List<HourlyExposureUiState>,
    avgDb: Float,
    peakDb: Float,
    trend: String,
): Last24HoursChartHeaderState = if (hourlyAverages.isEmpty()) {
        Last24HoursChartHeaderState(
            avgDb = 0,
            trend = trend,
            peakLabel = "--",
            hasData = false,
        )
    } else {
        Last24HoursChartHeaderState(
            avgDb = avgDb.toInt(),
            trend = trend,
            peakLabel = peakDb.toInt().toString(),
            hasData = true,
        )
    }

private fun DrawScope.drawLast24HoursChartData(
    hourlyAverages: List<HourlyExposureUiState>,
    lineColor: Color,
    fillGradient: Brush,
) {
    if (hourlyAverages.isEmpty()) return

    val geometry =
        last24HoursChartGeometry(
            hourlyAverages = hourlyAverages,
            width = size.width,
            height = size.height,
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
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )
    } else {
        geometry.points.singleOrNull()?.let { point ->
            drawCircle(color = lineColor, radius = 3.dp.toPx(), center = point)
        }
    }
}
