package com.dbcheck.app.ui.settings.state

import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.local.preferences.model.WaveformStyle

private val defaultSettingsPreferences = UserPreferences()

data class SettingsUiState(
    val themeMode: String = defaultSettingsPreferences.themeMode,
    val exposureAlertsEnabled: Boolean = defaultSettingsPreferences.exposureAlertsEnabled,
    val peakWarningsEnabled: Boolean = defaultSettingsPreferences.peakWarningsEnabled,
    val notificationThreshold: Int = defaultSettingsPreferences.notificationThreshold,
    val micSensitivityOffset: Float = defaultSettingsPreferences.micSensitivityOffset,
    val frequencyWeighting: String = defaultSettingsPreferences.frequencyWeighting,
    val waveformStyle: WaveformStyle = defaultSettingsPreferences.waveformStyle,
    val refreshRate: MeterRefreshRate = defaultSettingsPreferences.refreshRate,
    val lockscreenMeterEnabled: Boolean = defaultSettingsPreferences.lockscreenMeterEnabled,
    val healthConnectEnabled: Boolean = defaultSettingsPreferences.healthConnectEnabled,
    val heartRateOverlayEnabled: Boolean = defaultSettingsPreferences.heartRateOverlayEnabled,
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
    val debugForceFreeEnabled: Boolean = defaultSettingsPreferences.debugForceFreeEnabled,
    val isProUser: Boolean = defaultSettingsPreferences.isProUser,
)

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
