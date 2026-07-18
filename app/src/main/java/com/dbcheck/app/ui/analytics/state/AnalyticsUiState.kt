package com.dbcheck.app.ui.analytics.state

import com.dbcheck.app.domain.audio.SpectralBandwidth
import com.dbcheck.app.domain.hearing.HearingHealthSummary

sealed interface AnalyticsUiState {
    data object Loading : AnalyticsUiState

    data object Empty : AnalyticsUiState

    data class Error(val message: String) : AnalyticsUiState

    data class Success(
        val weeklyAverageDb: Float = 0f,
        val dailyAverages: List<DailyExposureUiState> = emptyList(),
        val hearingHealthSummary: HearingHealthSummary? = null,
        val isProUser: Boolean = false,
        val hasExposureData: Boolean = true,
        val isRecording: Boolean = false,
        val selectedSection: AnalyticsSection = AnalyticsSection.OVERVIEW,
        val selectedOverviewRange: AnalyticsOverviewRange = AnalyticsOverviewRange.WEEKLY,
        val selectedSpectralMode: SpectralMode = SpectralMode.BARS,
        val spectralAnalysis: SpectralAnalysisUiState = SpectralAnalysisUiState.Idle,
        val spectrogram: SpectrogramUiState = SpectrogramUiState.Empty,
        val rta: RtaUiState = RtaUiState.Empty,
        val environmentMix: EnvironmentMixUiState = EnvironmentMixUiState.Empty,
        val activeEnvironmentMix: EnvironmentMixUiState = EnvironmentMixUiState.Empty,
        val soundDetection: SoundDetectionUiState = SoundDetectionUiState.Idle,
        val soundDetectionEnabled: Boolean = false,
        val monthlyTrend: MonthlyTrendUiState = MonthlyTrendUiState.Empty,
        val yearlyReport: YearlyReportUiState = YearlyReportUiState.Empty,
    ) : AnalyticsUiState
}

enum class AnalyticsSection { OVERVIEW, SPECTRAL, ENVIRONMENT }

enum class AnalyticsOverviewRange { WEEKLY, MONTHLY }

enum class SpectralMode { BARS, SPECTROGRAM, RTA }

data class DailyExposureUiState(val dayStartMs: Long, val avgDb: Float, val maxDb: Float, val isToday: Boolean = false)

enum class EnvironmentMixCategory { QUIET, MODERATE, LOUD, CRITICAL }

sealed interface EnvironmentMixUiState {
    data object LockedPreview : EnvironmentMixUiState

    data object Empty : EnvironmentMixUiState

    data class Data(val rows: List<EnvironmentMixRowUiState>) : EnvironmentMixUiState
}

data class EnvironmentMixRowUiState(val category: EnvironmentMixCategory, val percent: Int)

sealed interface SoundDetectionUiState {
    data object LockedPreview : SoundDetectionUiState

    data object Idle : SoundDetectionUiState

    data class Live(
        val label: String,
        val confidencePercent: Int,
        val recentDetections: List<SoundDetectionChipUiState>,
    ) : SoundDetectionUiState

    data class Error(val message: String) : SoundDetectionUiState
}

data class SoundDetectionChipUiState(val label: String, val confidencePercent: Int)

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

data class SpectralBandUiState(val normalizedAmplitude: Float, val centerFrequencyHz: Float = 0f)

sealed interface SpectrogramUiState {
    data object LockedPreview : SpectrogramUiState

    data object Empty : SpectrogramUiState

    data class Data(val rows: List<SpectrogramRowUiState>) : SpectrogramUiState
}

data class SpectrogramRowUiState(val timestampMs: Long, val bands: List<SpectralBandUiState>)

sealed interface RtaUiState {
    data object LockedPreview : RtaUiState

    data object Empty : RtaUiState

    data class Data(val bands: List<RtaBandUiState>) : RtaUiState
}

data class RtaBandUiState(val centerFrequencyHz: Float, val normalizedAmplitude: Float)
