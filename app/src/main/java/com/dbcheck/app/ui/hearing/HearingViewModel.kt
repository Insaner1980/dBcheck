package com.dbcheck.app.ui.hearing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.HearingRecoveryRepository
import com.dbcheck.app.data.repository.HearingTestRepository
import com.dbcheck.app.data.repository.MeasurementRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.analytics.DailyExposureAverage
import com.dbcheck.app.domain.hearing.HearingHealthSummaryCalculator
import com.dbcheck.app.domain.hearingtest.HearingRecoveryResult
import com.dbcheck.app.domain.hearingtest.HearingTestResult
import com.dbcheck.app.domain.tinnitus.TinnitusPitchProfile
import com.dbcheck.app.service.AudioSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class HearingViewModel
    @Inject
    constructor(
        private val preferencesRepository: PreferencesRepository,
        private val measurementRepository: MeasurementRepository,
        private val hearingTestRepository: HearingTestRepository,
        private val hearingRecoveryRepository: HearingRecoveryRepository,
        private val audioSessionManager: AudioSessionManager,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(HearingUiState())
        val uiState: StateFlow<HearingUiState> = _uiState

        init {
            collectHearingState()
        }

        fun calibrateVoiceBaseline() {
            val state = _uiState.value
            if (!state.isProUser || !audioSessionManager.isRecording.value || !state.soundDetectionEnabled) return
            val capture = audioSessionManager.captureVoiceBaseline(isProUser = true) ?: return

            viewModelScope.launch {
                preferencesRepository.updateVoiceBaseline(
                    levelDb = capture.levelDb,
                    sampleCount = capture.sampleCount,
                    capturedAtMs = capture.capturedAtMs,
                )
            }
        }

        private fun collectHearingState() {
            viewModelScope.launch {
                combine(
                    preferencesRepository.userPreferences,
                    measurementRepository.getDailyAveragesLast7Days(),
                    hearingTestRepository.getLatestResult(),
                    hearingRecoveryRepository.getLatestResult(),
                    audioSessionManager.isRecording,
                ) { preferences, dailyAverages, latestHearingTest, latestRecovery, isRecording ->
                    buildUiState(
                        preferences = preferences,
                        dailyAverages = dailyAverages,
                        latestHearingTest = latestHearingTest,
                        latestRecovery = latestRecovery,
                        isRecording = isRecording,
                    )
                }.collect { state -> _uiState.value = state }
            }
        }

        private fun buildUiState(
            preferences: UserPreferences,
            dailyAverages: List<DailyExposureAverage>,
            latestHearingTest: HearingTestResult?,
            latestRecovery: HearingRecoveryResult?,
            isRecording: Boolean,
        ): HearingUiState {
            val isProUser = preferences.isProUser
            return HearingUiState(
                isProUser = isProUser,
                hearingHealthSummary =
                    HearingHealthSummaryCalculator.calculate(
                        dailyAverages = dailyAverages,
                        nowMs = System.currentTimeMillis(),
                        zoneId = ZoneId.systemDefault(),
                    ),
                latestHearingTest = latestHearingTest.toUiState(isProUser),
                hearingRecovery = mapHearingRecoveryState(isProUser, latestHearingTest, latestRecovery),
                tinnitusPitchProfile =
                    if (isProUser) {
                        preferences.tinnitusPitchProfile
                    } else {
                        TinnitusPitchProfile()
                    },
                sleepCardVisible = isProUser && preferences.sleepCardEnabled,
                voiceBaselineLevelDb = preferences.voiceBaselineLevelDb.takeIf { isProUser },
                voiceBaselineSampleCount =
                    if (isProUser && preferences.voiceBaselineLevelDb != null) {
                        preferences.voiceBaselineSampleCount
                    } else {
                        0
                    },
                voiceBaselineCapturedAtMs = preferences.voiceBaselineCapturedAtMs.takeIf { isProUser },
                isRecording = isRecording,
                soundDetectionEnabled = isProUser && preferences.soundDetectionEnabled,
            )
        }
    }

private fun HearingTestResult?.toUiState(isProUser: Boolean): HearingTestUiState = if (!isProUser || this == null) {
    HearingTestUiState.NoResult
} else {
    HearingTestUiState.Result(
        id = id,
        timestamp = timestamp,
        overallScore = overallScore,
        rating = rating,
        avgThreshold = avgThreshold,
    )
}

private fun mapHearingRecoveryState(
    isProUser: Boolean,
    latestBaseline: HearingTestResult?,
    latestRecovery: HearingRecoveryResult?,
): HearingRecoveryUiState = when {
    !isProUser -> HearingRecoveryUiState.LockedPreview

    latestBaseline == null -> HearingRecoveryUiState.MissingBaseline

    latestRecovery == null -> HearingRecoveryUiState.Ready

    else ->
        HearingRecoveryUiState.Result(
            averageShiftDb = latestRecovery.averageShiftDb,
            maxShiftDb = latestRecovery.maxShiftDb,
            status = latestRecovery.status,
            timestamp = latestRecovery.timestamp,
        )
}
