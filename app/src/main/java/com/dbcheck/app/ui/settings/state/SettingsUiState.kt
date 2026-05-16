package com.dbcheck.app.ui.settings.state

import com.dbcheck.app.data.local.preferences.model.UserPreferences

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val healthConnectStatus: HealthConnectUiState = HealthConnectUiState(),
    val healthConnectErrorMessage: String? = null,
    val isPurchaseLaunching: Boolean = false,
    val purchaseMessage: String? = null,
    val purchaseErrorMessage: String? = null,
    val isCsvExporting: Boolean = false,
    val csvExportMessage: String? = null,
    val csvExportErrorMessage: String? = null,
    val localBackups: List<LocalBackupUiState> = emptyList(),
    val isBackupCreating: Boolean = false,
    val isBackupRestoring: Boolean = false,
    val restoreCandidate: LocalBackupUiState? = null,
    val backupMessage: String? = null,
    val backupErrorMessage: String? = null,
) {
    val themeMode: String get() = preferences.themeMode
    val exposureAlertsEnabled: Boolean get() = preferences.exposureAlertsEnabled
    val peakWarningsEnabled: Boolean get() = preferences.peakWarningsEnabled
    val notificationThreshold: Int get() = preferences.notificationThreshold
    val micSensitivityOffset: Float get() = preferences.micSensitivityOffset
    val frequencyWeighting: String get() = preferences.frequencyWeighting
    val waveformStyle get() = preferences.waveformStyle
    val refreshRate get() = preferences.refreshRate
    val lockscreenMeterEnabled: Boolean get() = preferences.lockscreenMeterEnabled
    val healthConnectEnabled: Boolean get() = preferences.healthConnectEnabled
    val heartRateOverlayEnabled: Boolean get() = preferences.heartRateOverlayEnabled
    val debugForceFreeEnabled: Boolean get() = preferences.debugForceFreeEnabled
    val isProUser: Boolean get() = preferences.isProUser
}

enum class HealthConnectAvailabilityUi {
    AVAILABLE,
    UNAVAILABLE,
    UPDATE_REQUIRED,
}

data class HealthConnectUiState(
    val availability: HealthConnectAvailabilityUi = HealthConnectAvailabilityUi.UNAVAILABLE,
    val noiseSyncGranted: Boolean = false,
    val heartRateReadGranted: Boolean = false,
    val noiseSyncPermissions: Set<String> = emptySet(),
    val heartRateReadPermissions: Set<String> = emptySet(),
) {
    val isAvailable: Boolean
        get() = availability == HealthConnectAvailabilityUi.AVAILABLE

    val requiresInstall: Boolean
        get() = availability == HealthConnectAvailabilityUi.UPDATE_REQUIRED

    val label: String
        get() =
            when (availability) {
                HealthConnectAvailabilityUi.AVAILABLE ->
                    when {
                        noiseSyncGranted && heartRateReadGranted -> "Connected"
                        noiseSyncGranted || heartRateReadGranted -> "Partially connected"
                        else -> "Ready for permission setup"
                    }

                HealthConnectAvailabilityUi.UPDATE_REQUIRED -> "Install or update Health Connect"
                HealthConnectAvailabilityUi.UNAVAILABLE -> "Unavailable on this device"
            }
}

data class LocalBackupUiState(
    val filePath: String,
    val fileName: String,
    val createdAtMillis: Long,
    val sizeBytes: Long,
)
