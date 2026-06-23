package com.dbcheck.app.ui.history.detail

import com.dbcheck.app.domain.report.SessionReportData
import java.time.Instant

data class SessionDetailUiState(
    val isLoading: Boolean = true,
    val report: SessionReportData? = null,
    val unavailableReason: SessionDetailUnavailableReason? = null,
    val isProUser: Boolean = false,
    val heartRateOverlayEnabled: Boolean = false,
    val heartRateSamples: List<HeartRateSampleUiState> = emptyList(),
    val heartRateUnavailableMessage: String? = null,
    val hasWavRecording: Boolean = false,
    val isExporting: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null,
) {
    val isNotFound: Boolean
        get() = !isLoading && unavailableReason == SessionDetailUnavailableReason.SESSION_NOT_FOUND

    val isHistoryLocked: Boolean
        get() = !isLoading && unavailableReason == SessionDetailUnavailableReason.HISTORY_LOCKED
}

enum class SessionDetailUnavailableReason {
    SESSION_NOT_FOUND,
    HISTORY_LOCKED,
}

data class HeartRateSampleUiState(val time: Instant, val beatsPerMinute: Long)
