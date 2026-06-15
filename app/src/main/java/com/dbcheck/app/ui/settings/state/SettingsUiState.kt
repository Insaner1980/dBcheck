package com.dbcheck.app.ui.settings.state

import androidx.annotation.StringRes
import com.dbcheck.app.R
import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.local.preferences.model.WaveformStyle
import com.dbcheck.app.domain.audio.ResponseTime
import com.dbcheck.app.domain.noise.DosimeterStandard

private val defaultSettingsPreferences = UserPreferences()

data class SettingsUiState(
    val themeMode: String = defaultSettingsPreferences.themeMode,
    val exposureAlertsEnabled: Boolean = defaultSettingsPreferences.exposureAlertsEnabled,
    val peakWarningsEnabled: Boolean = defaultSettingsPreferences.peakWarningsEnabled,
    val notificationThreshold: Int = defaultSettingsPreferences.notificationThreshold,
    val micSensitivityOffset: Float = defaultSettingsPreferences.micSensitivityOffset,
    val frequencyWeighting: String = defaultSettingsPreferences.frequencyWeighting,
    val responseTime: ResponseTime = defaultSettingsPreferences.responseTime,
    val dosimeterStandard: DosimeterStandard = defaultSettingsPreferences.dosimeterStandard,
    val waveformStyle: WaveformStyle = defaultSettingsPreferences.waveformStyle,
    val refreshRate: MeterRefreshRate = defaultSettingsPreferences.refreshRate,
    val lockscreenMeterEnabled: Boolean = defaultSettingsPreferences.lockscreenMeterEnabled,
    val healthConnectEnabled: Boolean = defaultSettingsPreferences.healthConnectEnabled,
    val heartRateOverlayEnabled: Boolean = defaultSettingsPreferences.heartRateOverlayEnabled,
    val wavRecordingDefaultEnabled: Boolean = defaultSettingsPreferences.wavRecordingDefaultEnabled,
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

    @get:StringRes
    val labelRes: Int
        get() =
            when (availability) {
                HealthConnectAvailabilityUi.AVAILABLE ->
                    when {
                        noiseSyncGranted && heartRateReadGranted -> R.string.health_connect_connected
                        noiseSyncGranted || heartRateReadGranted -> R.string.health_connect_partially_connected
                        else -> R.string.health_connect_ready_for_permission_setup
                    }

                HealthConnectAvailabilityUi.UPDATE_REQUIRED -> R.string.health_connect_install_or_update

                HealthConnectAvailabilityUi.UNAVAILABLE -> R.string.health_connect_unavailable
            }
}

data class LocalBackupUiState(
    val filePath: String,
    val fileName: String,
    val displayName: String,
    val createdAtMillis: Long,
    val sizeBytes: Long,
)
