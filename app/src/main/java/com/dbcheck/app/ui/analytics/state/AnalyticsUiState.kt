package com.dbcheck.app.ui.analytics.state

import com.dbcheck.app.data.local.db.dao.DailyAverage

sealed interface AnalyticsUiState {
    data object Loading : AnalyticsUiState
    data object Empty : AnalyticsUiState
    data class Success(
        val weeklyAverageDb: Float = 0f,
        val dailyAverages: List<DailyAverage> = emptyList(),
        val healthStatus: HealthStatus = HealthStatus.SAFE,
        val todayVsWeekPercent: Int = 0,
        val isProUser: Boolean = false,
    ) : AnalyticsUiState
}

enum class HealthStatus { SAFE, WARNING, DANGER }
