package com.dbcheck.app.ui.history.state

import com.dbcheck.app.domain.session.Session

sealed interface HistoryUiState {
    data object Loading : HistoryUiState

    data object Empty : HistoryUiState

    data class Error(val message: String) : HistoryUiState

    data class Success(
        val last24HoursData: List<HourlyExposureUiState> = emptyList(),
        val last24HoursAvg: Float = 0f,
        val last24HoursMax: Float = 0f,
        val last24HoursTrend: String = "Stable",
        val last24HoursWindowStartMs: Long = 0L,
        val last24HoursWindowEndMs: Long = 0L,
        val recentSessions: List<Session> = emptyList(),
        val weeklyTrendPercent: Int = 0,
        val weeklyTrendLabel: String = "",
        val safeHours: Float = 0f,
        val isProUser: Boolean = false,
        val isShowingAllSessions: Boolean = false,
        val metadataErrorMessage: String? = null,
    ) : HistoryUiState
}

data class HourlyExposureUiState(
    val hour: Int,
    val avgDb: Float,
    val maxDb: Float,
    val hourStartMs: Long = 0L,
)
