package com.dbcheck.app.ui.history.state

import androidx.compose.runtime.Immutable
import com.dbcheck.app.domain.session.Session

@Immutable
sealed interface HistoryUiState {
    data object Loading : HistoryUiState

    data object Empty : HistoryUiState

    data class Error(val message: String) : HistoryUiState

    @Immutable
    data class Success(
        val last24HoursData: List<HourlyExposureUiState> = emptyList(),
        val last24HoursAvg: Float = 0f,
        val last24HoursMax: Float = 0f,
        val last24HoursTrend: String? = null,
        val last24HoursWindowStartMs: Long = 0L,
        val last24HoursWindowEndMs: Long = 0L,
        val recentSessions: List<Session> = emptyList(),
        val sleepSessionIds: Set<Long> = emptySet(),
        val weeklyTrendPercent: Int? = null,
        val weeklyTrendLabel: String? = null,
        val safeHours: Float? = null,
        val isProUser: Boolean = false,
        val isShowingAllSessions: Boolean = false,
        val searchQuery: String = "",
        val selectedSearchFilter: HistorySearchFilter = HistorySearchFilter.ALL,
        val hasActiveSearch: Boolean = false,
        val isHistorySearchLocked: Boolean = false,
        val metadataErrorMessage: String? = null,
    ) : HistoryUiState
}

enum class HistorySearchFilter {
    ALL,
    A_WEIGHTED,
    LOUD,
    WITH_LOCATION,
}

data class HourlyExposureUiState(val hour: Int, val avgDb: Float, val maxDb: Float, val hourStartMs: Long = 0L)
