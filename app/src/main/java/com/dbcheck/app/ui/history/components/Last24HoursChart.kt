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
import androidx.compose.ui.unit.dp
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

    DbCheckCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("LAST 24 HOURS", style = typography.labelMd, color = colors.material.onSurfaceVariant)
                    Text("Average ${avgDb.toInt()} dB · $trend", style = typography.bodyMd, color = colors.material.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${peakDb.toInt()}", style = typography.dataXl, color = colors.material.onSurface)
                    Text("PEAK DB", style = typography.labelSm, color = colors.material.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(16.dp))

            val lineColor = colors.material.primary
            val fillGradient =
                remember(colors) {
                    Brush.verticalGradient(
                        colors = listOf(colors.material.primary.copy(alpha = 0.3f), colors.material.primary.copy(alpha = 0f)),
                    )
                }

            Canvas(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(100.dp),
            ) {
                drawLast24HoursChartData(hourlyAverages, lineColor, fillGradient)
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf("00:00", "06:00", "12:00", "18:00", "NOW").forEach { label ->
                    Text(
                        text = label,
                        style = typography.labelSm,
                        color = colors.material.onSurfaceVariant,
                    )
                }
            }
        }
    }
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
