package com.dbcheck.app.ui.history.detail

import androidx.compose.runtime.Immutable
import com.dbcheck.app.domain.report.DbHistogramBucket
import com.dbcheck.app.domain.report.SessionReportData
import java.time.Instant

@Immutable
data class SessionDetailUiState(
    val isLoading: Boolean = true,
    val report: SessionReportData? = null,
    val unavailableReason: SessionDetailUnavailableReason? = null,
    val isProUser: Boolean = false,
    val heartRateOverlayEnabled: Boolean = false,
    val heartRateSamples: List<HeartRateSampleUiState> = emptyList(),
    val heartRateUnavailableMessage: String? = null,
    val sleepResults: SleepResultsUiState? = null,
    val sleepInsights: SleepInsightsUiState? = null,
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

@Immutable
data class SleepResultsUiState(
    val targetDurationMinutes: Int,
    val recordedDurationMs: Long,
    val equivalentLevelLabel: String,
    val equivalentLevelDb: Float,
    val maxDb: Float,
    val lcPeakDb: Float,
    val peakEventCount: Int?,
    val loudPeriodCount: Int?,
    val sampleCount: Int?,
    val histogramBuckets: List<DbHistogramBucket>,
)

data class SleepInsightsUiState(
    val isAvailable: Boolean,
    val notableEventCount: Int?,
    val loudestPeriod: SleepInsightPeriodUiState?,
)

data class SleepInsightPeriodUiState(val durationMs: Long, val maxDb: Float)
