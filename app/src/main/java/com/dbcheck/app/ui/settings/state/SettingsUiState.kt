package com.dbcheck.app.ui.settings.state

import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.data.local.preferences.model.UserPreferenceDefaults
import com.dbcheck.app.data.local.preferences.model.WaveformStyle

data class SettingsUiState(
    val themeMode: String = UserPreferenceDefaults.THEME_MODE,
    val exposureAlertsEnabled: Boolean = UserPreferenceDefaults.EXPOSURE_ALERTS_ENABLED,
    val peakWarningsEnabled: Boolean = UserPreferenceDefaults.PEAK_WARNINGS_ENABLED,
    val notificationThreshold: Int = UserPreferenceDefaults.NOTIFICATION_THRESHOLD,
    val micSensitivityOffset: Float = UserPreferenceDefaults.MIC_SENSITIVITY_OFFSET,
    val frequencyWeighting: String = UserPreferenceDefaults.FREQUENCY_WEIGHTING,
    val waveformStyle: WaveformStyle = UserPreferenceDefaults.waveformStyle,
    val refreshRate: MeterRefreshRate = UserPreferenceDefaults.refreshRate,
    val lockscreenMeterEnabled: Boolean = UserPreferenceDefaults.LOCKSCREEN_METER_ENABLED,
    val healthConnectEnabled: Boolean = UserPreferenceDefaults.HEALTH_CONNECT_ENABLED,
    val heartRateOverlayEnabled: Boolean = UserPreferenceDefaults.HEART_RATE_OVERLAY_ENABLED,
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
    val debugForceFreeEnabled: Boolean = UserPreferenceDefaults.DEBUG_FORCE_FREE_ENABLED,
    val isProUser: Boolean = UserPreferenceDefaults.IS_PRO_USER,
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
