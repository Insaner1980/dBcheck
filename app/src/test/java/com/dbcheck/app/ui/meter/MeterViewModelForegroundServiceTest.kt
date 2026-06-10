package com.dbcheck.app.ui.meter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.projectFile
import com.dbcheck.app.service.MeasurementForegroundService
import com.dbcheck.app.ui.meter.state.MeasurementMode
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
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
    fun recordingDurationUsesActiveSessionStartTimeWhenViewModelReconnects() = runTest {
        val sessionStartTimeMs = System.currentTimeMillis() - 5_000L
        harness.activeSessionStartTimeMs.value = sessionStartTimeMs
        val viewModel = createViewModel()

        try {
            harness.isRecording.value = true

            assertTrue(viewModel.uiState.value.sessionDurationMs >= 4_000L)
        } finally {
            harness.isRecording.value = false
        }
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

    @Test
    fun measurementModeSwitchUpdatesStateWithoutStartingOrStoppingMeasurement() = runTest {
        val viewModel = createViewModel()

        assertTrue(viewModel.uiState.value.measurementMode == MeasurementMode.DB_METER)

        viewModel.setMeasurementMode(MeasurementMode.DOSIMETER)

        assertTrue(viewModel.uiState.value.measurementMode == MeasurementMode.DOSIMETER)
        verify(exactly = 0) { harness.context.startForegroundService(any()) }
        verify(exactly = 0) { harness.context.startService(any()) }
        coVerify(exactly = 0) { harness.audioSessionManager.startSession() }
        verify(exactly = 0) { harness.audioSessionManager.stopSession(any()) }
    }

    @Test
    fun proPreferenceControlsMeterModeChipGateState() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        assertFalse(viewModel.uiState.value.isProUser)

        harness.preferencesFlow.value = UserPreferences(isProUser = true)
        runCurrent()

        assertTrue(viewModel.uiState.value.isProUser)
    }

    @Test
    fun viewModelClearDoesNotOwnForegroundMeasurementServiceStop() {
        val onClearedBlock =
            projectFile("src/main/java/com/dbcheck/app/ui/meter/MeterViewModel.kt")
                .readText()
                .substringAfter("override fun onCleared()")

        assertFalse(onClearedBlock.contains("stopService"))
    }

    private fun createViewModel(): MeterViewModel = harness.createViewModel()
}
