package com.dbcheck.app.data.local.preferences.model

import com.dbcheck.app.domain.audio.ResponseTime
import com.dbcheck.app.domain.noise.DosimeterStandard

data class UserPreferences(
    val themeMode: String = UserPreferenceDefaults.THEME_MODE,
    val exposureAlertsEnabled: Boolean = UserPreferenceDefaults.EXPOSURE_ALERTS_ENABLED,
    val peakWarningsEnabled: Boolean = UserPreferenceDefaults.PEAK_WARNINGS_ENABLED,
    val notificationThreshold: Int = UserPreferenceDefaults.NOTIFICATION_THRESHOLD,
    val micSensitivityOffset: Float = UserPreferenceDefaults.MIC_SENSITIVITY_OFFSET,
    val frequencyWeighting: String = UserPreferenceDefaults.FREQUENCY_WEIGHTING,
    val responseTime: ResponseTime = UserPreferenceDefaults.responseTime,
    val dosimeterStandard: DosimeterStandard = UserPreferenceDefaults.dosimeterStandard,
    val selectedCalibrationProfileId: Long? = UserPreferenceDefaults.SELECTED_CALIBRATION_PROFILE_ID,
    val waveformStyle: WaveformStyle = UserPreferenceDefaults.waveformStyle,
    val refreshRate: MeterRefreshRate = UserPreferenceDefaults.refreshRate,
    val lockscreenMeterEnabled: Boolean = UserPreferenceDefaults.LOCKSCREEN_METER_ENABLED,
    val healthConnectEnabled: Boolean = UserPreferenceDefaults.HEALTH_CONNECT_ENABLED,
    val heartRateOverlayEnabled: Boolean = UserPreferenceDefaults.HEART_RATE_OVERLAY_ENABLED,
    val technicalMetadataEnabled: Boolean = UserPreferenceDefaults.TECHNICAL_METADATA_ENABLED,
    val dosimeterCardEnabled: Boolean = UserPreferenceDefaults.DOSIMETER_CARD_ENABLED,
    val soundDetectionEnabled: Boolean = UserPreferenceDefaults.SOUND_DETECTION_ENABLED,
    val soundDetectionPersistenceEnabled: Boolean = UserPreferenceDefaults.SOUND_DETECTION_PERSISTENCE_ENABLED,
    val sleepCardEnabled: Boolean = UserPreferenceDefaults.SLEEP_CARD_ENABLED,
    val wavRecordingDefaultEnabled: Boolean = UserPreferenceDefaults.WAV_RECORDING_DEFAULT_ENABLED,
    val debugForceFreeEnabled: Boolean = UserPreferenceDefaults.DEBUG_FORCE_FREE_ENABLED,
    val isProUser: Boolean = UserPreferenceDefaults.IS_PRO_USER,
)
