package com.dbcheck.app.data.local.preferences.model

import com.dbcheck.app.domain.audio.WeightingType

object UserPreferenceDefaults {
    const val THEME_MODE = "system"
    const val EXPOSURE_ALERTS_ENABLED = true
    const val PEAK_WARNINGS_ENABLED = false
    const val NOTIFICATION_THRESHOLD_MIN = 60
    const val NOTIFICATION_THRESHOLD_MAX = 110
    const val NOTIFICATION_THRESHOLD = 85
    const val MIC_SENSITIVITY_OFFSET_MIN = -10f
    const val MIC_SENSITIVITY_OFFSET_MAX = 10f
    const val MIC_SENSITIVITY_OFFSET = 0f
    const val FREQUENCY_WEIGHTING = WeightingType.DEFAULT_PREFERENCE_VALUE
    val waveformStyle = WaveformStyle.LINE
    val refreshRate = MeterRefreshRate.STANDARD
    const val LOCKSCREEN_METER_ENABLED = false
    const val HEALTH_CONNECT_ENABLED = false
    const val HEART_RATE_OVERLAY_ENABLED = false
    const val DEBUG_FORCE_FREE_ENABLED = false
    const val IS_PRO_USER = false

    fun normalizeThemeMode(mode: String?): String = ThemeMode.fromPreference(mode).preferenceValue

    fun normalizeNotificationThreshold(threshold: Int?): Int = threshold?.coerceIn(
        NOTIFICATION_THRESHOLD_MIN,
        NOTIFICATION_THRESHOLD_MAX,
    ) ?: NOTIFICATION_THRESHOLD

    fun normalizeMicSensitivityOffset(offset: Float?): Float = offset
            ?.takeIf { it.isFinite() }
            ?.coerceIn(MIC_SENSITIVITY_OFFSET_MIN, MIC_SENSITIVITY_OFFSET_MAX)
            ?: MIC_SENSITIVITY_OFFSET

    fun normalizeFrequencyWeighting(weighting: String?): String = WeightingType.fromPreference(weighting).name
}
