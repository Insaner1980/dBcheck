package com.dbcheck.app.ui.analytics

import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.clearForTest
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.MeasurementRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.data.repository.SessionRepository
import com.dbcheck.app.domain.analytics.EnvironmentExposureMixCounts
import com.dbcheck.app.domain.audio.AudioEngine
import com.dbcheck.app.domain.audio.RtaFrame
import com.dbcheck.app.domain.audio.SoundDetectionState
import com.dbcheck.app.domain.audio.SpectralFrame
import com.dbcheck.app.service.AudioSessionManager
import com.dbcheck.app.testStringContext
import com.dbcheck.app.ui.analytics.state.AnalyticsUiState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelRollingWindowTest {
    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val preferences = MutableStateFlow(UserPreferences(isProUser = true))
    private val isRecordingFlow = MutableStateFlow(false)
    private val liveEnvironmentMixCountsFlow = MutableStateFlow(EnvironmentExposureMixCounts())
    private val soundDetectionStateFlow = MutableStateFlow(SoundDetectionState())
    private val spectralFrameFlow = MutableStateFlow<SpectralFrame?>(null)
    private val rtaFrameFlow = MutableStateFlow<RtaFrame?>(null)
    private val measurementRepository =
        mockk<MeasurementRepository> {
            every { getDailyAveragesLast7Days() } returns flowOf(emptyList())
            every { getEnvironmentMixLast7Days() } returns flowOf(EnvironmentExposureMixCounts())
            every { getWeightedMeasurementsInRange(any(), any()) } answers { flowOf(emptyList()) }
        }
    private val sessionRepository =
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

    @Test
    fun proExposureQueriesRefreshRollingWindowsWhileCollected() = runTest(testDispatcher.scheduler) {
        val viewModel = createViewModel()
        try {
            runCurrent()

            verify(exactly = 2) { measurementRepository.getWeightedMeasurementsInRange(any(), any()) }
            verify(exactly = 1) { sessionRepository.getCompletedSessionCountInRange(any(), any()) }

            advanceTimeBy(ROLLING_WINDOW_REFRESH_MS)
            runCurrent()

            verify(exactly = 4) { measurementRepository.getWeightedMeasurementsInRange(any(), any()) }
            verify(exactly = 2) { sessionRepository.getCompletedSessionCountInRange(any(), any()) }
        } finally {
            viewModel.clearForTest()
            runCurrent()
        }
    }

    @Test
    fun analyticsLoadFailureShowsErrorState() = runTest(testDispatcher.scheduler) {
        every { measurementRepository.getDailyAveragesLast7Days() } returns
            flow { throw IllegalStateException("db") }

        val viewModel = createViewModel()
        try {
            runCurrent()

            assertEquals(
                AnalyticsUiState.Error("Unable to load analytics"),
                viewModel.uiState.value,
            )
        } finally {
            viewModel.clearForTest()
            runCurrent()
        }
    }

    private fun createViewModel(): AnalyticsViewModel = AnalyticsViewModel(
        context = testStringContext(),
        measurementRepository = measurementRepository,
        sessionRepository = sessionRepository,
        preferencesRepository = preferencesRepository,
        audioSessionManager = audioSessionManager,
        audioEngine = audioEngine,
    )

    private companion object {
        const val ROLLING_WINDOW_REFRESH_MS = 60_000L
    }
}
