package com.dbcheck.app.ui.analytics

import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.HearingRecoveryRepository
import com.dbcheck.app.data.repository.HearingTestRepository
import com.dbcheck.app.data.repository.MeasurementRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.data.repository.SessionRepository
import com.dbcheck.app.domain.analytics.EnvironmentExposureMixCounts
import com.dbcheck.app.domain.audio.RtaFrame
import com.dbcheck.app.domain.audio.SoundDetectionState
import com.dbcheck.app.domain.audio.SpectralFrame
import com.dbcheck.app.domain.hearingtest.HearingRecoveryResult
import com.dbcheck.app.domain.hearingtest.HearingTestResult
import com.dbcheck.app.service.AudioEngine
import com.dbcheck.app.service.AudioSessionManager
import com.dbcheck.app.testStringContext
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

internal class AnalyticsViewModelTestFixture(
    private val defaultDispatcher: CoroutineDispatcher,
    val preferences: MutableStateFlow<UserPreferences> = MutableStateFlow(UserPreferences(isProUser = true)),
    val latestBaseline: MutableStateFlow<HearingTestResult?> = MutableStateFlow(null),
    val latestRecovery: MutableStateFlow<HearingRecoveryResult?> = MutableStateFlow(null),
) {
    val isRecordingFlow = MutableStateFlow(false)
    val liveEnvironmentMixCountsFlow = MutableStateFlow(EnvironmentExposureMixCounts())
    val soundDetectionStateFlow = MutableStateFlow(SoundDetectionState())
    val spectralFrameFlow = MutableStateFlow<SpectralFrame?>(null)
    val rtaFrameFlow = MutableStateFlow<RtaFrame?>(null)
    val measurementRepository =
        mockk<MeasurementRepository> {
            every { getDailyAveragesLast7Days() } returns flowOf(emptyList())
            every { getEnvironmentMixLast7Days() } returns flowOf(EnvironmentExposureMixCounts())
            every { getWeightedMeasurementsInRange(any(), any()) } answers { flowOf(emptyList()) }
        }
    val sessionRepository =
        mockk<SessionRepository> {
            every { getCompletedSessionCountInRange(any(), any()) } answers { flowOf(0) }
        }
    private val preferencesRepository =
        mockk<PreferencesRepository> {
            every { userPreferences } returns preferences
        }
    private val audioSessionManager =
        mockk<AudioSessionManager> {
            every { isRecording } returns isRecordingFlow
            every { liveEnvironmentMixCounts } returns liveEnvironmentMixCountsFlow
            every { soundDetectionState } returns soundDetectionStateFlow
        }
    private val audioEngine =
        mockk<AudioEngine> {
            every { spectralFrame } returns spectralFrameFlow
            every { rtaFrame } returns rtaFrameFlow
        }
    private val hearingTestRepository =
        mockk<HearingTestRepository> {
            every { getLatestResult() } returns latestBaseline
        }
    private val hearingRecoveryRepository =
        mockk<HearingRecoveryRepository> {
            every { getLatestResult() } returns latestRecovery
        }

    fun createViewModel(): AnalyticsViewModel = AnalyticsViewModel(
        context = testStringContext(),
        measurementRepository = measurementRepository,
        sessionRepository = sessionRepository,
        preferencesRepository = preferencesRepository,
        audioSessionManager = audioSessionManager,
        audioEngine = audioEngine,
        hearingTestRepository = hearingTestRepository,
        hearingRecoveryRepository = hearingRecoveryRepository,
        defaultDispatcher = defaultDispatcher,
    )
}
