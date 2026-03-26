package com.dbcheck.app.ui.settings.state

data class SettingsUiState(
    val themeMode: String = "system",
    val exposureAlertsEnabled: Boolean = true,
    val peakWarningsEnabled: Boolean = false,
    val notificationThreshold: Int = 85,
    val micSensitivityOffset: Float = 0f,
    val frequencyWeighting: String = "A",
    val isProUser: Boolean = false,
)
