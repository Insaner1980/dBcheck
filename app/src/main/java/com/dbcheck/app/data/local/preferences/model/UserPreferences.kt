package com.dbcheck.app.data.local.preferences.model

import com.dbcheck.app.domain.ambient.AmbientSoundPreset
import com.dbcheck.app.domain.audio.ResponseTime
import com.dbcheck.app.domain.noise.DosimeterStandard
import com.dbcheck.app.domain.noise.NoiseNotificationSchedule
import com.dbcheck.app.domain.tinnitus.TinnitusPitchProfile

data class UserPreferences(
    val themeMode: String = UserPreferenceDefaults.THEME_MODE,
    val exposureAlertsEnabled: Boolean = UserPreferenceDefaults.EXPOSURE_ALERTS_ENABLED,
    val peakWarningsEnabled: Boolean = UserPreferenceDefaults.PEAK_WARNINGS_ENABLED,
    val notificationThreshold: Int = UserPreferenceDefaults.NOTIFICATION_THRESHOLD,
    val notificationSchedule: NoiseNotificationSchedule = UserPreferenceDefaults.notificationSchedule,
    val micSensitivityOffset: Float = UserPreferenceDefaults.MIC_SENSITIVITY_OFFSET,
    val frequencyWeighting: String = UserPreferenceDefaults.FREQUENCY_WEIGHTING,
    val responseTime: ResponseTime = UserPreferenceDefaults.responseTime,
    val dosimeterStandard: DosimeterStandard = UserPreferenceDefaults.dosimeterStandard,
    val selectedCalibrationProfileId: Long? = UserPreferenceDefaults.SELECTED_CALIBRATION_PROFILE_ID,
    val selectedAudioInputDeviceId: Int? = UserPreferenceDefaults.SELECTED_AUDIO_INPUT_DEVICE_ID,
    val waveformStyle: WaveformStyle = UserPreferenceDefaults.waveformStyle,
    val refreshRate: MeterRefreshRate = UserPreferenceDefaults.refreshRate,
    val lockscreenMeterEnabled: Boolean = UserPreferenceDefaults.LOCKSCREEN_METER_ENABLED,
    val showLockscreenMeterPublicly: Boolean = UserPreferenceDefaults.SHOW_LOCKSCREEN_METER_PUBLICLY,
    val healthConnectEnabled: Boolean = UserPreferenceDefaults.HEALTH_CONNECT_ENABLED,
    val heartRateOverlayEnabled: Boolean = UserPreferenceDefaults.HEART_RATE_OVERLAY_ENABLED,
    val technicalMetadataEnabled: Boolean = UserPreferenceDefaults.TECHNICAL_METADATA_ENABLED,
    val dosimeterCardEnabled: Boolean = UserPreferenceDefaults.DOSIMETER_CARD_ENABLED,
    val soundDetectionEnabled: Boolean = UserPreferenceDefaults.SOUND_DETECTION_ENABLED,
    val soundDetectionPersistenceEnabled: Boolean = UserPreferenceDefaults.SOUND_DETECTION_PERSISTENCE_ENABLED,
    val sleepCardEnabled: Boolean = UserPreferenceDefaults.SLEEP_CARD_ENABLED,
    val wavRecordingDefaultEnabled: Boolean = UserPreferenceDefaults.WAV_RECORDING_DEFAULT_ENABLED,
    val audibleAlarmEnabled: Boolean = UserPreferenceDefaults.AUDIBLE_ALARM_ENABLED,
    val ttsRiskPromptEnabled: Boolean = UserPreferenceDefaults.TTS_RISK_PROMPT_ENABLED,
    val ambientSoundPreset: AmbientSoundPreset = UserPreferenceDefaults.ambientSoundPreset,
    val ambientSoundVolume: Float = UserPreferenceDefaults.AMBIENT_SOUND_VOLUME,
    val ambientSoundTimerMinutes: Int = UserPreferenceDefaults.AMBIENT_SOUND_TIMER_MINUTES,
    val tinnitusPitchProfile: TinnitusPitchProfile = UserPreferenceDefaults.tinnitusPitchProfile,
    val voiceBaselineLevelDb: Float? = UserPreferenceDefaults.VOICE_BASELINE_LEVEL_DB,
    val voiceBaselineSampleCount: Int = UserPreferenceDefaults.VOICE_BASELINE_SAMPLE_COUNT,
    val voiceBaselineCapturedAtMs: Long? = UserPreferenceDefaults.VOICE_BASELINE_CAPTURED_AT_MS,
    val debugForceFreeEnabled: Boolean = UserPreferenceDefaults.DEBUG_FORCE_FREE_ENABLED,
    val isProUser: Boolean = UserPreferenceDefaults.IS_PRO_USER,
)
