package com.dbcheck.app.ui.history.components

import androidx.compose.ui.geometry.Offset
import com.dbcheck.app.ui.history.state.HourlyExposureUiState

internal data class Last24HoursChartGeometry(val points: List<Offset>, val drawFilledArea: Boolean)

internal fun last24HoursChartGeometry(
    hourlyAverages: List<HourlyExposureUiState>,
    width: Float,
    height: Float,
    windowStartMs: Long = 0L,
    windowEndMs: Long = LAST_24_HOURS_MILLIS,
): Last24HoursChartGeometry {
    if (hourlyAverages.isEmpty()) {
        return Last24HoursChartGeometry(points = emptyList(), drawFilledArea = false)
    }

    val maxDb = hourlyAverages.maxOfOrNull { it.avgDb }?.coerceAtLeast(1f) ?: 100f
    val windowDurationMs = (windowEndMs - windowStartMs).coerceAtLeast(1L).toDouble()
    return Last24HoursChartGeometry(
        points =
            hourlyAverages.map { hourly ->
                Offset(
                    x = (((hourly.hourStartMs - windowStartMs) / windowDurationMs).coerceIn(0.0, 1.0) * width)
                        .toFloat(),
                    y = height - (hourly.avgDb / maxDb * height * CHART_HEIGHT_FRACTION),
                )
            },
        drawFilledArea = hourlyAverages.size >= 2,
    )
}

private const val CHART_HEIGHT_FRACTION = 0.85f
private const val LAST_24_HOURS_MILLIS = 24L * 60L * 60L * 1_000L
