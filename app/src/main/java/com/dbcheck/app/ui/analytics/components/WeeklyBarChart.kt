package com.dbcheck.app.ui.analytics.components

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.dbcheck.app.R
import com.dbcheck.app.ui.analytics.state.DailyExposureUiState
import com.dbcheck.app.ui.theme.ChartTokens
import com.dbcheck.app.ui.theme.DbCheckTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WeeklyBarChart(dailyAverages: List<DailyExposureUiState>, modifier: Modifier = Modifier) {
    val colors = DbCheckTheme.colorScheme
    val todayBarColor = colors.material.primary
    val defaultBarColor = colors.primaryDim
    val labelColor = colors.material.onSurfaceVariant
    val maxDb = dailyAverages.maxOfOrNull { it.avgDb }?.coerceAtLeast(1f) ?: 100f
    val context = LocalContext.current
    val labelTypeface =
        remember(context) {
            ResourcesCompat.getFont(context, R.font.space_grotesk_regular)
        }
    val chartDescription =
        remember(context, dailyAverages) {
            weeklyBarChartContentDescription(context, dailyAverages)
        }

    val dayLabels =
        remember(dailyAverages) {
            val dayFormat = SimpleDateFormat("E", Locale.getDefault())
            dailyAverages.map { dayFormat.format(Date(it.dayStartMs)).first().uppercase() }
        }

    val labelSizePx = with(LocalDensity.current) { 11.sp.toPx() }
    val labelBottomPadding = with(LocalDensity.current) { 4.sp.toPx() }

    Canvas(
        modifier =
            modifier
                .semantics {
                    contentDescription = chartDescription
                }
                .fillMaxSize(),
    ) {
        val barCount = dailyAverages.size.coerceAtLeast(1)
        val gap = 8f
        val labelAreaHeight = labelSizePx + labelBottomPadding
        val chartHeight = size.height - labelAreaHeight
        val barWidth = (size.width - gap * (barCount - 1)) / barCount
        val labelPaint =
            android.graphics.Paint().apply {
                color = labelColor.toArgb()
                textSize = labelSizePx
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                typeface = labelTypeface
            }
        val labelBaseline =
            weeklyBarLabelBaseline(
                canvasHeight = size.height,
                labelBottomPadding = labelBottomPadding,
                textDescent = labelPaint.fontMetrics.descent,
            )

        dailyAverages.forEachIndexed { index, daily ->
            val barHeight = (daily.avgDb / maxDb) * chartHeight * 0.85f
            val x = index * (barWidth + gap)
            val y = chartHeight - barHeight

            drawRoundRect(
                color = if (daily.isToday) todayBarColor else defaultBarColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(ChartTokens.BarRadius.toPx(), ChartTokens.BarRadius.toPx()),
            )

            // Day label below bar
            if (index < dayLabels.size) {
                drawContext.canvas.nativeCanvas.drawText(
                    dayLabels[index],
                    x + barWidth / 2,
                    labelBaseline,
                    labelPaint,
                )
            }
        }
    }
}

internal fun weeklyBarLabelBaseline(canvasHeight: Float, labelBottomPadding: Float, textDescent: Float): Float =
    (canvasHeight - labelBottomPadding - textDescent).coerceIn(0f, canvasHeight)

internal fun weeklyBarChartContentDescription(context: Context, dailyAverages: List<DailyExposureUiState>): String {
    if (dailyAverages.isEmpty()) return context.getString(R.string.a11y_weekly_exposure_chart_empty)

    val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    val dailyDescriptions =
        dailyAverages.joinToString(separator = "; ") { daily ->
            val dayLabel =
                if (daily.isToday) {
                    context.getString(R.string.date_today)
                } else {
                    dayFormat.format(Date(daily.dayStartMs))
                }
            context.getString(R.string.a11y_weekly_exposure_day, dayLabel, daily.avgDb.toInt(), daily.maxDb.toInt())
        }
    return context.getString(R.string.a11y_weekly_exposure_chart_with_data, dailyDescriptions)
}
