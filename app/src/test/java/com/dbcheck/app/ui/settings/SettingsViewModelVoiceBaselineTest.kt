package com.dbcheck.app.ui.settings

import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.domain.voice.VoiceBaselineCapture
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelVoiceBaselineTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun proUserCanPersistVoiceBaselineCapturedFromSessionManager() = runTest {
        val harness =
            SettingsViewModelTestHarness(
                UserPreferences(
                    isProUser = true,
                    soundDetectionEnabled = true,
                ),
            )
        harness.recordingFlow.value = true
        every { harness.audioSessionManager.captureVoiceBaseline(isProUser = true) } returns
            VoiceBaselineCapture(
                levelDb = 68.5f,
                sampleCount = 7,
                capturedAtMs = 1_700_000_000_000L,
            )
        val viewModel = harness.createViewModel()

        viewModel.calibrateVoiceBaseline()
        advanceUntilIdle()

        coVerify {
            harness.preferencesRepository.updateVoiceBaseline(
                levelDb = 68.5f,
                sampleCount = 7,
                capturedAtMs = 1_700_000_000_000L,
            )
        }
    }

    @Test
    fun freeUserCannotCaptureOrPersistVoiceBaseline() = runTest {
        val harness = SettingsViewModelTestHarness(UserPreferences(isProUser = false))
        harness.recordingFlow.value = true
        val viewModel = harness.createViewModel()

        viewModel.calibrateVoiceBaseline()
        advanceUntilIdle()

        verify(exactly = 0) { harness.audioSessionManager.captureVoiceBaseline(any()) }
        coVerify(exactly = 0) { harness.preferencesRepository.updateVoiceBaseline(any(), any(), any()) }
    }

    @Test
    fun voiceBaselineUiStateIsEffectiveAndRequiresRecordingWithSoundDetection() = runTest {
        val harness =
            SettingsViewModelTestHarness(
                UserPreferences(
                    isProUser = true,
                    soundDetectionEnabled = true,
                    voiceBaselineLevelDb = 68.5f,
                    voiceBaselineSampleCount = 7,
                    voiceBaselineCapturedAtMs = 1_700_000_000_000L,
                ),
            )
        val viewModel = harness.createViewModel()

        assertEquals(68.5f, viewModel.uiState.value.voiceBaselineLevelDb ?: 0f, 0f)
        assertEquals(7, viewModel.uiState.value.voiceBaselineSampleCount)
        assertEquals(1_700_000_000_000L, viewModel.uiState.value.voiceBaselineCapturedAtMs)
        assertFalse(viewModel.uiState.value.canCalibrateVoiceBaseline)

        harness.recordingFlow.value = true
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.canCalibrateVoiceBaseline)
    }
}
