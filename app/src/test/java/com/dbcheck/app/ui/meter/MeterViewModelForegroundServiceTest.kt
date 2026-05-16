package com.dbcheck.app.ui.meter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.service.AudioSessionManager
import com.dbcheck.app.service.MeasurementForegroundService
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MeterViewModelForegroundServiceTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val flows = MeterViewModelTestFlows()
    private val dependencies = MeterViewModelTestDependencies(flows)
    private val audioSessionManager =
        mockk<AudioSessionManager>(relaxed = true) {
            every { sessionStats } returns flows.sessionStats
            every { completedSessionIds } returns flows.completedSessions
            every { healthConnectSyncFailures } returns flows.healthConnectSyncFailures
            every { isRecording } returns flows.isRecording
        }

    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
        unmockkObject(MeasurementForegroundService.Companion)
    }

    @Test
    fun toggleRecordingStartsForegroundServiceButDoesNotStartAudioSessionDirectly() = runTest {
        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(dependencies.context, Manifest.permission.RECORD_AUDIO) } returns
            PackageManager.PERMISSION_GRANTED
        val viewModel = createViewModel()
        viewModel.onMicPermissionResult(granted = true)

        viewModel.toggleRecording()

        verify(exactly = 1) { dependencies.context.startForegroundService(any()) }
        coVerify(exactly = 0) { audioSessionManager.startSession() }
        assertFalse(viewModel.uiState.value.isRecording)
    }

    @Test
    fun recordingStateFromSessionManagerControlsUiRecordingState() = runTest {
        val viewModel = createViewModel()

        flows.isRecording.value = true

        assertTrue(viewModel.uiState.value.isRecording)

        flows.isRecording.value = false

        assertFalse(viewModel.uiState.value.isRecording)
    }

    @Test
    fun resetWhileRecordingStopsActiveSessionWithoutCompletionNavigation() = runTest {
        val stopIntent = mockk<Intent>()
        mockkObject(MeasurementForegroundService.Companion)
        every { MeasurementForegroundService.stopIntent(dependencies.context, emitCompleted = false) } returns stopIntent
        val viewModel = createViewModel()
        flows.isRecording.value = true

        viewModel.resetMeasurement()

        verify(exactly = 1) { dependencies.context.startService(stopIntent) }
        verify(exactly = 0) { audioSessionManager.stopSession(emitCompleted = false) }
        verify(exactly = 0) { dependencies.context.stopService(any()) }
        assertFalse(viewModel.uiState.value.isRecording)
    }

    private fun createViewModel(): MeterViewModel = dependencies.createViewModel(audioSessionManager)
}
