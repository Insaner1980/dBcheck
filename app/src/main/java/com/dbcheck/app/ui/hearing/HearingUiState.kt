package com.dbcheck.app.ui.hearing

import com.dbcheck.app.domain.hearing.HearingHealthSummary
import com.dbcheck.app.domain.hearingtest.HearingRecoveryStatus
import com.dbcheck.app.domain.tinnitus.TinnitusPitchProfile

data class HearingUiState(
    val isProUser: Boolean = false,
    val hearingHealthSummary: HearingHealthSummary? = null,
    val latestHearingTest: HearingTestUiState = HearingTestUiState.NoResult,
    val hearingRecovery: HearingRecoveryUiState = HearingRecoveryUiState.LockedPreview,
    val tinnitusPitchProfile: TinnitusPitchProfile = TinnitusPitchProfile(),
    val sleepCardVisible: Boolean = false,
    val voiceBaselineLevelDb: Float? = null,
    val voiceBaselineSampleCount: Int = 0,
    val voiceBaselineCapturedAtMs: Long? = null,
    val isRecording: Boolean = false,
    val soundDetectionEnabled: Boolean = false,
) {
    val canCalibrateVoiceBaseline: Boolean
        get() = isProUser && isRecording && soundDetectionEnabled
}

sealed interface HearingTestUiState {
    data object NoResult : HearingTestUiState

    data class Result(
        val id: Long,
        val timestamp: Long,
        val overallScore: Int,
        val rating: String,
        val avgThreshold: Float,
    ) : HearingTestUiState
}

sealed interface HearingRecoveryUiState {
    data object LockedPreview : HearingRecoveryUiState

    data object MissingBaseline : HearingRecoveryUiState

    data object Ready : HearingRecoveryUiState

    data class Result(
        val averageShiftDb: Float,
        val maxShiftDb: Float,
        val status: HearingRecoveryStatus,
        val timestamp: Long,
    ) : HearingRecoveryUiState
}
