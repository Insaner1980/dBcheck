package com.dbcheck.app.data.local.preferences.model

import com.dbcheck.app.domain.audio.ResponseTime
import com.dbcheck.app.domain.audio.WeightingType
import com.dbcheck.app.domain.calibration.CalibrationOffsetPolicy
import com.dbcheck.app.domain.noise.DosimeterStandard
import com.dbcheck.app.domain.noise.NoiseNotificationSchedule

@Suppress("TooManyFunctions")
object UserPreferenceDefaults {
    const val THEME_MODE = "system"
    const val EXPOSURE_ALERTS_ENABLED = true
    const val PEAK_WARNINGS_ENABLED = false
    const val NOTIFICATION_THRESHOLD_MIN = 60
    const val NOTIFICATION_THRESHOLD_MAX = 110
    const val NOTIFICATION_THRESHOLD = 85
    val notificationSchedule = NoiseNotificationSchedule()
    const val MIC_SENSITIVITY_OFFSET_MIN = CalibrationOffsetPolicy.MIN_OFFSET_DB
    const val MIC_SENSITIVITY_OFFSET_MAX = CalibrationOffsetPolicy.MAX_OFFSET_DB
    const val MIC_SENSITIVITY_OFFSET = CalibrationOffsetPolicy.DEFAULT_OFFSET_DB
    const val FREQUENCY_WEIGHTING = WeightingType.DEFAULT_PREFERENCE_VALUE
    val responseTime = ResponseTime.FAST
    val dosimeterStandard = DosimeterStandard.NIOSH_REL
    val SELECTED_CALIBRATION_PROFILE_ID: Long? = null
    val SELECTED_AUDIO_INPUT_DEVICE_ID: Int? = null
    val waveformStyle = WaveformStyle.LINE
    val refreshRate = MeterRefreshRate.STANDARD
    const val LOCKSCREEN_METER_ENABLED = false
    const val SHOW_LOCKSCREEN_METER_PUBLICLY = false
    const val HEALTH_CONNECT_ENABLED = false
    const val HEART_RATE_OVERLAY_ENABLED = false
    const val TECHNICAL_METADATA_ENABLED = true
    const val DOSIMETER_CARD_ENABLED = true
    const val SOUND_DETECTION_ENABLED = false
    const val SOUND_DETECTION_PERSISTENCE_ENABLED = false
    const val SLEEP_CARD_ENABLED = false
    const val WAV_RECORDING_DEFAULT_ENABLED = false
    const val AUDIBLE_ALARM_ENABLED = false
    const val TTS_RISK_PROMPT_ENABLED = false
    val VOICE_BASELINE_LEVEL_DB: Float? = null
    const val VOICE_BASELINE_SAMPLE_COUNT = 0
    val VOICE_BASELINE_CAPTURED_AT_MS: Long? = null
    const val DEBUG_FORCE_FREE_ENABLED = false
    const val IS_PRO_USER = false

    fun normalizeThemeMode(mode: String?): String = ThemeMode.fromPreference(mode).preferenceValue

    fun normalizeNotificationThreshold(threshold: Int?): Int = threshold?.coerceIn(
        NOTIFICATION_THRESHOLD_MIN,
        NOTIFICATION_THRESHOLD_MAX,
    ) ?: NOTIFICATION_THRESHOLD

    fun normalizeNotificationSchedule(
        activeDaysPreferenceValue: String?,
        startMinuteOfDay: Int?,
        endMinuteOfDay: Int?,
    ): NoiseNotificationSchedule = NoiseNotificationSchedule.normalized(
            activeDays = NoiseNotificationSchedule.activeDaysFromPreference(activeDaysPreferenceValue),
            startMinuteOfDay = startMinuteOfDay,
            endMinuteOfDay = endMinuteOfDay,
        )

    fun normalizeMicSensitivityOffset(offset: Float?): Float = CalibrationOffsetPolicy.normalizeOffsetDb(offset)

    fun normalizeFrequencyWeighting(weighting: String?): String = WeightingType.fromPreference(weighting).name

    fun normalizeResponseTime(responseTime: String?): ResponseTime = ResponseTime.fromPreference(responseTime)

    fun normalizeDosimeterStandard(standard: String?): DosimeterStandard = DosimeterStandard.fromPreference(standard)

    fun normalizeSelectedCalibrationProfileId(profileId: Long?): Long? = profileId?.takeIf { it > 0L }

    fun normalizeSelectedAudioInputDeviceId(deviceId: Int?): Int? = deviceId?.takeIf { it >= 0 }

    fun normalizeVoiceBaselineLevelDb(levelDb: Float?): Float? =
        levelDb?.takeIf { it.isFinite() }?.coerceIn(VOICE_BASELINE_LEVEL_DB_MIN, VOICE_BASELINE_LEVEL_DB_MAX)

    fun normalizeVoiceBaselineSampleCount(sampleCount: Int?): Int =
        sampleCount?.takeIf { it > 0 } ?: VOICE_BASELINE_SAMPLE_COUNT

    fun normalizeVoiceBaselineCapturedAtMs(capturedAtMs: Long?): Long? = capturedAtMs?.takeIf { it > 0L }

    private const val VOICE_BASELINE_LEVEL_DB_MIN = 0f
    private const val VOICE_BASELINE_LEVEL_DB_MAX = 140f
}
