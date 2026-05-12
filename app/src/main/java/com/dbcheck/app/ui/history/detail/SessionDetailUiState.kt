package com.dbcheck.app.ui.history.detail

import com.dbcheck.app.domain.report.SessionReportData
import java.time.Instant

data class SessionDetailUiState(
    val isLoading: Boolean = true,
    val report: SessionReportData? = null,
    val isProUser: Boolean = false,
    val heartRateOverlayEnabled: Boolean = false,
    val heartRateSamples: List<HeartRateSampleUiState> = emptyList(),
    val isExporting: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null,
) {
    val isNotFound: Boolean
        get() = !isLoading && report == null
}

data class HeartRateSampleUiState(
    val time: Instant,
    val beatsPerMinute: Long,
)
