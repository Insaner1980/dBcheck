package com.dbcheck.app.data.repository

import com.dbcheck.app.data.local.preferences.UserPreferencesDataStore
import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.local.preferences.model.WaveformStyle
import com.dbcheck.app.domain.audio.ResponseTime
import com.dbcheck.app.domain.noise.DosimeterStandard
import com.dbcheck.app.domain.noise.NoiseNotificationSchedule
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooManyFunctions")
class PreferencesRepository
    @Inject
    constructor(private val dataStore: UserPreferencesDataStore) {
        val userPreferences: Flow<UserPreferences> = dataStore.userPreferences

        suspend fun updateThemeMode(mode: String) = dataStore.updateThemeMode(mode)

        suspend fun updateExposureAlerts(enabled: Boolean) = dataStore.updateExposureAlerts(enabled)

        suspend fun updatePeakWarnings(enabled: Boolean) = dataStore.updatePeakWarnings(enabled)

        suspend fun updateNotificationThreshold(threshold: Int) = dataStore.updateNotificationThreshold(threshold)

        suspend fun updateNotificationSchedule(schedule: NoiseNotificationSchedule) =
            dataStore.updateNotificationSchedule(schedule)

        suspend fun updateMicSensitivityOffset(offset: Float) = dataStore.updateMicSensitivityOffset(offset)

        suspend fun updateFrequencyWeighting(weighting: String) = dataStore.updateFrequencyWeighting(weighting)

        suspend fun updateResponseTime(responseTime: ResponseTime) = dataStore.updateResponseTime(responseTime)

        suspend fun updateDosimeterStandard(standard: DosimeterStandard) = dataStore.updateDosimeterStandard(standard)

        suspend fun updateSelectedCalibrationProfileId(profileId: Long?) =
            dataStore.updateSelectedCalibrationProfileId(profileId)

        suspend fun updateSelectedAudioInputDeviceId(deviceId: Int?) =
            dataStore.updateSelectedAudioInputDeviceId(deviceId)

        suspend fun updateWaveformStyle(style: WaveformStyle) = dataStore.updateWaveformStyle(style)

        suspend fun updateRefreshRate(rate: MeterRefreshRate) = dataStore.updateRefreshRate(rate)

        suspend fun updateLockscreenMeterEnabled(enabled: Boolean) = dataStore.updateLockscreenMeterEnabled(enabled)

        suspend fun updateShowLockscreenMeterPublicly(enabled: Boolean) =
            dataStore.updateShowLockscreenMeterPublicly(enabled)

        suspend fun updateHealthConnectEnabled(enabled: Boolean) = dataStore.updateHealthConnectEnabled(enabled)

        suspend fun updateHeartRateOverlayEnabled(enabled: Boolean) = dataStore.updateHeartRateOverlayEnabled(enabled)

        suspend fun updateTechnicalMetadataEnabled(enabled: Boolean) = dataStore.updateTechnicalMetadataEnabled(enabled)

        suspend fun updateDosimeterCardEnabled(enabled: Boolean) = dataStore.updateDosimeterCardEnabled(enabled)

        suspend fun updateSoundDetectionEnabled(enabled: Boolean) = dataStore.updateSoundDetectionEnabled(enabled)

        suspend fun updateSoundDetectionPersistenceEnabled(enabled: Boolean) =
            dataStore.updateSoundDetectionPersistenceEnabled(enabled)

        suspend fun updateSleepCardEnabled(enabled: Boolean) = dataStore.updateSleepCardEnabled(enabled)

        suspend fun updateWavRecordingDefaultEnabled(enabled: Boolean) =
            dataStore.updateWavRecordingDefaultEnabled(enabled)

        suspend fun updateAudibleAlarmEnabled(enabled: Boolean) = dataStore.updateAudibleAlarmEnabled(enabled)

        suspend fun updateTtsRiskPromptEnabled(enabled: Boolean) = dataStore.updateTtsRiskPromptEnabled(enabled)

        suspend fun updateVoiceBaseline(levelDb: Float, sampleCount: Int, capturedAtMs: Long) =
            dataStore.updateVoiceBaseline(levelDb, sampleCount, capturedAtMs)

        suspend fun updateDebugForceFreeEnabled(enabled: Boolean) = dataStore.updateDebugForceFreeEnabled(enabled)

        suspend fun updateProUser(isPro: Boolean) = dataStore.updateProUser(isPro)
    }
