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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.dbcheck.app.data.local.db.dao.HourlyAverage
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun Last24HoursChart(
    hourlyAverages: List<HourlyAverage>,
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
            val fillGradient = remember(colors) {
                Brush.verticalGradient(
                    colors = listOf(colors.material.primary.copy(alpha = 0.3f), colors.material.primary.copy(alpha = 0f)),
                )
            }
            val maxDb = hourlyAverages.maxOfOrNull { it.avgDb }?.coerceAtLeast(1f) ?: 100f

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
            ) {
                if (hourlyAverages.isEmpty()) return@Canvas

                val stepX = size.width / (hourlyAverages.size - 1).coerceAtLeast(1)
                val linePath = Path()
                val fillPath = Path()

                hourlyAverages.forEachIndexed { index, hourly ->
                    val x = index * stepX
                    val y = size.height - (hourly.avgDb / maxDb * size.height * 0.85f)
                    if (index == 0) {
                        linePath.moveTo(x, y)
                        fillPath.moveTo(x, size.height)
                        fillPath.lineTo(x, y)
                    } else {
                        linePath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }

                fillPath.lineTo(size.width, size.height)
                fillPath.close()

                drawPath(path = fillPath, brush = fillGradient)
                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
    }
}
