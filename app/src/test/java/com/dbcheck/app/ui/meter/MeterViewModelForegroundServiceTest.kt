package com.dbcheck.app.ui.meter

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.audio.AudioEngine
import com.dbcheck.app.domain.audio.AudioRecordingFailure
import com.dbcheck.app.domain.audio.DecibelReading
import com.dbcheck.app.service.AudioSessionManager
import com.dbcheck.app.service.MeasurementForegroundService
import com.dbcheck.app.service.SessionStats
import com.dbcheck.app.util.HapticFeedbackHelper
import com.dbcheck.app.util.ShareResultsGenerator
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MeterViewModelForegroundServiceTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val decibelReadings = MutableSharedFlow<DecibelReading>()
    private val sessionStats = MutableStateFlow(SessionStats())
    private val completedSessions = MutableSharedFlow<Long>()
    private val healthConnectSyncFailures = MutableSharedFlow<String>()
    private val recordingFailures = MutableSharedFlow<AudioRecordingFailure>()
    private val isRecording = MutableStateFlow(false)
    private val preferencesFlow = MutableStateFlow(UserPreferences())
    private val context = mockk<Context>(relaxed = true)
    private val audioEngine =
        mockk<AudioEngine> {
            every { decibelFlow } returns decibelReadings
        }
    private val audioSessionManager =
        mockk<AudioSessionManager>(relaxed = true) {
            every { sessionStats } returns this@MeterViewModelForegroundServiceTest.sessionStats
            every { completedSessionIds } returns completedSessions
            every {
                healthConnectSyncFailures
            } returns this@MeterViewModelForegroundServiceTest.healthConnectSyncFailures
            every { recordingFailures } returns this@MeterViewModelForegroundServiceTest.recordingFailures
            every { isRecording } returns this@MeterViewModelForegroundServiceTest.isRecording
        }
    private val preferencesRepository =
        mockk<PreferencesRepository> {
            every { userPreferences } returns preferencesFlow
        }
    private val hapticHelper = mockk<HapticFeedbackHelper>(relaxed = true)
    private val shareResultsGenerator = mockk<ShareResultsGenerator>()

    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
        unmockkObject(MeasurementForegroundService.Companion)
    }

    @Test
    fun toggleRecordingStartsForegroundServiceButDoesNotStartAudioSessionDirectly() = runTest {
        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) } returns
            PackageManager.PERMISSION_GRANTED
        val viewModel = createViewModel()
        viewModel.onMicPermissionResult(granted = true)

        viewModel.toggleRecording()

        verify(exactly = 1) { context.startForegroundService(any()) }
        coVerify(exactly = 0) { audioSessionManager.startSession() }
        assertFalse(viewModel.uiState.value.isRecording)
    }

    @Test
    fun recordingStateFromSessionManagerControlsUiRecordingState() = runTest {
        val viewModel = createViewModel()

        isRecording.value = true

        assertTrue(viewModel.uiState.value.isRecording)

        isRecording.value = false

        assertFalse(viewModel.uiState.value.isRecording)
    }

    @Test
    fun resetWhileRecordingStopsActiveSessionWithoutCompletionNavigation() = runTest {
        val stopIntent = mockk<Intent>()
        mockkObject(MeasurementForegroundService.Companion)
        every { MeasurementForegroundService.stopIntent(context, emitCompleted = false) } returns stopIntent
        val viewModel = createViewModel()
        isRecording.value = true

        viewModel.resetMeasurement()

        verify(exactly = 1) { context.startService(stopIntent) }
        verify(exactly = 0) { audioSessionManager.stopSession(emitCompleted = false) }
        verify(exactly = 0) { context.stopService(any()) }
        assertFalse(viewModel.uiState.value.isRecording)
    }

    private fun createViewModel(): MeterViewModel = MeterViewModel(
        context = context,
        audioEngine = audioEngine,
        audioSessionManager = audioSessionManager,
        preferencesRepository = preferencesRepository,
        hapticHelper = hapticHelper,
        shareResultsGenerator = shareResultsGenerator,
    )
}
