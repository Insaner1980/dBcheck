package com.dbcheck.app.ui.meter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.dbcheck.app.MainDispatcherRule
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

    private val harness = MeterViewModelTestHarness()

    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
        unmockkObject(MeasurementForegroundService.Companion)
    }

    @Test
    fun toggleRecordingStartsForegroundServiceButDoesNotStartAudioSessionDirectly() = runTest {
        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(harness.context, Manifest.permission.RECORD_AUDIO) } returns
            PackageManager.PERMISSION_GRANTED
        val viewModel = createViewModel()
        viewModel.onMicPermissionResult(granted = true)

        viewModel.toggleRecording()

        verify(exactly = 1) { harness.context.startForegroundService(any()) }
        coVerify(exactly = 0) { harness.audioSessionManager.startSession() }
        assertFalse(viewModel.uiState.value.isRecording)
    }

    @Test
    fun recordingStateFromSessionManagerControlsUiRecordingState() = runTest {
        val viewModel = createViewModel()

        harness.isRecording.value = true

        assertTrue(viewModel.uiState.value.isRecording)

        harness.isRecording.value = false

        assertFalse(viewModel.uiState.value.isRecording)
    }

    @Test
    fun resetWhileRecordingStopsActiveSessionWithoutCompletionNavigation() = runTest {
        val stopIntent = mockk<Intent>()
        mockkObject(MeasurementForegroundService.Companion)
        every { MeasurementForegroundService.stopIntent(harness.context, emitCompleted = false) } returns stopIntent
        val viewModel = createViewModel()
        harness.isRecording.value = true

        viewModel.resetMeasurement()

        verify(exactly = 1) { harness.context.startService(stopIntent) }
        verify(exactly = 0) { harness.audioSessionManager.stopSession(emitCompleted = false) }
        verify(exactly = 0) { harness.context.stopService(any()) }
        assertFalse(viewModel.uiState.value.isRecording)
    }

    private fun createViewModel(): MeterViewModel = harness.createViewModel()
}
