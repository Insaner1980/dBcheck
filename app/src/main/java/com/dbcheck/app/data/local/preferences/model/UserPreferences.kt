package com.dbcheck.app.data.local.preferences.model

data class UserPreferences(
    val themeMode: String = "system",
    val exposureAlertsEnabled: Boolean = true,
    val peakWarningsEnabled: Boolean = false,
    val notificationThreshold: Int = 85,
    val micSensitivityOffset: Float = 0f,
    val frequencyWeighting: String = "A",
    val waveformStyle: String = "default",
    val refreshRate: String = "standard",
    val isProUser: Boolean = false,
)
