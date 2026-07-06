package com.dbcheck.app.data.repository

import com.dbcheck.app.data.local.preferences.UserPreferencesDataStore
import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.local.preferences.model.WaveformStyle
import com.dbcheck.app.domain.ambient.AmbientSoundPreset
import com.dbcheck.app.domain.audio.ResponseTime
import com.dbcheck.app.domain.noise.DosimeterStandard
import com.dbcheck.app.domain.noise.NoiseNotificationSchedule
import com.dbcheck.app.domain.tinnitus.TinnitusPitchProfile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PreferencesRepositoryTest {
    private val dataStore = mockk<UserPreferencesDataStore>()

    @Test
    fun exposesUserPreferencesFlowFromDataStore() = runTest {
        val preferences = UserPreferences(isProUser = true)
        val repository = createRepository(preferences)

        assertEquals(preferences, repository.userPreferences.first())
    }

    @Test
    fun coreUpdateMethodsDelegateToDataStore() = runTest {
        stubUpdates()
        val repository = createRepository()
        val schedule =
            NoiseNotificationSchedule(
                activeDays = emptySet(),
                startMinuteOfDay = 22 * MINUTES_PER_HOUR,
                endMinuteOfDay = 6 * MINUTES_PER_HOUR,
            )

        repository.updateThemeMode("dark")
        repository.updateExposureAlerts(false)
        repository.updatePeakWarnings(false)
        repository.updateNotificationThreshold(90)
        repository.updateNotificationSchedule(schedule)
        repository.updateMicSensitivityOffset(2.5f)
        repository.updateFrequencyWeighting("C")
        repository.updateResponseTime(ResponseTime.SLOW)
        repository.updateDosimeterStandard(DosimeterStandard.OSHA_PEL)
        repository.updateSelectedCalibrationProfileId(42L)
        repository.updateWaveformStyle(WaveformStyle.BARS)
        repository.updateRefreshRate(MeterRefreshRate.LOW)

        coVerify(exactly = 1) {
            dataStore.updateThemeMode("dark")
            dataStore.updateExposureAlerts(false)
            dataStore.updatePeakWarnings(false)
            dataStore.updateNotificationThreshold(90)
            dataStore.updateNotificationSchedule(schedule)
            dataStore.updateMicSensitivityOffset(2.5f)
            dataStore.updateFrequencyWeighting("C")
            dataStore.updateResponseTime(ResponseTime.SLOW)
            dataStore.updateDosimeterStandard(DosimeterStandard.OSHA_PEL)
            dataStore.updateSelectedCalibrationProfileId(42L)
            dataStore.updateWaveformStyle(WaveformStyle.BARS)
            dataStore.updateRefreshRate(MeterRefreshRate.LOW)
        }
    }

    @Test
    fun featureUpdateMethodsDelegateToDataStore() = runTest {
        stubUpdates()
        val repository = createRepository()
        val pitchProfile =
            TinnitusPitchProfile(
                leftFrequencyHz = 1_000f,
                rightFrequencyHz = 4_000f,
                updatedAtMs = 1_700_000_000_000L,
            )

        repository.updateLockscreenMeterEnabled(true)
        repository.updateShowLockscreenMeterPublicly(true)
        repository.updateHealthConnectEnabled(true)
        repository.updateHeartRateOverlayEnabled(true)
        repository.updateTechnicalMetadataEnabled(true)
        repository.updateDosimeterCardEnabled(true)
        repository.updateSoundDetectionEnabled(true)
        repository.updateSoundDetectionPersistenceEnabled(true)
        repository.updateSleepCardEnabled(true)
        repository.updateWavRecordingDefaultEnabled(true)
        repository.updateAudibleAlarmEnabled(true)
        repository.updateTtsRiskPromptEnabled(true)
        repository.updateAmbientSoundPreset(AmbientSoundPreset.FAN)
        repository.updateAmbientSoundVolume(0.6f)
        repository.updateAmbientSoundTimerMinutes(15)
        repository.updateTinnitusPitchProfile(pitchProfile)
        repository.updateVoiceBaseline(levelDb = 68.5f, sampleCount = 7, capturedAtMs = 1_700_000_000_000L)
        repository.updateDebugForceFreeEnabled(true)
        repository.updateProUser(true)

        coVerify(exactly = 1) {
            dataStore.updateLockscreenMeterEnabled(true)
            dataStore.updateShowLockscreenMeterPublicly(true)
            dataStore.updateHealthConnectEnabled(true)
            dataStore.updateHeartRateOverlayEnabled(true)
            dataStore.updateTechnicalMetadataEnabled(true)
            dataStore.updateDosimeterCardEnabled(true)
            dataStore.updateSoundDetectionEnabled(true)
            dataStore.updateSoundDetectionPersistenceEnabled(true)
            dataStore.updateSleepCardEnabled(true)
            dataStore.updateWavRecordingDefaultEnabled(true)
            dataStore.updateAudibleAlarmEnabled(true)
            dataStore.updateTtsRiskPromptEnabled(true)
            dataStore.updateAmbientSoundPreset(AmbientSoundPreset.FAN)
            dataStore.updateAmbientSoundVolume(0.6f)
            dataStore.updateAmbientSoundTimerMinutes(15)
            dataStore.updateTinnitusPitchProfile(pitchProfile)
            dataStore.updateVoiceBaseline(levelDb = 68.5f, sampleCount = 7, capturedAtMs = 1_700_000_000_000L)
            dataStore.updateDebugForceFreeEnabled(true)
            dataStore.updateProUser(true)
        }
    }

    private fun createRepository(preferences: UserPreferences = UserPreferences()): PreferencesRepository {
        every { dataStore.userPreferences } returns flowOf(preferences)
        return PreferencesRepository(dataStore)
    }

    private fun stubUpdates() {
        coEvery { dataStore.updateThemeMode(any()) } returns Unit
        coEvery { dataStore.updateExposureAlerts(any()) } returns Unit
        coEvery { dataStore.updatePeakWarnings(any()) } returns Unit
        coEvery { dataStore.updateNotificationThreshold(any()) } returns Unit
        coEvery { dataStore.updateNotificationSchedule(any()) } returns Unit
        coEvery { dataStore.updateMicSensitivityOffset(any()) } returns Unit
        coEvery { dataStore.updateFrequencyWeighting(any()) } returns Unit
        coEvery { dataStore.updateResponseTime(any()) } returns Unit
        coEvery { dataStore.updateDosimeterStandard(any()) } returns Unit
        coEvery { dataStore.updateSelectedCalibrationProfileId(any()) } returns Unit
        coEvery { dataStore.updateWaveformStyle(any()) } returns Unit
        coEvery { dataStore.updateRefreshRate(any()) } returns Unit
        coEvery { dataStore.updateLockscreenMeterEnabled(any()) } returns Unit
        coEvery { dataStore.updateShowLockscreenMeterPublicly(any()) } returns Unit
        coEvery { dataStore.updateHealthConnectEnabled(any()) } returns Unit
        coEvery { dataStore.updateHeartRateOverlayEnabled(any()) } returns Unit
        coEvery { dataStore.updateTechnicalMetadataEnabled(any()) } returns Unit
        coEvery { dataStore.updateDosimeterCardEnabled(any()) } returns Unit
        coEvery { dataStore.updateSoundDetectionEnabled(any()) } returns Unit
        coEvery { dataStore.updateSoundDetectionPersistenceEnabled(any()) } returns Unit
        coEvery { dataStore.updateSleepCardEnabled(any()) } returns Unit
        coEvery { dataStore.updateWavRecordingDefaultEnabled(any()) } returns Unit
        coEvery { dataStore.updateAudibleAlarmEnabled(any()) } returns Unit
        coEvery { dataStore.updateTtsRiskPromptEnabled(any()) } returns Unit
        coEvery { dataStore.updateAmbientSoundPreset(any()) } returns Unit
        coEvery { dataStore.updateAmbientSoundVolume(any()) } returns Unit
        coEvery { dataStore.updateAmbientSoundTimerMinutes(any()) } returns Unit
        coEvery { dataStore.updateTinnitusPitchProfile(any()) } returns Unit
        coEvery { dataStore.updateVoiceBaseline(any(), any(), any()) } returns Unit
        coEvery { dataStore.updateDebugForceFreeEnabled(any()) } returns Unit
        coEvery { dataStore.updateProUser(any()) } returns Unit
    }

    private companion object {
        const val MINUTES_PER_HOUR = 60
    }
}
