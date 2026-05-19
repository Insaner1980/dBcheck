package com.dbcheck.app.ui.settings

import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.data.export.ExportCsvUseCase
import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.local.preferences.model.WaveformStyle
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.service.AudioSessionManager
import com.dbcheck.app.service.BackupService
import com.dbcheck.app.service.HealthConnectService
import com.dbcheck.app.sync.HealthConnectManager
import com.dbcheck.app.sync.HealthConnectStatus
import com.dbcheck.app.testStringContext
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsViewModelDisplayPreferenceTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferencesFlow =
        MutableStateFlow(
            UserPreferences(
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
            coEvery { updateWaveformStyle(any()) } just runs
            coEvery { updateRefreshRate(any()) } just runs
            coEvery { updateHealthConnectEnabled(any()) } just runs
            coEvery { updateHeartRateOverlayEnabled(any()) } just runs
            coEvery { updateThemeMode(any()) } just runs
        }
    private val healthConnectManager =
        mockk<HealthConnectManager> {
            coEvery { getStatus() } returns HealthConnectStatus()
        }
    private val audioSessionManager =
        mockk<AudioSessionManager> {
            every { isRecording } returns MutableStateFlow(false)
        }

    @Test
    fun displayPreferencesAreMappedIntoUiState() = runTest {
            val viewModel = createViewModel()

            assertEquals(WaveformStyle.BARS, viewModel.uiState.value.waveformStyle)
            assertEquals(MeterRefreshRate.LOW, viewModel.uiState.value.refreshRate)
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
    fun noiseNotificationUpdatesPersistSelectedValues() = runTest {
            val viewModel = createViewModel()

            viewModel.updateNoiseNotification(NoiseNotificationUpdate.ExposureAlerts(true))
            viewModel.updateNoiseNotification(NoiseNotificationUpdate.PeakWarnings(true))
            viewModel.updateNoiseNotification(NoiseNotificationUpdate.NotificationThreshold(90))

            coVerify { preferencesRepository.updateExposureAlerts(true) }
            coVerify { preferencesRepository.updatePeakWarnings(true) }
            coVerify { preferencesRepository.updateNotificationThreshold(90) }
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

            coVerify { preferencesRepository.updateThemeMode("system") }
            coVerify { preferencesRepository.updateFrequencyWeighting("A") }
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

            coVerify(exactly = 0) { preferencesRepository.updateMicSensitivityOffset(any()) }
            coVerify(exactly = 0) { preferencesRepository.updateFrequencyWeighting(any()) }
        }

    @Test
    fun healthConnectInstallUnavailableShowsHealthConnectError() = runTest {
            val viewModel = createViewModel()

            viewModel.onHealthConnectInstallUnavailable()

            assertEquals("Unable to open Health Connect", viewModel.uiState.value.healthConnectErrorMessage)
        }

    private fun createViewModel(): SettingsViewModel = SettingsViewModel(
            context = testStringContext(),
            preferencesRepository = preferencesRepository,
            healthConnectService = HealthConnectService(healthConnectManager),
            billingGateway = TestBillingGateway(),
            exportCsvUseCase = mockk<ExportCsvUseCase>(),
            backupService = BackupService(TestBackupGateway()),
            audioSessionManager = audioSessionManager,
        )
}
