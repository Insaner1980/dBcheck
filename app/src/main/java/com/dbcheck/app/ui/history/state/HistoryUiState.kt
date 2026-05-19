package com.dbcheck.app.ui.history.state

import com.dbcheck.app.domain.session.Session

sealed interface HistoryUiState {
    data object Loading : HistoryUiState

    data object Empty : HistoryUiState

    data class Success(
        val last24HoursData: List<HourlyExposureUiState> = emptyList(),
        val last24HoursAvg: Float = 0f,
        val last24HoursPeak: Float = 0f,
        val last24HoursTrend: String = "Stable",
        val recentSessions: List<Session> = emptyList(),
        val weeklyTrendPercent: Int = 0,
        val weeklyTrendLabel: String = "",
        val safeHours: Float = 0f,
        val isProUser: Boolean = false,
        val isShowingAllSessions: Boolean = false,
    ) : HistoryUiState
}

data class HourlyExposureUiState(val hour: Int, val avgDb: Float, val maxDb: Float)
