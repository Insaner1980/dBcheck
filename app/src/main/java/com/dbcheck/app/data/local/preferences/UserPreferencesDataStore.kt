package com.dbcheck.app.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dbcheck.app.BuildConfig
import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.data.local.preferences.model.UserPreferenceDefaults
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.local.preferences.model.WaveformStyle
import com.dbcheck.app.domain.ambient.AmbientSoundPreset
import com.dbcheck.app.domain.audio.ResponseTime
import com.dbcheck.app.domain.entitlement.ProEntitlementPolicy
import com.dbcheck.app.domain.noise.DosimeterStandard
import com.dbcheck.app.domain.noise.NoiseNotificationSchedule
import com.dbcheck.app.domain.tinnitus.TinnitusPitchProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

private object Keys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val EXPOSURE_ALERTS = booleanPreferencesKey("exposure_alerts")
    val PEAK_WARNINGS = booleanPreferencesKey("peak_warnings")
    val NOTIFICATION_THRESHOLD = intPreferencesKey("notification_threshold")
    val NOTIFICATION_SCHEDULE_ACTIVE_DAYS = stringPreferencesKey("notification_schedule_active_days")
    val NOTIFICATION_SCHEDULE_START_MINUTE = intPreferencesKey("notification_schedule_start_minute")
    val NOTIFICATION_SCHEDULE_END_MINUTE = intPreferencesKey("notification_schedule_end_minute")
    val MIC_SENSITIVITY_OFFSET = floatPreferencesKey("mic_sensitivity_offset")
    val FREQUENCY_WEIGHTING = stringPreferencesKey("frequency_weighting")
    val RESPONSE_TIME = stringPreferencesKey("response_time")
    val DOSIMETER_STANDARD = stringPreferencesKey("dosimeter_standard")
    val SELECTED_CALIBRATION_PROFILE_ID = longPreferencesKey("selected_calibration_profile_id")
    val SELECTED_AUDIO_INPUT_DEVICE_ID = intPreferencesKey("selected_audio_input_device_id")
    val WAVEFORM_STYLE = stringPreferencesKey("waveform_style")
    val REFRESH_RATE = stringPreferencesKey("refresh_rate")
    val LOCKSCREEN_METER = booleanPreferencesKey("lockscreen_meter")
    val SHOW_LOCKSCREEN_METER_PUBLICLY = booleanPreferencesKey("show_lockscreen_meter_publicly")
    val HEALTH_CONNECT = booleanPreferencesKey("health_connect")
    val HEART_RATE_OVERLAY = booleanPreferencesKey("heart_rate_overlay")
    val TECHNICAL_METADATA = booleanPreferencesKey("technical_metadata")
    val DOSIMETER_CARD = booleanPreferencesKey("dosimeter_card")
    val SOUND_DETECTION = booleanPreferencesKey("sound_detection")
    val SOUND_DETECTION_PERSISTENCE = booleanPreferencesKey("sound_detection_persistence")
    val SLEEP_CARD = booleanPreferencesKey("sleep_card")
    val WAV_RECORDING_DEFAULT = booleanPreferencesKey("wav_recording_default")
    val AUDIBLE_ALARM = booleanPreferencesKey("audible_alarm")
    val TTS_RISK_PROMPT = booleanPreferencesKey("tts_risk_prompt")
    val AMBIENT_SOUND_PRESET = stringPreferencesKey("ambient_sound_preset")
    val AMBIENT_SOUND_VOLUME = floatPreferencesKey("ambient_sound_volume")
    val AMBIENT_SOUND_TIMER_MINUTES = intPreferencesKey("ambient_sound_timer_minutes")
    val TINNITUS_LEFT_PITCH_HZ = floatPreferencesKey("tinnitus_left_pitch_hz")
    val TINNITUS_RIGHT_PITCH_HZ = floatPreferencesKey("tinnitus_right_pitch_hz")
    val TINNITUS_PITCH_UPDATED_AT_MS = longPreferencesKey("tinnitus_pitch_updated_at_ms")
    val VOICE_BASELINE_LEVEL_DB = floatPreferencesKey("voice_baseline_level_db")
    val VOICE_BASELINE_SAMPLE_COUNT = intPreferencesKey("voice_baseline_sample_count")
    val VOICE_BASELINE_CAPTURED_AT_MS = longPreferencesKey("voice_baseline_captured_at_ms")
    val DEBUG_FORCE_FREE = booleanPreferencesKey("debug_force_free")
    val IS_PRO_USER = booleanPreferencesKey("is_pro_user")
}

internal fun Flow<Preferences>.toUserPreferencesFlow(isDebugBuild: Boolean): Flow<UserPreferences> =
    catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }.map { prefs -> prefs.toUserPreferences(isDebugBuild) }

private data class StoredVoiceBaselinePreferences(val levelDb: Float?, val sampleCount: Int, val capturedAtMs: Long?)

private fun Preferences.toUserPreferences(isDebugBuild: Boolean): UserPreferences {
    val debugForceFreeEnabled =
        booleanValue(Keys.DEBUG_FORCE_FREE, UserPreferenceDefaults.DEBUG_FORCE_FREE_ENABLED)
    val isPurchased = booleanValue(Keys.IS_PRO_USER, UserPreferenceDefaults.IS_PRO_USER)
    val voiceBaseline = toStoredVoiceBaselinePreferences()

    return UserPreferences(
        themeMode = UserPreferenceDefaults.normalizeThemeMode(this[Keys.THEME_MODE]),
        exposureAlertsEnabled = booleanValue(Keys.EXPOSURE_ALERTS, UserPreferenceDefaults.EXPOSURE_ALERTS_ENABLED),
        peakWarningsEnabled = booleanValue(Keys.PEAK_WARNINGS, UserPreferenceDefaults.PEAK_WARNINGS_ENABLED),
        notificationThreshold =
            UserPreferenceDefaults.normalizeNotificationThreshold(this[Keys.NOTIFICATION_THRESHOLD]),
        notificationSchedule =
            UserPreferenceDefaults.normalizeNotificationSchedule(
                activeDaysPreferenceValue = this[Keys.NOTIFICATION_SCHEDULE_ACTIVE_DAYS],
                startMinuteOfDay = this[Keys.NOTIFICATION_SCHEDULE_START_MINUTE],
                endMinuteOfDay = this[Keys.NOTIFICATION_SCHEDULE_END_MINUTE],
            ),
        micSensitivityOffset =
            UserPreferenceDefaults.normalizeMicSensitivityOffset(this[Keys.MIC_SENSITIVITY_OFFSET]),
        frequencyWeighting =
            UserPreferenceDefaults.normalizeFrequencyWeighting(this[Keys.FREQUENCY_WEIGHTING]),
        responseTime = UserPreferenceDefaults.normalizeResponseTime(this[Keys.RESPONSE_TIME]),
        dosimeterStandard = UserPreferenceDefaults.normalizeDosimeterStandard(this[Keys.DOSIMETER_STANDARD]),
        selectedCalibrationProfileId =
            UserPreferenceDefaults.normalizeSelectedCalibrationProfileId(this[Keys.SELECTED_CALIBRATION_PROFILE_ID]),
        selectedAudioInputDeviceId =
            UserPreferenceDefaults.normalizeSelectedAudioInputDeviceId(this[Keys.SELECTED_AUDIO_INPUT_DEVICE_ID]),
        waveformStyle = WaveformStyle.fromPreference(this[Keys.WAVEFORM_STYLE]),
        refreshRate = MeterRefreshRate.fromPreference(this[Keys.REFRESH_RATE]),
        lockscreenMeterEnabled =
            booleanValue(Keys.LOCKSCREEN_METER, UserPreferenceDefaults.LOCKSCREEN_METER_ENABLED),
        showLockscreenMeterPublicly =
            booleanValue(
                Keys.SHOW_LOCKSCREEN_METER_PUBLICLY,
                UserPreferenceDefaults.SHOW_LOCKSCREEN_METER_PUBLICLY,
            ),
        healthConnectEnabled =
            booleanValue(Keys.HEALTH_CONNECT, UserPreferenceDefaults.HEALTH_CONNECT_ENABLED),
        heartRateOverlayEnabled =
            booleanValue(Keys.HEART_RATE_OVERLAY, UserPreferenceDefaults.HEART_RATE_OVERLAY_ENABLED),
        technicalMetadataEnabled =
            booleanValue(Keys.TECHNICAL_METADATA, UserPreferenceDefaults.TECHNICAL_METADATA_ENABLED),
        dosimeterCardEnabled =
            booleanValue(Keys.DOSIMETER_CARD, UserPreferenceDefaults.DOSIMETER_CARD_ENABLED),
        soundDetectionEnabled =
            booleanValue(Keys.SOUND_DETECTION, UserPreferenceDefaults.SOUND_DETECTION_ENABLED),
        soundDetectionPersistenceEnabled =
            booleanValue(
                Keys.SOUND_DETECTION_PERSISTENCE,
                UserPreferenceDefaults.SOUND_DETECTION_PERSISTENCE_ENABLED,
            ),
        sleepCardEnabled =
            booleanValue(Keys.SLEEP_CARD, UserPreferenceDefaults.SLEEP_CARD_ENABLED),
        wavRecordingDefaultEnabled =
            booleanValue(Keys.WAV_RECORDING_DEFAULT, UserPreferenceDefaults.WAV_RECORDING_DEFAULT_ENABLED),
        audibleAlarmEnabled =
            booleanValue(Keys.AUDIBLE_ALARM, UserPreferenceDefaults.AUDIBLE_ALARM_ENABLED),
        ttsRiskPromptEnabled =
            booleanValue(Keys.TTS_RISK_PROMPT, UserPreferenceDefaults.TTS_RISK_PROMPT_ENABLED),
        ambientSoundPreset = UserPreferenceDefaults.normalizeAmbientSoundPreset(this[Keys.AMBIENT_SOUND_PRESET]),
        ambientSoundVolume = UserPreferenceDefaults.normalizeAmbientSoundVolume(this[Keys.AMBIENT_SOUND_VOLUME]),
        ambientSoundTimerMinutes =
            UserPreferenceDefaults.normalizeAmbientSoundTimerMinutes(this[Keys.AMBIENT_SOUND_TIMER_MINUTES]),
        tinnitusPitchProfile = toStoredTinnitusPitchProfile(),
        voiceBaselineLevelDb = voiceBaseline.levelDb,
        voiceBaselineSampleCount = voiceBaseline.sampleCount,
        voiceBaselineCapturedAtMs = voiceBaseline.capturedAtMs,
        debugForceFreeEnabled = debugForceFreeEnabled,
        isProUser =
            ProEntitlementPolicy.isProUser(
                isPurchased = isPurchased,
                isDebugBuild = isDebugBuild,
                debugForceFreeEnabled = debugForceFreeEnabled,
            ),
    )
}

private fun Preferences.booleanValue(key: Preferences.Key<Boolean>, defaultValue: Boolean): Boolean =
    this[key] ?: defaultValue

private fun Preferences.toStoredVoiceBaselinePreferences(): StoredVoiceBaselinePreferences {
    val levelDb = UserPreferenceDefaults.normalizeVoiceBaselineLevelDb(this[Keys.VOICE_BASELINE_LEVEL_DB])
    return if (levelDb == null) {
        StoredVoiceBaselinePreferences(
            levelDb = UserPreferenceDefaults.VOICE_BASELINE_LEVEL_DB,
            sampleCount = UserPreferenceDefaults.VOICE_BASELINE_SAMPLE_COUNT,
            capturedAtMs = UserPreferenceDefaults.VOICE_BASELINE_CAPTURED_AT_MS,
        )
    } else {
        StoredVoiceBaselinePreferences(
            levelDb = levelDb,
            sampleCount =
                UserPreferenceDefaults.normalizeVoiceBaselineSampleCount(this[Keys.VOICE_BASELINE_SAMPLE_COUNT]),
            capturedAtMs =
                UserPreferenceDefaults.normalizeVoiceBaselineCapturedAtMs(this[Keys.VOICE_BASELINE_CAPTURED_AT_MS]),
        )
    }
}

private fun Preferences.toStoredTinnitusPitchProfile(): TinnitusPitchProfile {
    val leftFrequencyHz =
        UserPreferenceDefaults.normalizeTinnitusPitchFrequencyHz(this[Keys.TINNITUS_LEFT_PITCH_HZ])
    val rightFrequencyHz =
        UserPreferenceDefaults.normalizeTinnitusPitchFrequencyHz(this[Keys.TINNITUS_RIGHT_PITCH_HZ])
    val updatedAtMs =
        if (leftFrequencyHz != null || rightFrequencyHz != null) {
            UserPreferenceDefaults.normalizeTinnitusPitchUpdatedAtMs(this[Keys.TINNITUS_PITCH_UPDATED_AT_MS])
        } else {
            null
        }
    return TinnitusPitchProfile(
        leftFrequencyHz = leftFrequencyHz,
        rightFrequencyHz = rightFrequencyHz,
        updatedAtMs = updatedAtMs,
    )
}

@Singleton
@Suppress("TooManyFunctions")
class UserPreferencesDataStore
    @Inject
    constructor(@param:ApplicationContext private val context: Context) {
        val userPreferences: Flow<UserPreferences> =
            context.dataStore.data.toUserPreferencesFlow(isDebugBuild = BuildConfig.DEBUG)

        suspend fun updateThemeMode(mode: String) {
            context.dataStore.edit { it[Keys.THEME_MODE] = UserPreferenceDefaults.normalizeThemeMode(mode) }
        }

        suspend fun updateExposureAlerts(enabled: Boolean) {
            context.dataStore.edit { it[Keys.EXPOSURE_ALERTS] = enabled }
        }

        suspend fun updatePeakWarnings(enabled: Boolean) {
            context.dataStore.edit { it[Keys.PEAK_WARNINGS] = enabled }
        }

        suspend fun updateNotificationThreshold(threshold: Int) {
            context.dataStore.edit {
                it[Keys.NOTIFICATION_THRESHOLD] =
                    UserPreferenceDefaults.normalizeNotificationThreshold(threshold)
            }
        }

        suspend fun updateNotificationSchedule(schedule: NoiseNotificationSchedule) {
            context.dataStore.edit {
                it[Keys.NOTIFICATION_SCHEDULE_ACTIVE_DAYS] =
                    NoiseNotificationSchedule.activeDaysPreferenceValue(schedule.activeDays)
                it[Keys.NOTIFICATION_SCHEDULE_START_MINUTE] = schedule.startMinuteOfDay
                it[Keys.NOTIFICATION_SCHEDULE_END_MINUTE] = schedule.endMinuteOfDay
            }
        }

        suspend fun updateMicSensitivityOffset(offset: Float) {
            context.dataStore.edit {
                it[Keys.MIC_SENSITIVITY_OFFSET] =
                    UserPreferenceDefaults.normalizeMicSensitivityOffset(offset)
            }
        }

        suspend fun updateFrequencyWeighting(weighting: String) {
            context.dataStore.edit {
                it[Keys.FREQUENCY_WEIGHTING] =
                    UserPreferenceDefaults.normalizeFrequencyWeighting(weighting)
            }
        }

        suspend fun updateResponseTime(responseTime: ResponseTime) {
            context.dataStore.edit { it[Keys.RESPONSE_TIME] = responseTime.preferenceValue }
        }

        suspend fun updateDosimeterStandard(standard: DosimeterStandard) {
            context.dataStore.edit { it[Keys.DOSIMETER_STANDARD] = standard.preferenceValue }
        }

        suspend fun updateSelectedCalibrationProfileId(profileId: Long?) {
            context.dataStore.edit {
                val normalized = UserPreferenceDefaults.normalizeSelectedCalibrationProfileId(profileId)
                if (normalized == null) {
                    it.remove(Keys.SELECTED_CALIBRATION_PROFILE_ID)
                } else {
                    it[Keys.SELECTED_CALIBRATION_PROFILE_ID] = normalized
                }
            }
        }

        suspend fun updateSelectedAudioInputDeviceId(deviceId: Int?) {
            context.dataStore.edit {
                val normalized = UserPreferenceDefaults.normalizeSelectedAudioInputDeviceId(deviceId)
                if (normalized == null) {
                    it.remove(Keys.SELECTED_AUDIO_INPUT_DEVICE_ID)
                } else {
                    it[Keys.SELECTED_AUDIO_INPUT_DEVICE_ID] = normalized
                }
            }
        }

        suspend fun updateWaveformStyle(style: WaveformStyle) {
            context.dataStore.edit { it[Keys.WAVEFORM_STYLE] = style.preferenceValue }
        }

        suspend fun updateRefreshRate(rate: MeterRefreshRate) {
            context.dataStore.edit { it[Keys.REFRESH_RATE] = rate.preferenceValue }
        }

        suspend fun updateLockscreenMeterEnabled(enabled: Boolean) {
            context.dataStore.edit { it[Keys.LOCKSCREEN_METER] = enabled }
        }

        suspend fun updateShowLockscreenMeterPublicly(enabled: Boolean) {
            context.dataStore.edit { it[Keys.SHOW_LOCKSCREEN_METER_PUBLICLY] = enabled }
        }

        suspend fun updateHealthConnectEnabled(enabled: Boolean) {
            context.dataStore.edit { it[Keys.HEALTH_CONNECT] = enabled }
        }

        suspend fun updateHeartRateOverlayEnabled(enabled: Boolean) {
            context.dataStore.edit { it[Keys.HEART_RATE_OVERLAY] = enabled }
        }

        suspend fun updateTechnicalMetadataEnabled(enabled: Boolean) {
            context.dataStore.edit { it[Keys.TECHNICAL_METADATA] = enabled }
        }

        suspend fun updateDosimeterCardEnabled(enabled: Boolean) {
            context.dataStore.edit { it[Keys.DOSIMETER_CARD] = enabled }
        }

        suspend fun updateSoundDetectionEnabled(enabled: Boolean) {
            context.dataStore.edit { it[Keys.SOUND_DETECTION] = enabled }
        }

        suspend fun updateSoundDetectionPersistenceEnabled(enabled: Boolean) {
            context.dataStore.edit { it[Keys.SOUND_DETECTION_PERSISTENCE] = enabled }
        }

        suspend fun updateSleepCardEnabled(enabled: Boolean) {
            context.dataStore.edit { it[Keys.SLEEP_CARD] = enabled }
        }

        suspend fun updateWavRecordingDefaultEnabled(enabled: Boolean) {
            context.dataStore.edit { it[Keys.WAV_RECORDING_DEFAULT] = enabled }
        }

        suspend fun updateAudibleAlarmEnabled(enabled: Boolean) {
            context.dataStore.edit { it[Keys.AUDIBLE_ALARM] = enabled }
        }

        suspend fun updateTtsRiskPromptEnabled(enabled: Boolean) {
            context.dataStore.edit { it[Keys.TTS_RISK_PROMPT] = enabled }
        }

        suspend fun updateAmbientSoundPreset(preset: AmbientSoundPreset) {
            context.dataStore.edit {
                it[Keys.AMBIENT_SOUND_PRESET] =
                    UserPreferenceDefaults.normalizeAmbientSoundPreset(preset.preferenceValue).preferenceValue
            }
        }

        suspend fun updateAmbientSoundVolume(volume: Float) {
            context.dataStore.edit {
                it[Keys.AMBIENT_SOUND_VOLUME] = UserPreferenceDefaults.normalizeAmbientSoundVolume(volume)
            }
        }

        suspend fun updateAmbientSoundTimerMinutes(minutes: Int) {
            context.dataStore.edit {
                it[Keys.AMBIENT_SOUND_TIMER_MINUTES] =
                    UserPreferenceDefaults.normalizeAmbientSoundTimerMinutes(minutes)
            }
        }

        suspend fun updateTinnitusPitchProfile(profile: TinnitusPitchProfile) {
            val leftFrequencyHz =
                UserPreferenceDefaults.normalizeTinnitusPitchFrequencyHz(profile.leftFrequencyHz)
            val rightFrequencyHz =
                UserPreferenceDefaults.normalizeTinnitusPitchFrequencyHz(profile.rightFrequencyHz)
            val updatedAtMs =
                UserPreferenceDefaults.normalizeTinnitusPitchUpdatedAtMs(profile.updatedAtMs)
            context.dataStore.edit {
                if (leftFrequencyHz == null) {
                    it.remove(Keys.TINNITUS_LEFT_PITCH_HZ)
                } else {
                    it[Keys.TINNITUS_LEFT_PITCH_HZ] = leftFrequencyHz
                }
                if (rightFrequencyHz == null) {
                    it.remove(Keys.TINNITUS_RIGHT_PITCH_HZ)
                } else {
                    it[Keys.TINNITUS_RIGHT_PITCH_HZ] = rightFrequencyHz
                }
                if ((leftFrequencyHz == null && rightFrequencyHz == null) || updatedAtMs == null) {
                    it.remove(Keys.TINNITUS_PITCH_UPDATED_AT_MS)
                } else {
                    it[Keys.TINNITUS_PITCH_UPDATED_AT_MS] = updatedAtMs
                }
            }
        }

        suspend fun updateVoiceBaseline(levelDb: Float, sampleCount: Int, capturedAtMs: Long) {
            val normalizedLevelDb = UserPreferenceDefaults.normalizeVoiceBaselineLevelDb(levelDb)
            val normalizedSampleCount = UserPreferenceDefaults.normalizeVoiceBaselineSampleCount(sampleCount)
            val normalizedCapturedAtMs = UserPreferenceDefaults.normalizeVoiceBaselineCapturedAtMs(capturedAtMs)
            context.dataStore.edit {
                if (normalizedLevelDb == null || normalizedSampleCount <= 0 || normalizedCapturedAtMs == null) {
                    it.remove(Keys.VOICE_BASELINE_LEVEL_DB)
                    it.remove(Keys.VOICE_BASELINE_SAMPLE_COUNT)
                    it.remove(Keys.VOICE_BASELINE_CAPTURED_AT_MS)
                } else {
                    it[Keys.VOICE_BASELINE_LEVEL_DB] = normalizedLevelDb
                    it[Keys.VOICE_BASELINE_SAMPLE_COUNT] = normalizedSampleCount
                    it[Keys.VOICE_BASELINE_CAPTURED_AT_MS] = normalizedCapturedAtMs
                }
            }
        }

        suspend fun updateDebugForceFreeEnabled(enabled: Boolean) {
            context.dataStore.edit { it[Keys.DEBUG_FORCE_FREE] = enabled }
        }

        suspend fun updateProUser(isPro: Boolean) {
            context.dataStore.edit { it[Keys.IS_PRO_USER] = isPro }
        }
    }
