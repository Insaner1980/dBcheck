package com.dbcheck.app.data.local.preferences

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.data.local.preferences.model.UserPreferenceDefaults
import com.dbcheck.app.data.local.preferences.model.WaveformStyle
import com.dbcheck.app.domain.ambient.AmbientSoundPreset
import com.dbcheck.app.domain.audio.ResponseTime
import com.dbcheck.app.domain.noise.DosimeterStandard
import com.dbcheck.app.domain.noise.NoiseNotificationSchedule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.IOException
import java.time.DayOfWeek
import java.time.ZonedDateTime

class UserPreferencesDataStoreMappingTest {
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val notificationThresholdKey = intPreferencesKey("notification_threshold")
    private val notificationScheduleDaysKey = stringPreferencesKey("notification_schedule_active_days")
    private val notificationScheduleStartMinuteKey = intPreferencesKey("notification_schedule_start_minute")
    private val notificationScheduleEndMinuteKey = intPreferencesKey("notification_schedule_end_minute")
    private val micSensitivityOffsetKey = floatPreferencesKey("mic_sensitivity_offset")
    private val frequencyWeightingKey = stringPreferencesKey("frequency_weighting")
    private val responseTimeKey = stringPreferencesKey("response_time")
    private val dosimeterStandardKey = stringPreferencesKey("dosimeter_standard")
    private val selectedCalibrationProfileIdKey = longPreferencesKey("selected_calibration_profile_id")
    private val selectedAudioInputDeviceIdKey = intPreferencesKey("selected_audio_input_device_id")
    private val waveformStyleKey = stringPreferencesKey("waveform_style")
    private val refreshRateKey = stringPreferencesKey("refresh_rate")
    private val technicalMetadataKey = booleanPreferencesKey("technical_metadata")
    private val dosimeterCardKey = booleanPreferencesKey("dosimeter_card")
    private val soundDetectionKey = booleanPreferencesKey("sound_detection")
    private val soundDetectionPersistenceKey = booleanPreferencesKey("sound_detection_persistence")
    private val sleepCardKey = booleanPreferencesKey("sleep_card")
    private val wavRecordingDefaultKey = booleanPreferencesKey("wav_recording_default")
    private val audibleAlarmKey = booleanPreferencesKey("audible_alarm")
    private val ttsRiskPromptKey = booleanPreferencesKey("tts_risk_prompt")
    private val ambientSoundPresetKey = stringPreferencesKey("ambient_sound_preset")
    private val ambientSoundVolumeKey = floatPreferencesKey("ambient_sound_volume")
    private val ambientSoundTimerMinutesKey = intPreferencesKey("ambient_sound_timer_minutes")
    private val tinnitusLeftPitchHzKey = floatPreferencesKey("tinnitus_left_pitch_hz")
    private val tinnitusRightPitchHzKey = floatPreferencesKey("tinnitus_right_pitch_hz")
    private val tinnitusPitchUpdatedAtMsKey = longPreferencesKey("tinnitus_pitch_updated_at_ms")
    private val voiceBaselineLevelDbKey = floatPreferencesKey("voice_baseline_level_db")
    private val voiceBaselineSampleCountKey = intPreferencesKey("voice_baseline_sample_count")
    private val voiceBaselineCapturedAtMsKey = longPreferencesKey("voice_baseline_captured_at_ms")
    private val showLockscreenMeterPubliclyKey = booleanPreferencesKey("show_lockscreen_meter_publicly")

    @Test
    fun readIOExceptionFallsBackToDefaultPreferences() = runTest {
        val preferences =
            flow<Preferences> {
                throw IOException("read failed")
            }.toUserPreferencesFlow(isDebugBuild = false)
                .first()

        assertEquals(UserPreferenceDefaults.THEME_MODE, preferences.themeMode)
        assertEquals(UserPreferenceDefaults.NOTIFICATION_THRESHOLD, preferences.notificationThreshold)
        assertEquals(UserPreferenceDefaults.notificationSchedule, preferences.notificationSchedule)
        assertEquals(UserPreferenceDefaults.MIC_SENSITIVITY_OFFSET, preferences.micSensitivityOffset, 0f)
        assertEquals(UserPreferenceDefaults.FREQUENCY_WEIGHTING, preferences.frequencyWeighting)
        assertEquals(UserPreferenceDefaults.responseTime, preferences.responseTime)
        assertEquals(UserPreferenceDefaults.dosimeterStandard, preferences.dosimeterStandard)
        assertEquals(UserPreferenceDefaults.SELECTED_CALIBRATION_PROFILE_ID, preferences.selectedCalibrationProfileId)
        assertEquals(UserPreferenceDefaults.SELECTED_AUDIO_INPUT_DEVICE_ID, preferences.selectedAudioInputDeviceId)
        assertEquals(UserPreferenceDefaults.waveformStyle, preferences.waveformStyle)
        assertEquals(UserPreferenceDefaults.refreshRate, preferences.refreshRate)
        assertEquals(UserPreferenceDefaults.TECHNICAL_METADATA_ENABLED, preferences.technicalMetadataEnabled)
        assertEquals(UserPreferenceDefaults.DOSIMETER_CARD_ENABLED, preferences.dosimeterCardEnabled)
        assertEquals(UserPreferenceDefaults.SOUND_DETECTION_ENABLED, preferences.soundDetectionEnabled)
        assertEquals(
            UserPreferenceDefaults.SOUND_DETECTION_PERSISTENCE_ENABLED,
            preferences.soundDetectionPersistenceEnabled,
        )
        assertEquals(UserPreferenceDefaults.SLEEP_CARD_ENABLED, preferences.sleepCardEnabled)
        assertEquals(UserPreferenceDefaults.WAV_RECORDING_DEFAULT_ENABLED, preferences.wavRecordingDefaultEnabled)
        assertEquals(UserPreferenceDefaults.AUDIBLE_ALARM_ENABLED, preferences.audibleAlarmEnabled)
        assertEquals(UserPreferenceDefaults.TTS_RISK_PROMPT_ENABLED, preferences.ttsRiskPromptEnabled)
        assertEquals(UserPreferenceDefaults.ambientSoundPreset, preferences.ambientSoundPreset)
        assertEquals(UserPreferenceDefaults.AMBIENT_SOUND_VOLUME, preferences.ambientSoundVolume, 0f)
        assertEquals(UserPreferenceDefaults.AMBIENT_SOUND_TIMER_MINUTES, preferences.ambientSoundTimerMinutes)
        assertEquals(UserPreferenceDefaults.tinnitusPitchProfile, preferences.tinnitusPitchProfile)
        assertEquals(UserPreferenceDefaults.VOICE_BASELINE_LEVEL_DB, preferences.voiceBaselineLevelDb)
        assertEquals(UserPreferenceDefaults.VOICE_BASELINE_SAMPLE_COUNT, preferences.voiceBaselineSampleCount)
        assertEquals(UserPreferenceDefaults.VOICE_BASELINE_CAPTURED_AT_MS, preferences.voiceBaselineCapturedAtMs)
        assertEquals(UserPreferenceDefaults.SHOW_LOCKSCREEN_METER_PUBLICLY, preferences.showLockscreenMeterPublicly)
        assertFalse(preferences.isProUser)
    }

    @Test
    fun invalidStoredValuesAreNormalizedBeforeReachingCallers() = runTest {
        val preferences =
            flowOf(
                preferencesOf(
                    themeModeKey to "midnight",
                    notificationThresholdKey to 130,
                    notificationScheduleDaysKey to "8,nope",
                    notificationScheduleStartMinuteKey to -30,
                    notificationScheduleEndMinuteKey to 1_600,
                    micSensitivityOffsetKey to 25f,
                    frequencyWeightingKey to "Q",
                    responseTimeKey to "instant",
                    dosimeterStandardKey to "european",
                    selectedCalibrationProfileIdKey to -1L,
                    selectedAudioInputDeviceIdKey to -1,
                    voiceBaselineLevelDbKey to Float.NaN,
                    voiceBaselineSampleCountKey to -1,
                    voiceBaselineCapturedAtMsKey to -1L,
                    ambientSoundPresetKey to "relief",
                    ambientSoundVolumeKey to 5f,
                    ambientSoundTimerMinutesKey to 7,
                    tinnitusLeftPitchHzKey to 120f,
                    tinnitusRightPitchHzKey to Float.NaN,
                    tinnitusPitchUpdatedAtMsKey to -1L,
                    waveformStyleKey to "sparkline",
                    refreshRateKey to "turbo",
                ),
            ).toUserPreferencesFlow(isDebugBuild = false)
                .first()

        assertEquals(UserPreferenceDefaults.THEME_MODE, preferences.themeMode)
        assertEquals(UserPreferenceDefaults.NOTIFICATION_THRESHOLD_MAX, preferences.notificationThreshold)
        assertEquals(
            UserPreferenceDefaults.notificationSchedule.activeDays,
            preferences.notificationSchedule.activeDays,
        )
        assertEquals(NoiseNotificationSchedule.MIN_MINUTE_OF_DAY, preferences.notificationSchedule.startMinuteOfDay)
        assertEquals(NoiseNotificationSchedule.MAX_MINUTE_OF_DAY, preferences.notificationSchedule.endMinuteOfDay)
        assertEquals(UserPreferenceDefaults.MIC_SENSITIVITY_OFFSET_MAX, preferences.micSensitivityOffset, 0f)
        assertEquals(UserPreferenceDefaults.FREQUENCY_WEIGHTING, preferences.frequencyWeighting)
        assertEquals(ResponseTime.FAST, preferences.responseTime)
        assertEquals(DosimeterStandard.NIOSH_REL, preferences.dosimeterStandard)
        assertEquals(UserPreferenceDefaults.SELECTED_CALIBRATION_PROFILE_ID, preferences.selectedCalibrationProfileId)
        assertEquals(UserPreferenceDefaults.SELECTED_AUDIO_INPUT_DEVICE_ID, preferences.selectedAudioInputDeviceId)
        assertEquals(UserPreferenceDefaults.VOICE_BASELINE_LEVEL_DB, preferences.voiceBaselineLevelDb)
        assertEquals(UserPreferenceDefaults.VOICE_BASELINE_SAMPLE_COUNT, preferences.voiceBaselineSampleCount)
        assertEquals(UserPreferenceDefaults.VOICE_BASELINE_CAPTURED_AT_MS, preferences.voiceBaselineCapturedAtMs)
        assertEquals(250f, preferences.tinnitusPitchProfile.leftFrequencyHz ?: 0f, 0f)
        assertEquals(null, preferences.tinnitusPitchProfile.rightFrequencyHz)
        assertEquals(null, preferences.tinnitusPitchProfile.updatedAtMs)
        assertEquals(WaveformStyle.LINE, preferences.waveformStyle)
        assertEquals(MeterRefreshRate.STANDARD, preferences.refreshRate)
        assertEquals(UserPreferenceDefaults.ambientSoundPreset, preferences.ambientSoundPreset)
        assertEquals(1f, preferences.ambientSoundVolume, 0f)
        assertEquals(UserPreferenceDefaults.AMBIENT_SOUND_TIMER_MINUTES, preferences.ambientSoundTimerMinutes)
    }

    @Test
    fun storedResponseTimeIsMappedIntoPreferences() = runTest {
        val preferences =
            flowOf(
                preferencesOf(responseTimeKey to ResponseTime.SLOW.preferenceValue),
            ).toUserPreferencesFlow(isDebugBuild = false)
                .first()

        assertEquals(ResponseTime.SLOW, preferences.responseTime)
    }

    @Test
    fun storedDosimeterStandardIsMappedIntoPreferences() = runTest {
        val preferences =
            flowOf(
                preferencesOf(dosimeterStandardKey to DosimeterStandard.OSHA_PEL.preferenceValue),
            ).toUserPreferencesFlow(isDebugBuild = false)
                .first()

        assertEquals(DosimeterStandard.OSHA_PEL, preferences.dosimeterStandard)
    }

    @Test
    fun storedSelectedCalibrationProfileIdIsMappedIntoPreferences() = runTest {
        val preferences =
            flowOf(
                preferencesOf(selectedCalibrationProfileIdKey to 42L),
            ).toUserPreferencesFlow(isDebugBuild = false)
                .first()

        assertEquals(42L, preferences.selectedCalibrationProfileId)
    }

    @Test
    fun storedSelectedAudioInputDeviceIdIsMappedIntoPreferences() = runTest {
        val preferences =
            flowOf(
                preferencesOf(selectedAudioInputDeviceIdKey to 12),
            ).toUserPreferencesFlow(isDebugBuild = false)
                .first()

        assertEquals(12, preferences.selectedAudioInputDeviceId)
    }

    @Test
    fun storedNotificationScheduleIsMappedIntoPreferences() = runTest {
        val preferences =
            flowOf(
                preferencesOf(
                    notificationScheduleDaysKey to "1,3,5",
                    notificationScheduleStartMinuteKey to 22 * MINUTES_PER_HOUR,
                    notificationScheduleEndMinuteKey to 6 * MINUTES_PER_HOUR,
                ),
            ).toUserPreferencesFlow(isDebugBuild = false)
                .first()

        assertEquals(
            setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            preferences.notificationSchedule.activeDays,
        )
        assertEquals(22 * MINUTES_PER_HOUR, preferences.notificationSchedule.startMinuteOfDay)
        assertEquals(6 * MINUTES_PER_HOUR, preferences.notificationSchedule.endMinuteOfDay)
        assertEquals(true, preferences.notificationSchedule.isActiveAt(time("2026-06-23T02:00:00Z")))
    }

    @Test
    fun storedSoundDetectionToggleIsMappedIntoPreferences() = runTest {
        val preferences =
            flowOf(
                preferencesOf(soundDetectionKey to true),
            ).toUserPreferencesFlow(isDebugBuild = false)
                .first()

        assertEquals(true, preferences.soundDetectionEnabled)
    }

    @Test
    fun storedFeatureVisibilityTogglesAreMappedIntoPreferences() = runTest {
        val preferences =
            flowOf(
                preferencesOf(
                    technicalMetadataKey to false,
                    dosimeterCardKey to false,
                    sleepCardKey to true,
                ),
            ).toUserPreferencesFlow(isDebugBuild = false)
                .first()

        assertEquals(false, preferences.technicalMetadataEnabled)
        assertEquals(false, preferences.dosimeterCardEnabled)
        assertEquals(true, preferences.sleepCardEnabled)
    }

    @Test
    fun storedSoundDetectionPersistenceOptInIsMappedIntoPreferences() = runTest {
        val preferences =
            flowOf(
                preferencesOf(soundDetectionPersistenceKey to true),
            ).toUserPreferencesFlow(isDebugBuild = false)
                .first()

        assertEquals(true, preferences.soundDetectionPersistenceEnabled)
    }

    @Test
    fun storedWavRecordingDefaultOptInIsMappedIntoPreferences() = runTest {
        val preferences =
            flowOf(
                preferencesOf(wavRecordingDefaultKey to true),
            ).toUserPreferencesFlow(isDebugBuild = false)
                .first()

        assertEquals(true, preferences.wavRecordingDefaultEnabled)
    }

    @Test
    fun storedAudibleAlarmOptInIsMappedIntoPreferences() = runTest {
        val preferences =
            flowOf(
                preferencesOf(audibleAlarmKey to true),
            ).toUserPreferencesFlow(isDebugBuild = false)
                .first()

        assertEquals(true, preferences.audibleAlarmEnabled)
    }

    @Test
    fun storedTtsRiskPromptOptInIsMappedIntoPreferences() = runTest {
        val preferences =
            flowOf(
                preferencesOf(ttsRiskPromptKey to true),
            ).toUserPreferencesFlow(isDebugBuild = false)
                .first()

        assertEquals(true, preferences.ttsRiskPromptEnabled)
    }

    @Test
    fun storedAmbientSoundPreferencesAreMappedIntoPreferences() = runTest {
        val preferences =
            flowOf(
                preferencesOf(
                    ambientSoundPresetKey to AmbientSoundPreset.FAN.preferenceValue,
                    ambientSoundVolumeKey to 0.6f,
                    ambientSoundTimerMinutesKey to 15,
                ),
            ).toUserPreferencesFlow(isDebugBuild = false)
                .first()

        assertEquals(AmbientSoundPreset.FAN, preferences.ambientSoundPreset)
        assertEquals(0.6f, preferences.ambientSoundVolume, 0f)
        assertEquals(15, preferences.ambientSoundTimerMinutes)
    }

    @Test
    fun storedTinnitusPitchProfileIsMappedIntoPreferences() = runTest {
        val preferences =
            flowOf(
                preferencesOf(
                    tinnitusLeftPitchHzKey to 1_000f,
                    tinnitusRightPitchHzKey to 4_000f,
                    tinnitusPitchUpdatedAtMsKey to 1_700_000_000_000L,
                ),
            ).toUserPreferencesFlow(isDebugBuild = false)
                .first()

        assertEquals(1_000f, preferences.tinnitusPitchProfile.leftFrequencyHz ?: 0f, 0f)
        assertEquals(4_000f, preferences.tinnitusPitchProfile.rightFrequencyHz ?: 0f, 0f)
        assertEquals(1_700_000_000_000L, preferences.tinnitusPitchProfile.updatedAtMs)
    }

    @Test
    fun storedVoiceBaselineIsMappedIntoPreferences() = runTest {
        val preferences =
            flowOf(
                preferencesOf(
                    voiceBaselineLevelDbKey to 68.5f,
                    voiceBaselineSampleCountKey to 7,
                    voiceBaselineCapturedAtMsKey to 1_700_000_000_000L,
                ),
            ).toUserPreferencesFlow(isDebugBuild = false)
                .first()

        assertEquals(68.5f, preferences.voiceBaselineLevelDb ?: 0f, 0f)
        assertEquals(7, preferences.voiceBaselineSampleCount)
        assertEquals(1_700_000_000_000L, preferences.voiceBaselineCapturedAtMs)
    }

    @Test
    fun storedLockscreenPublicVisibilityOptInIsMappedIntoPreferences() = runTest {
        val preferences =
            flowOf(
                preferencesOf(showLockscreenMeterPubliclyKey to true),
            ).toUserPreferencesFlow(isDebugBuild = false)
                .first()

        assertEquals(true, preferences.showLockscreenMeterPublicly)
    }

    private fun time(value: String): ZonedDateTime = ZonedDateTime.parse(value)

    private companion object {
        const val MINUTES_PER_HOUR = 60
    }
}
