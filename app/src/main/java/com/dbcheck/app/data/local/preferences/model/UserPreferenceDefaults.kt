package com.dbcheck.app.data.local.preferences.model

import com.dbcheck.app.domain.audio.ResponseTime
import com.dbcheck.app.domain.audio.WeightingType
import com.dbcheck.app.domain.calibration.CalibrationOffsetPolicy
import com.dbcheck.app.domain.noise.DosimeterStandard

object UserPreferenceDefaults {
    const val THEME_MODE = "system"
    const val EXPOSURE_ALERTS_ENABLED = true
    const val PEAK_WARNINGS_ENABLED = false
    const val NOTIFICATION_THRESHOLD_MIN = 60
    const val NOTIFICATION_THRESHOLD_MAX = 110
    const val NOTIFICATION_THRESHOLD = 85
    const val MIC_SENSITIVITY_OFFSET_MIN = CalibrationOffsetPolicy.MIN_OFFSET_DB
    const val MIC_SENSITIVITY_OFFSET_MAX = CalibrationOffsetPolicy.MAX_OFFSET_DB
    const val MIC_SENSITIVITY_OFFSET = CalibrationOffsetPolicy.DEFAULT_OFFSET_DB
    const val FREQUENCY_WEIGHTING = WeightingType.DEFAULT_PREFERENCE_VALUE
    val responseTime = ResponseTime.FAST
    val dosimeterStandard = DosimeterStandard.NIOSH_REL
    val SELECTED_CALIBRATION_PROFILE_ID: Long? = null
    val waveformStyle = WaveformStyle.LINE
    val refreshRate = MeterRefreshRate.STANDARD
    const val LOCKSCREEN_METER_ENABLED = false
    const val HEALTH_CONNECT_ENABLED = false
    const val HEART_RATE_OVERLAY_ENABLED = false
    const val TECHNICAL_METADATA_ENABLED = true
    const val DOSIMETER_CARD_ENABLED = true
    const val SOUND_DETECTION_ENABLED = false
    const val SOUND_DETECTION_PERSISTENCE_ENABLED = false
    const val SLEEP_CARD_ENABLED = false
    const val WAV_RECORDING_DEFAULT_ENABLED = false
    const val DEBUG_FORCE_FREE_ENABLED = false
    const val IS_PRO_USER = false

    fun normalizeThemeMode(mode: String?): String = ThemeMode.fromPreference(mode).preferenceValue

    fun normalizeNotificationThreshold(threshold: Int?): Int = threshold?.coerceIn(
        NOTIFICATION_THRESHOLD_MIN,
        NOTIFICATION_THRESHOLD_MAX,
    ) ?: NOTIFICATION_THRESHOLD

    fun normalizeMicSensitivityOffset(offset: Float?): Float = CalibrationOffsetPolicy.normalizeOffsetDb(offset)

    fun normalizeFrequencyWeighting(weighting: String?): String = WeightingType.fromPreference(weighting).name

    fun normalizeResponseTime(responseTime: String?): ResponseTime = ResponseTime.fromPreference(responseTime)

    fun normalizeDosimeterStandard(standard: String?): DosimeterStandard = DosimeterStandard.fromPreference(standard)

    fun normalizeSelectedCalibrationProfileId(profileId: Long?): Long? = profileId?.takeIf { it > 0L }
}
