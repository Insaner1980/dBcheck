package com.dbcheck.app.ui.history.components

import androidx.compose.ui.geometry.Offset
import com.dbcheck.app.ui.history.state.HourlyExposureUiState

internal data class Last24HoursChartGeometry(val points: List<Offset>, val drawFilledArea: Boolean)

internal fun last24HoursChartGeometry(
    hourlyAverages: List<HourlyExposureUiState>,
    width: Float,
    height: Float,
): Last24HoursChartGeometry {
    if (hourlyAverages.isEmpty()) {
        return Last24HoursChartGeometry(points = emptyList(), drawFilledArea = false)
    }

    val maxDb = hourlyAverages.maxOfOrNull { it.avgDb }?.coerceAtLeast(1f) ?: 100f
    val stepX = width / (hourlyAverages.size - 1).coerceAtLeast(1)
    return Last24HoursChartGeometry(
        points =
            hourlyAverages.mapIndexed { index, hourly ->
                Offset(
                    x = index * stepX,
                    y = height - (hourly.avgDb / maxDb * height * CHART_HEIGHT_FRACTION),
                )
            },
        drawFilledArea = hourlyAverages.size >= 2,
    )
}

private const val CHART_HEIGHT_FRACTION = 0.85f
