package com.dbcheck.app.ui.analytics.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.theme.DbCheckTheme

data class YearlyReport(
    val totalSessions: Int = 0,
    val avgDailyExposure: Float = 0f,
    val loudestEnvironment: String = "",
    val loudestDb: Float = 0f,
    val quietPercent: Float = 0f,
    val moderatePercent: Float = 0f,
    val loudPercent: Float = 0f,
    val criticalPercent: Float = 0f,
)

@Composable
fun YearlyReportCard(
    report: YearlyReport,
    modifier: Modifier = Modifier,
) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    DbCheckCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("YEARLY SUMMARY", style = typography.labelMd, color = colors.material.onSurfaceVariant)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("Sessions", report.totalSessions.toString())
                StatItem("Avg Daily", "${report.avgDailyExposure.toInt()} dB")
                StatItem("Loudest", "${report.loudestDb.toInt()} dB")
            }

            Spacer(Modifier.height(8.dp))

            Text("NOISE ZONE DISTRIBUTION", style = typography.labelMd, color = colors.material.onSurfaceVariant)

            // Simple pie chart / horizontal bar
            val zones =
                listOf(
                    "Quiet" to report.quietPercent to colors.success,
                    "Moderate" to report.moderatePercent to colors.material.primary,
                    "Loud" to report.loudPercent to colors.warning,
                    "Critical" to report.criticalPercent to colors.material.error,
                )

            Canvas(modifier = Modifier.fillMaxWidth().height(24.dp)) {
                var startX = 0f
                zones.forEach { (labelAndPercent, color) ->
                    val (_, percent) = labelAndPercent
                    val barWidth = size.width * (percent / 100f)
                    drawRect(
                        color = color,
                        topLeft = Offset(startX, 0f),
                        size = Size(barWidth, size.height),
                    )
                    startX += barWidth
                }
            }

            zones.forEach { (labelAndPercent, color) ->
                val (label, percent) = labelAndPercent
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Canvas(Modifier.size(8.dp)) { drawCircle(color = color) }
                    Text("$label ${percent.toInt()}%", style = typography.bodyMd, color = colors.material.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = typography.dataLg, color = colors.material.onSurface)
        Text(label, style = typography.labelSm, color = colors.material.onSurfaceVariant)
    }
}
