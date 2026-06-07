package com.dbcheck.app.ui.analytics.state

import com.dbcheck.app.domain.audio.SpectralBandwidth

sealed interface AnalyticsUiState {
    data object Loading : AnalyticsUiState

    data object Empty : AnalyticsUiState

    data class Error(val message: String) : AnalyticsUiState

    data class Success(
        val weeklyAverageDb: Float = 0f,
        val dailyAverages: List<DailyExposureUiState> = emptyList(),
        val healthStatus: HealthStatus = HealthStatus.SAFE,
        val todayVsWeekPercent: Int = 0,
        val isProUser: Boolean = false,
        val hasExposureData: Boolean = true,
        val isRecording: Boolean = false,
        val spectralAnalysis: SpectralAnalysisUiState = SpectralAnalysisUiState.Idle,
        val environmentMix: EnvironmentMixUiState = EnvironmentMixUiState.Empty,
        val monthlyTrend: MonthlyTrendUiState = MonthlyTrendUiState.Empty,
        val yearlyReport: YearlyReportUiState = YearlyReportUiState.Empty,
    ) : AnalyticsUiState
}

enum class HealthStatus { SAFE, WARNING, DANGER }

data class DailyExposureUiState(val dayStartMs: Long, val avgDb: Float, val maxDb: Float, val isToday: Boolean = false)

enum class EnvironmentMixCategory { QUIET, MODERATE, LOUD, CRITICAL }

sealed interface EnvironmentMixUiState {
    data object LockedPreview : EnvironmentMixUiState

    data object Empty : EnvironmentMixUiState

    data class Data(val rows: List<EnvironmentMixRowUiState>) : EnvironmentMixUiState
}

data class EnvironmentMixRowUiState(val category: EnvironmentMixCategory, val percent: Int)

sealed interface MonthlyTrendUiState {
    data object LockedPreview : MonthlyTrendUiState

    data object Empty : MonthlyTrendUiState

    data class Data(val points: List<MonthlyTrendPointUiState>, val laeqDb: Float, val loudestDb: Float?) :
        MonthlyTrendUiState
}

data class MonthlyTrendPointUiState(val dayStartMs: Long, val laeqDb: Float?, val maxDb: Float?)

sealed interface YearlyReportUiState {
    data object LockedPreview : YearlyReportUiState

    data object Empty : YearlyReportUiState

    data class Data(
        val totalSessions: Int,
        val laeqDb: Float,
        val loudestDayLabel: String,
        val loudestDb: Float?,
        val zoneRows: List<EnvironmentMixRowUiState>,
    ) : YearlyReportUiState
}

sealed interface SpectralAnalysisUiState {
    data object LockedPreview : SpectralAnalysisUiState

    data object Idle : SpectralAnalysisUiState

    data class Live(
        val bands: List<SpectralBandUiState>,
        val dominantFrequencyHz: Float,
        val bandwidth: SpectralBandwidth,
    ) : SpectralAnalysisUiState
}

data class SpectralBandUiState(val normalizedAmplitude: Float)
