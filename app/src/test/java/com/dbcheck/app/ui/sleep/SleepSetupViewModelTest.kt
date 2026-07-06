package com.dbcheck.app.ui.sleep

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.service.AudioSessionManager
import com.dbcheck.app.service.MeasurementForegroundService
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SleepSetupViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferences = MutableStateFlow(UserPreferences(isProUser = false))
    private val preferencesRepository =
        mockk<PreferencesRepository> {
            every { userPreferences } returns preferences
        }
    private val isRecording = MutableStateFlow(false)
    private val activeSessionStartTimeMs = MutableStateFlow<Long?>(null)
    private val audioSessionManager =
        mockk<AudioSessionManager>(relaxed = true) {
            every { isRecording } returns this@SleepSetupViewModelTest.isRecording
            every { activeSessionStartTimeMs } returns this@SleepSetupViewModelTest.activeSessionStartTimeMs
        }
    private val context = mockk<Context>(relaxed = true)

    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
        unmockkObject(MeasurementForegroundService.Companion)
    }

    @Test
    fun freeUserGetsLockedExecutionState() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        assertEquals(SleepSetupAvailability.Locked, viewModel.uiState.value.availability)
    }

    @Test
    fun proUserGetsReadyExecutionState() = runTest {
        preferences.value = UserPreferences(isProUser = true)

        val viewModel = createViewModel()
        runCurrent()

        assertEquals(SleepSetupAvailability.Ready, viewModel.uiState.value.availability)
    }

    @Test
    fun sleepSetupAvailabilityIsMappedFromProEntitlementNotSleepCardVisibilitySetting() = runTest {
        preferences.value = UserPreferences(isProUser = true, sleepCardEnabled = false)

        val viewModel = createViewModel()
        runCurrent()

        assertEquals(SleepSetupDefaults.DURATION_OPTIONS_MINUTES, viewModel.uiState.value.durationOptionsMinutes)
        assertEquals(SleepSetupDefaults.DEFAULT_DURATION_MINUTES, viewModel.uiState.value.targetDurationMinutes)
        assertEquals(false, viewModel.uiState.value.keepAwakeEnabled)
    }

    @Test
    fun proUserCanPrepareSleepSessionOptions() = runTest {
        preferences.value = UserPreferences(isProUser = true)
        val viewModel = createViewModel()
        runCurrent()

        viewModel.updateTargetDurationMinutes(600)
        viewModel.updateKeepAwakeEnabled(true)

        assertEquals(600, viewModel.uiState.value.targetDurationMinutes)
        assertEquals(true, viewModel.uiState.value.keepAwakeEnabled)
    }

    @Test
    fun unsupportedSleepDurationIsIgnored() = runTest {
        preferences.value = UserPreferences(isProUser = true)
        val viewModel = createViewModel()
        runCurrent()

        viewModel.updateTargetDurationMinutes(123)

        assertEquals(SleepSetupDefaults.DEFAULT_DURATION_MINUTES, viewModel.uiState.value.targetDurationMinutes)
    }

    @Test
    fun freeUserCannotPrepareSleepSessionOptions() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.updateTargetDurationMinutes(600)
        viewModel.updateKeepAwakeEnabled(true)

        assertEquals(SleepSetupDefaults.DEFAULT_DURATION_MINUTES, viewModel.uiState.value.targetDurationMinutes)
        assertEquals(false, viewModel.uiState.value.keepAwakeEnabled)
    }

    @Test
    fun startSleepRecordingUsesPreparedOptionsAndForegroundService() = runTest {
        preferences.value = UserPreferences(isProUser = true)
        mockkStatic(ContextCompat::class)
        mockkObject(MeasurementForegroundService.Companion)
        val sleepIntent = mockk<Intent>()
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) } returns
            PackageManager.PERMISSION_GRANTED
        every { MeasurementForegroundService.startSleepIntent(context, 600, true) } returns sleepIntent
        every { context.startForegroundService(sleepIntent) } returns null
        val viewModel = createViewModel()
        runCurrent()

        viewModel.updateTargetDurationMinutes(600)
        viewModel.updateKeepAwakeEnabled(true)
        viewModel.startSleepRecording()

        verify(exactly = 1) { context.startForegroundService(sleepIntent) }
    }

    @Test
    fun activeSleepRecordingStateFollowsSessionManager() = runTest {
        preferences.value = UserPreferences(isProUser = true)
        val viewModel = createViewModel()
        runCurrent()

        isRecording.value = true

        assertEquals(true, viewModel.uiState.value.isRecording)

        isRecording.value = false

        assertEquals(false, viewModel.uiState.value.isRecording)
    }

    private fun createViewModel(): SleepSetupViewModel = SleepSetupViewModel(
            context = context,
            preferencesRepository = preferencesRepository,
            audioSessionManager = audioSessionManager,
        )
}
