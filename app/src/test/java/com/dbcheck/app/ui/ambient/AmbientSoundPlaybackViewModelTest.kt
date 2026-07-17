package com.dbcheck.app.ui.ambient

import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.ambient.AmbientSoundPreset
import com.dbcheck.app.service.AmbientSoundPlaybackController
import com.dbcheck.app.service.AmbientSoundPlaybackRequest
import com.dbcheck.app.testStringContext
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AmbientSoundPlaybackViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferences = MutableStateFlow(UserPreferences(isProUser = true))
    private val preferencesRepository =
        mockk<PreferencesRepository> {
            every { userPreferences } returns preferences
        }
    private val playbackController =
        mockk<AmbientSoundPlaybackController> {
            every { isPlaying } returns MutableStateFlow(false)
            every { startPlayback(any()) } returns Unit
            every { stopPlayback() } returns Unit
        }

    @Test
    fun freeUserCannotStartPlayback() = runTest {
        preferences.value = UserPreferences(isProUser = false)
        val viewModel = createViewModel()
        runCurrent()

        viewModel.play(notificationPermissionGranted = true)

        assertTrue(viewModel.uiState.value.isLocked)
        assertFalse(viewModel.uiState.value.isPlaying)
        verify(exactly = 0) { playbackController.startPlayback(any()) }
    }

    @Test
    fun proUserStartsPlaybackOnlyFromPlayActionWithNotificationPermission() = runTest {
        preferences.value =
            UserPreferences(
                isProUser = true,
                ambientSoundPreset = AmbientSoundPreset.PINK_NOISE,
                ambientSoundVolume = 0.5f,
                ambientSoundTimerMinutes = 60,
            )
        val viewModel = createViewModel()
        runCurrent()

        viewModel.play(notificationPermissionGranted = true)

        verify(exactly = 1) {
            playbackController.startPlayback(
                AmbientSoundPlaybackRequest(
                    preset = AmbientSoundPreset.PINK_NOISE,
                    volume = 0.5f,
                    timerMinutes = 60,
                ),
            )
        }
    }

    @Test
    fun notificationPermissionDeniedPreventsPlaybackStart() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.play(notificationPermissionGranted = false)

        assertEquals("Notifications are required for ambient sound playback", viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.notificationPermissionDenied)
        verify(exactly = 0) { playbackController.startPlayback(any()) }
    }

    @Test
    fun notificationPermissionGrantClearsDeniedRecoveryState() = runTest {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.play(notificationPermissionGranted = false)

        viewModel.play(notificationPermissionGranted = true)

        assertFalse(viewModel.uiState.value.notificationPermissionDenied)
    }

    @Test
    fun proUserPreferenceUpdatesAreNormalizedAndPersisted() = runTest {
        coEvery { preferencesRepository.updateAmbientSoundPreset(any()) } returns Unit
        coEvery { preferencesRepository.updateAmbientSoundVolume(any()) } returns Unit
        coEvery { preferencesRepository.updateAmbientSoundTimerMinutes(any()) } returns Unit
        val viewModel = createViewModel()
        runCurrent()

        viewModel.updatePreset(AmbientSoundPreset.FAN)
        viewModel.updateVolume(2f)
        viewModel.updateTimerMinutes(7)

        coVerify(exactly = 1) {
            preferencesRepository.updateAmbientSoundPreset(AmbientSoundPreset.FAN)
            preferencesRepository.updateAmbientSoundVolume(1f)
            preferencesRepository.updateAmbientSoundTimerMinutes(30)
        }
    }

    @Test
    fun freeUserCannotPersistAmbientSettings() = runTest {
        preferences.value = UserPreferences(isProUser = false)
        val viewModel = createViewModel()
        runCurrent()

        viewModel.updatePreset(AmbientSoundPreset.FAN)
        viewModel.updateVolume(0.8f)
        viewModel.updateTimerMinutes(15)

        coVerify(exactly = 0) {
            preferencesRepository.updateAmbientSoundPreset(any())
            preferencesRepository.updateAmbientSoundVolume(any())
            preferencesRepository.updateAmbientSoundTimerMinutes(any())
        }
    }

    private fun createViewModel(): AmbientSoundPlaybackViewModel = AmbientSoundPlaybackViewModel(
            context = testStringContext(),
            preferencesRepository = preferencesRepository,
            playbackController = playbackController,
        )
}
