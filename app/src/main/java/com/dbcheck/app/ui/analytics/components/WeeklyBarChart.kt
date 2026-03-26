package com.dbcheck.app.ui.analytics.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import com.dbcheck.app.data.local.db.dao.DailyAverage
import com.dbcheck.app.ui.theme.DbCheckTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WeeklyBarChart(
    dailyAverages: List<DailyAverage>,
    modifier: Modifier = Modifier,
) {
    val colors = DbCheckTheme.colorScheme
    val gradient = remember(colors) {
        Brush.verticalGradient(
            colors = listOf(colors.material.primary, colors.material.secondary),
        )
    }
    val dimGradient = remember(colors) {
        Brush.verticalGradient(
            colors = listOf(
                colors.material.primary.copy(alpha = 0.6f),
                colors.material.secondary.copy(alpha = 0.6f),
            ),
        )
    }
    val maxDb = dailyAverages.maxOfOrNull { it.avgDb }?.coerceAtLeast(1f) ?: 100f

    Canvas(modifier = modifier.fillMaxSize()) {
        val barCount = dailyAverages.size.coerceAtLeast(1)
        val gap = 8f
        val barWidth = (size.width - gap * (barCount - 1)) / barCount
        val todayIndex = barCount - 1

        dailyAverages.forEachIndexed { index, daily ->
            val barHeight = (daily.avgDb / maxDb) * size.height * 0.85f
            val x = index * (barWidth + gap)
            val y = size.height - barHeight

            drawRoundRect(
                brush = if (index == todayIndex) gradient else dimGradient,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(8f, 8f),
            )
        }
    }
}
