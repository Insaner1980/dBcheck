package com.dbcheck.app.ui.settings

import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.data.local.preferences.model.UserPreferenceDefaults
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.local.preferences.model.WaveformStyle
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.audio.ResponseTime
import com.dbcheck.app.domain.noise.DosimeterStandard
import com.dbcheck.app.service.AudioSessionManager
import com.dbcheck.app.sync.HealthConnectManager
import com.dbcheck.app.sync.HealthConnectStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelDisplayPreferenceTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferencesFlow =
        MutableStateFlow(
                UserPreferences(
                    isProUser = true,
                    responseTime = ResponseTime.SLOW,
                    dosimeterStandard = DosimeterStandard.OSHA_PEL,
                    waveformStyle = WaveformStyle.BARS,
                refreshRate = MeterRefreshRate.LOW,
            ),
        )
    private val preferencesRepository =
        mockk<PreferencesRepository> {
            every { userPreferences } returns preferencesFlow
            coEvery { updateExposureAlerts(any()) } just runs
            coEvery { updatePeakWarnings(any()) } just runs
            coEvery { updateNotificationThreshold(any()) } just runs
            coEvery { updateMicSensitivityOffset(any()) } just runs
            coEvery { updateFrequencyWeighting(any()) } just runs
            coEvery { updateResponseTime(any()) } just runs
            coEvery { updateDosimeterStandard(any()) } just runs
            coEvery { updateWaveformStyle(any()) } just runs
            coEvery { updateRefreshRate(any()) } just runs
            coEvery { updateLockscreenMeterEnabled(any()) } just runs
            coEvery { updateShowLockscreenMeterPublicly(any()) } just runs
            coEvery { updateHealthConnectEnabled(any()) } just runs
            coEvery { updateHeartRateOverlayEnabled(any()) } just runs
            coEvery { updateThemeMode(any()) } just runs
            coEvery { updateTechnicalMetadataEnabled(any()) } just runs
            coEvery { updateDosimeterCardEnabled(any()) } just runs
            coEvery { updateWavRecordingDefaultEnabled(any()) } just runs
            coEvery { updateAudibleAlarmEnabled(any()) } just runs
            coEvery { updateTtsRiskPromptEnabled(any()) } just runs
            coEvery { updateSoundDetectionEnabled(any()) } just runs
            coEvery { updateSleepCardEnabled(any()) } just runs
        }
    private val healthConnectManager =
        mockk<HealthConnectManager> {
            coEvery { getStatus() } returns HealthConnectStatus()
        }
    private val audioSessionManager =
        mockk<AudioSessionManager> {
            every { isRecording } returns MutableStateFlow(false)
            every { previewAudibleAlarm(any()) } returns Unit
        }

    @Test
    fun displayPreferencesAreMappedIntoUiState() = runTest {
            val viewModel = createViewModel()

            assertEquals(WaveformStyle.BARS, viewModel.uiState.value.waveformStyle)
            assertEquals(MeterRefreshRate.LOW, viewModel.uiState.value.refreshRate)
            assertEquals(ResponseTime.SLOW, viewModel.uiState.value.responseTime)
            assertEquals(DosimeterStandard.OSHA_PEL, viewModel.uiState.value.dosimeterStandard)
            assertEquals(true, viewModel.uiState.value.technicalMetadataEnabled)
            assertEquals(true, viewModel.uiState.value.dosimeterCardEnabled)
            assertEquals(false, viewModel.uiState.value.soundDetectionEnabled)
            assertEquals(false, viewModel.uiState.value.sleepCardEnabled)
        }

    @Test
    fun displayPreferenceUpdatesPersistSelectedValues() = runTest {
            val viewModel = createViewModel()

            viewModel.updateDisplayPreference(DisplayPreferenceUpdate.WaveformStyleChange(WaveformStyle.FILLED))
            viewModel.updateDisplayPreference(DisplayPreferenceUpdate.RefreshRateChange(MeterRefreshRate.HIGH))

            coVerify { preferencesRepository.updateWaveformStyle(WaveformStyle.FILLED) }
            coVerify { preferencesRepository.updateRefreshRate(MeterRefreshRate.HIGH) }
        }

    @Test
    fun featureToggleUpdatesPersistSelectedValues() = runTest {
            preferencesFlow.value = UserPreferences(isProUser = true)
            val viewModel = createViewModel()

            viewModel.updateFeatureToggle(FeatureToggleUpdate.TechnicalMetadata(false))
            viewModel.updateFeatureToggle(FeatureToggleUpdate.DosimeterCard(false))
            viewModel.updateFeatureToggle(FeatureToggleUpdate.SoundDetection(true))
            viewModel.updateFeatureToggle(FeatureToggleUpdate.SleepCard(true))

            coVerify { preferencesRepository.updateTechnicalMetadataEnabled(false) }
            coVerify { preferencesRepository.updateDosimeterCardEnabled(false) }
            coVerify { preferencesRepository.updateSoundDetectionEnabled(true) }
            coVerify { preferencesRepository.updateSleepCardEnabled(true) }
        }

    @Test
    fun noiseNotificationUpdatesPersistSelectedValues() = runTest {
            val viewModel = createViewModel()

            viewModel.updateNoiseNotification(NoiseNotificationUpdate.ExposureAlerts(true))
            viewModel.updateNoiseNotification(NoiseNotificationUpdate.PeakWarnings(true))
            viewModel.updateNoiseNotification(NoiseNotificationUpdate.NotificationThreshold(90))
            viewModel.updateNoiseNotification(NoiseNotificationUpdate.AudibleAlarm(true))
            viewModel.updateNoiseNotification(NoiseNotificationUpdate.TtsRiskPrompt(true))

            coVerify { preferencesRepository.updateExposureAlerts(true) }
            coVerify { preferencesRepository.updatePeakWarnings(true) }
            coVerify { preferencesRepository.updateNotificationThreshold(90) }
            coVerify { preferencesRepository.updateAudibleAlarmEnabled(true) }
            coVerify { preferencesRepository.updateTtsRiskPromptEnabled(true) }
        }

    @Test
    fun notificationThresholdUpdateIsClampedBeforePersisting() = runTest {
            val viewModel = createViewModel()

            viewModel.updateNoiseNotification(NoiseNotificationUpdate.NotificationThreshold(130))

            coVerify { preferencesRepository.updateNotificationThreshold(110) }
        }

    @Test
    fun stringPreferenceUpdatesAreNormalizedBeforePersisting() = runTest {
            preferencesFlow.value = UserPreferences(isProUser = true)
            val viewModel = createViewModel()

            viewModel.updateThemeMode("midnight")
            viewModel.updateFrequencyWeighting("Q")
            viewModel.updateResponseTime(ResponseTime.IMPULSE)
            viewModel.updateDosimeterStandard(DosimeterStandard.OSHA_PEL)

            coVerify { preferencesRepository.updateThemeMode("system") }
            coVerify { preferencesRepository.updateFrequencyWeighting("A") }
            coVerify { preferencesRepository.updateResponseTime(ResponseTime.IMPULSE) }
            coVerify { preferencesRepository.updateDosimeterStandard(DosimeterStandard.OSHA_PEL) }
        }

    @Test
    fun disablingHealthConnectKeepsHeartRateOverlayPreference() = runTest {
            val viewModel = createViewModel()

            viewModel.updateHealthConnectEnabled(false)

            coVerify { preferencesRepository.updateHealthConnectEnabled(false) }
            coVerify(exactly = 0) { preferencesRepository.updateHeartRateOverlayEnabled(any()) }
        }

    @Test
    fun freeUserCannotPersistProAudioCalibrationValues() = runTest {
            preferencesFlow.value = UserPreferences(isProUser = false)
            val viewModel = createViewModel()

            viewModel.updateMicSensitivity(6f)
            viewModel.updateFrequencyWeighting("C")
            viewModel.updateResponseTime(ResponseTime.SLOW)
            viewModel.updateDosimeterStandard(DosimeterStandard.OSHA_PEL)

            coVerify(exactly = 0) { preferencesRepository.updateMicSensitivityOffset(any()) }
            coVerify(exactly = 0) { preferencesRepository.updateFrequencyWeighting(any()) }
            coVerify(exactly = 0) { preferencesRepository.updateResponseTime(any()) }
            coVerify(exactly = 0) { preferencesRepository.updateDosimeterStandard(any()) }
        }

    @Test
    fun freeUserCannotPersistProOnlyToggles() = runTest {
            preferencesFlow.value = UserPreferences(isProUser = false)
            val viewModel = createViewModel()

            viewModel.updateLockscreenMeter(true)
            viewModel.updateShowLockscreenMeterPublicly(true)
            viewModel.updateHeartRateOverlayEnabled(true)
            viewModel.updateWavRecordingDefaultEnabled(true)
            viewModel.updateNoiseNotification(NoiseNotificationUpdate.AudibleAlarm(true))
            viewModel.updateNoiseNotification(NoiseNotificationUpdate.TtsRiskPrompt(true))
            viewModel.updateFeatureToggle(FeatureToggleUpdate.TechnicalMetadata(true))
            viewModel.updateFeatureToggle(FeatureToggleUpdate.DosimeterCard(true))
            viewModel.updateFeatureToggle(FeatureToggleUpdate.SoundDetection(true))
            viewModel.updateFeatureToggle(FeatureToggleUpdate.SleepCard(true))

            coVerify(exactly = 0) { preferencesRepository.updateLockscreenMeterEnabled(any()) }
            coVerify(exactly = 0) { preferencesRepository.updateShowLockscreenMeterPublicly(any()) }
            coVerify(exactly = 0) { preferencesRepository.updateHeartRateOverlayEnabled(any()) }
            coVerify(exactly = 0) { preferencesRepository.updateWavRecordingDefaultEnabled(any()) }
            coVerify(exactly = 0) { preferencesRepository.updateAudibleAlarmEnabled(any()) }
            coVerify(exactly = 0) { preferencesRepository.updateTtsRiskPromptEnabled(any()) }
            coVerify(exactly = 0) { preferencesRepository.updateTechnicalMetadataEnabled(any()) }
            coVerify(exactly = 0) { preferencesRepository.updateDosimeterCardEnabled(any()) }
            coVerify(exactly = 0) { preferencesRepository.updateSoundDetectionEnabled(any()) }
            coVerify(exactly = 0) { preferencesRepository.updateSleepCardEnabled(any()) }
        }

    @Test
    fun proUserCanPersistWavRecordingDefaultOptIn() = runTest {
            preferencesFlow.value = UserPreferences(isProUser = true)
            val viewModel = createViewModel()

            viewModel.updateWavRecordingDefaultEnabled(true)

            coVerify { preferencesRepository.updateWavRecordingDefaultEnabled(true) }
        }

    @Test
    fun proUserCanPersistLockscreenPublicVisibilityOptInWhenLockscreenMeterIsEnabled() = runTest {
            preferencesFlow.value = UserPreferences(isProUser = true, lockscreenMeterEnabled = true)
            val viewModel = createViewModel()

            viewModel.updateShowLockscreenMeterPublicly(true)

            coVerify { preferencesRepository.updateShowLockscreenMeterPublicly(true) }
        }

    @Test
    fun publicLockscreenVisibilityCannotBeEnabledWithoutEffectiveLockscreenMeter() = runTest {
            preferencesFlow.value = UserPreferences(isProUser = true, lockscreenMeterEnabled = false)
            val viewModel = createViewModel()

            viewModel.updateShowLockscreenMeterPublicly(true)

            coVerify(exactly = 0) { preferencesRepository.updateShowLockscreenMeterPublicly(true) }
        }

    @Test
    fun freeUserUiStateUsesEffectiveValuesForProOnlyPreferences() = runTest {
            preferencesFlow.value =
                UserPreferences(
                    isProUser = false,
                    micSensitivityOffset = 4f,
                    frequencyWeighting = "C",
                    responseTime = ResponseTime.SLOW,
                    dosimeterStandard = DosimeterStandard.OSHA_PEL,
                    lockscreenMeterEnabled = true,
                    heartRateOverlayEnabled = true,
                    technicalMetadataEnabled = true,
                    dosimeterCardEnabled = true,
                    soundDetectionEnabled = true,
                    sleepCardEnabled = true,
                    wavRecordingDefaultEnabled = true,
                    audibleAlarmEnabled = true,
                    showLockscreenMeterPublicly = true,
                )

            val viewModel = createViewModel()

            assertEquals(UserPreferenceDefaults.MIC_SENSITIVITY_OFFSET, viewModel.uiState.value.micSensitivityOffset)
            assertEquals(UserPreferenceDefaults.FREQUENCY_WEIGHTING, viewModel.uiState.value.frequencyWeighting)
            assertEquals(UserPreferenceDefaults.responseTime, viewModel.uiState.value.responseTime)
            assertEquals(UserPreferenceDefaults.dosimeterStandard, viewModel.uiState.value.dosimeterStandard)
            assertEquals(false, viewModel.uiState.value.lockscreenMeterEnabled)
            assertEquals(false, viewModel.uiState.value.heartRateOverlayEnabled)
            assertEquals(false, viewModel.uiState.value.technicalMetadataEnabled)
            assertEquals(false, viewModel.uiState.value.dosimeterCardEnabled)
            assertEquals(false, viewModel.uiState.value.soundDetectionEnabled)
            assertEquals(false, viewModel.uiState.value.sleepCardEnabled)
            assertEquals(false, viewModel.uiState.value.wavRecordingDefaultEnabled)
            assertEquals(false, viewModel.uiState.value.audibleAlarmEnabled)
            assertEquals(false, viewModel.uiState.value.showLockscreenMeterPublicly)
        }

    @Test
    fun audibleAlarmPreviewRequiresPro() = runTest {
            preferencesFlow.value = UserPreferences(isProUser = false, audibleAlarmEnabled = true)
            val viewModel = createViewModel()

            viewModel.previewAudibleAlarm()

            verify(exactly = 0) { audioSessionManager.previewAudibleAlarm(any()) }

            preferencesFlow.value = UserPreferences(isProUser = true, audibleAlarmEnabled = true)
            advanceUntilIdle()

            viewModel.previewAudibleAlarm()

            verify(exactly = 1) { audioSessionManager.previewAudibleAlarm(isProUser = true) }
        }

    @Test
    fun lockscreenPublicVisibilityUiStateRequiresEffectiveLockscreenMeter() = runTest {
            preferencesFlow.value =
                UserPreferences(
                    isProUser = true,
                    lockscreenMeterEnabled = false,
                    showLockscreenMeterPublicly = true,
                )
            val viewModel = createViewModel()

            assertEquals(false, viewModel.uiState.value.showLockscreenMeterPublicly)

            preferencesFlow.value =
                UserPreferences(
                    isProUser = true,
                    lockscreenMeterEnabled = true,
                    showLockscreenMeterPublicly = true,
                )
            advanceUntilIdle()

            assertEquals(true, viewModel.uiState.value.showLockscreenMeterPublicly)
        }

    @Test
    fun healthConnectInstallUnavailableShowsHealthConnectError() = runTest {
            val viewModel = createViewModel()

            viewModel.onHealthConnectInstallUnavailable()

            assertEquals("Unable to open Health Connect", viewModel.uiState.value.healthConnectErrorMessage)
        }

    @Test
    fun healthConnectStatusFailureShowsHealthConnectError() = runTest {
            coEvery { healthConnectManager.getStatus() } returns
                HealthConnectStatus(errorMessage = "Unable to check Health Connect status")

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals("Unable to check Health Connect status", viewModel.uiState.value.healthConnectErrorMessage)
        }

    private fun createViewModel(): SettingsViewModel = settingsViewModelForTest(
        preferencesRepository = preferencesRepository,
        healthConnectManager = healthConnectManager,
        audioSessionManager = audioSessionManager,
    )
}
