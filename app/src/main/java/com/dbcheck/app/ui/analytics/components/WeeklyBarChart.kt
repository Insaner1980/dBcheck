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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import com.dbcheck.app.data.local.db.dao.DailyAverage
import com.dbcheck.app.ui.theme.DbCheckTheme
import com.dbcheck.app.ui.theme.SpaceGroteskFamily
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
    val labelColor = colors.material.onSurfaceVariant
    val maxDb = dailyAverages.maxOfOrNull { it.avgDb }?.coerceAtLeast(1f) ?: 100f

    val dayLabels = remember(dailyAverages) {
        val dayFormat = SimpleDateFormat("E", Locale.getDefault())
        dailyAverages.map { dayFormat.format(Date(it.day)).first().uppercase() }
    }

    val labelSizePx = with(LocalDensity.current) { 11.sp.toPx() }
    val labelBottomPadding = with(LocalDensity.current) { 4.sp.toPx() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val barCount = dailyAverages.size.coerceAtLeast(1)
        val gap = 8f
        val labelAreaHeight = labelSizePx + labelBottomPadding
        val chartHeight = size.height - labelAreaHeight
        val barWidth = (size.width - gap * (barCount - 1)) / barCount
        val todayIndex = barCount - 1

        dailyAverages.forEachIndexed { index, daily ->
            val barHeight = (daily.avgDb / maxDb) * chartHeight * 0.85f
            val x = index * (barWidth + gap)
            val y = chartHeight - barHeight

            drawRoundRect(
                brush = if (index == todayIndex) gradient else dimGradient,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(8f, 8f),
            )

            // Day label below bar
            if (index < dayLabels.size) {
                drawContext.canvas.nativeCanvas.drawText(
                    dayLabels[index],
                    x + barWidth / 2,
                    size.height,
                    android.graphics.Paint().apply {
                        color = labelColor.toArgb()
                        textSize = labelSizePx
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    },
                )
            }
        }
    }
}
