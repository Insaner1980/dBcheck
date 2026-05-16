package com.dbcheck.app.ui.settings

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dbcheck.app.billing.BillingGateway
import com.dbcheck.app.billing.PurchaseEvent
import com.dbcheck.app.billing.PurchaseLaunchResult
import com.dbcheck.app.data.export.ExportCsvUseCase
import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.data.local.preferences.model.ProAudioPreferencePolicy
import com.dbcheck.app.data.local.preferences.model.UserPreferenceDefaults
import com.dbcheck.app.data.local.preferences.model.WaveformStyle
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.service.AudioSessionManager
import com.dbcheck.app.service.BackupService
import com.dbcheck.app.service.HealthConnectService
import com.dbcheck.app.service.HealthConnectServiceAvailability
import com.dbcheck.app.service.HealthConnectServiceStatus
import com.dbcheck.app.service.LocalBackupInfo
import com.dbcheck.app.service.LocalBackupResult
import com.dbcheck.app.service.LocalRestoreResult
import com.dbcheck.app.ui.settings.state.HealthConnectAvailabilityUi
import com.dbcheck.app.ui.settings.state.HealthConnectUiState
import com.dbcheck.app.ui.settings.state.LocalBackupUiState
import com.dbcheck.app.ui.settings.state.SettingsUiState
import com.dbcheck.app.util.toUserFacingMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DisplayPreferenceUpdate {
    data class WaveformStyleChange(val style: WaveformStyle) : DisplayPreferenceUpdate

    data class RefreshRateChange(val rate: MeterRefreshRate) : DisplayPreferenceUpdate
}

sealed interface NoiseNotificationUpdate {
    data class ExposureAlerts(val enabled: Boolean) : NoiseNotificationUpdate

    data class PeakWarnings(val enabled: Boolean) : NoiseNotificationUpdate

    data class NotificationThreshold(val threshold: Int) : NoiseNotificationUpdate
}

enum class HealthConnectIntentTarget {
    INSTALL,
    MANAGE_DATA,
}

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val preferencesRepository: PreferencesRepository,
        private val healthConnectService: HealthConnectService,
        private val billingGateway: BillingGateway,
        private val exportCsvUseCase: ExportCsvUseCase,
        private val backupService: BackupService,
        private val audioSessionManager: AudioSessionManager,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SettingsUiState())
        val uiState: StateFlow<SettingsUiState> = _uiState
        private val _events = MutableSharedFlow<SettingsEvent>(extraBufferCapacity = 1)
        val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()
        private val _csvExportIntents = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
        val csvExportIntents: SharedFlow<Intent> = _csvExportIntents.asSharedFlow()

        init {
            viewModelScope.launch {
                preferencesRepository.userPreferences.collect { prefs ->
                    _uiState.update {
                        it.copy(preferences = prefs)
                    }
                }
            }
            viewModelScope.launch {
                billingGateway.purchaseEvents.collect { event ->
                    handlePurchaseEvent(event, _uiState)
                }
            }
            refreshLocalBackups(backupService, _uiState)
            refreshHealthConnectStatus()
        }

        fun updateNoiseNotification(update: NoiseNotificationUpdate) {
            viewModelScope.launch {
                when (update) {
                    is NoiseNotificationUpdate.ExposureAlerts ->
                        preferencesRepository.updateExposureAlerts(update.enabled)

                    is NoiseNotificationUpdate.PeakWarnings ->
                        preferencesRepository.updatePeakWarnings(update.enabled)

                    is NoiseNotificationUpdate.NotificationThreshold ->
                        preferencesRepository.updateNotificationThreshold(
                            UserPreferenceDefaults.normalizeNotificationThreshold(update.threshold),
                        )
                }
            }
        }

        fun updateMicSensitivity(offset: Float) {
            if (!ProAudioPreferencePolicy.canUseProAudioPreferences(_uiState.value.isProUser)) return

            val normalized = UserPreferenceDefaults.normalizeMicSensitivityOffset(offset)
            viewModelScope.launch { preferencesRepository.updateMicSensitivityOffset(normalized) }
        }

        fun updateFrequencyWeighting(weighting: String) {
            if (!ProAudioPreferencePolicy.canUseProAudioPreferences(_uiState.value.isProUser)) return

            val normalized = UserPreferenceDefaults.normalizeFrequencyWeighting(weighting)
            viewModelScope.launch { preferencesRepository.updateFrequencyWeighting(normalized) }
        }

        fun updateThemeMode(mode: String) {
            viewModelScope.launch {
                preferencesRepository.updateThemeMode(UserPreferenceDefaults.normalizeThemeMode(mode))
            }
        }

        fun updateDisplayPreference(update: DisplayPreferenceUpdate) {
            viewModelScope.launch {
                when (update) {
                    is DisplayPreferenceUpdate.WaveformStyleChange ->
                        preferencesRepository.updateWaveformStyle(update.style)

                    is DisplayPreferenceUpdate.RefreshRateChange ->
                        preferencesRepository.updateRefreshRate(update.rate)
                }
            }
        }

        fun updateLockscreenMeter(enabled: Boolean) {
            viewModelScope.launch { preferencesRepository.updateLockscreenMeterEnabled(enabled) }
        }

        fun updateHealthConnectEnabled(enabled: Boolean) {
            viewModelScope.launch { preferencesRepository.updateHealthConnectEnabled(enabled) }
        }

        fun updateHeartRateOverlayEnabled(enabled: Boolean) {
            viewModelScope.launch { preferencesRepository.updateHeartRateOverlayEnabled(enabled) }
        }

        fun updateDebugForceFree(enabled: Boolean) {
            viewModelScope.launch { preferencesRepository.updateDebugForceFreeEnabled(enabled) }
        }

        fun launchProPurchase(activity: Activity) {
            if (_uiState.value.isPurchaseLaunching) return

            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        isPurchaseLaunching = true,
                        purchaseMessage = null,
                        purchaseErrorMessage = null,
                    )
                }
                when (val result = billingGateway.launchPurchaseFlow(activity)) {
                    PurchaseLaunchResult.Started ->
                        _uiState.update { it.copy(isPurchaseLaunching = false) }

                    PurchaseLaunchResult.AlreadyOwned ->
                        _uiState.update {
                            it.copy(
                                isPurchaseLaunching = false,
                                purchaseMessage = "dBcheck Pro already unlocked",
                            )
                        }

                    is PurchaseLaunchResult.Unavailable ->
                        showPurchaseLaunchFailure(result.reason, _uiState)

                    is PurchaseLaunchResult.Failed ->
                        showPurchaseLaunchFailure(result.reason, _uiState)
                }
            }
        }

        fun onPurchaseActivityUnavailable() {
            _uiState.update {
                it.copy(
                    isPurchaseLaunching = false,
                    purchaseMessage = null,
                    purchaseErrorMessage = "Unable to open Google Play purchase flow",
                )
            }
        }

        fun clearPurchaseMessages() {
            _uiState.update { it.copy(purchaseMessage = null, purchaseErrorMessage = null) }
        }

        fun createCsvExportIntent() {
            val current = _uiState.value
            when {
                !current.isProUser -> {
                    _uiState.update {
                        it.copy(
                            csvExportMessage = null,
                            csvExportErrorMessage = "CSV export requires dBcheck Pro",
                        )
                    }
                }

                current.isCsvExporting -> Unit

                else -> {
                    _uiState.update {
                        it.copy(
                            isCsvExporting = true,
                            csvExportMessage = null,
                            csvExportErrorMessage = null,
                        )
                    }
                    viewModelScope.launch {
                        runCatching { exportCsvUseCase.export() }
                            .onSuccess { intent ->
                                _uiState.update { state -> state.copy(isCsvExporting = false) }
                                _csvExportIntents.emit(intent)
                            }.onFailure { error ->
                                _uiState.update {
                                    it.copy(
                                        isCsvExporting = false,
                                        csvExportErrorMessage = error.toUserFacingMessage("CSV export failed"),
                                    )
                                }
                            }
                    }
                }
            }
        }

        fun onCsvShareStarted() {
            _uiState.update {
                it.copy(
                    csvExportMessage = "CSV export ready",
                    csvExportErrorMessage = null,
                )
            }
        }

        fun onCsvShareUnavailable() {
            _uiState.update {
                it.copy(
                    isCsvExporting = false,
                    csvExportMessage = null,
                    csvExportErrorMessage = "No app available to export CSV",
                )
            }
        }

        fun clearCsvExportMessages() {
            _uiState.update { it.copy(csvExportMessage = null, csvExportErrorMessage = null) }
        }

        fun createLocalBackup() {
            if (!ensureBackupAllowed(audioSessionManager, _uiState) || _uiState.value.isBackupOperationRunning()) return

            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        isBackupCreating = true,
                        backupMessage = null,
                        backupErrorMessage = null,
                    )
                }
                when (val result = backupService.createLocalBackup()) {
                    is LocalBackupResult.Created -> {
                        refreshLocalBackups(backupService, _uiState)
                        _uiState.update {
                            it.copy(
                                isBackupCreating = false,
                                backupMessage = "Backup created",
                                backupErrorMessage = null,
                            )
                        }
                    }

                    is LocalBackupResult.Failed ->
                        _uiState.update {
                            it.copy(
                                isBackupCreating = false,
                                backupMessage = null,
                                backupErrorMessage = result.reason,
                            )
                        }
                }
            }
        }

        fun requestRestoreBackup(backup: LocalBackupUiState) {
            if (!ensureBackupAllowed(audioSessionManager, _uiState) || _uiState.value.isBackupOperationRunning()) return

            _uiState.update {
                it.copy(
                    restoreCandidate = backup,
                    backupMessage = null,
                    backupErrorMessage = null,
                )
            }
        }

        fun dismissRestoreBackup() {
            if (_uiState.value.isBackupRestoring) return
            _uiState.update { it.copy(restoreCandidate = null) }
        }

        fun confirmRestoreBackup() {
            val backup = _uiState.value.restoreCandidate?.toBackupInfo() ?: return
            if (!ensureBackupAllowed(audioSessionManager, _uiState) || _uiState.value.isBackupOperationRunning()) return

            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        isBackupRestoring = true,
                        backupMessage = null,
                        backupErrorMessage = null,
                    )
                }
                when (val result = backupService.restoreFromBackup(backup)) {
                    is LocalRestoreResult.Restored -> {
                        refreshLocalBackups(backupService, _uiState)
                        _uiState.update {
                            it.copy(
                                isBackupRestoring = false,
                                restoreCandidate = null,
                                backupMessage = "Backup restored",
                                backupErrorMessage = null,
                            )
                        }
                        _events.emit(SettingsEvent.RestartAfterRestore)
                    }

                    is LocalRestoreResult.Failed -> {
                        _uiState.update {
                            it.copy(
                                isBackupRestoring = false,
                                restoreCandidate = null,
                                backupMessage = null,
                                backupErrorMessage = result.reason,
                            )
                        }
                        if (result.restartRequired) {
                            _events.emit(SettingsEvent.RestartAfterRestore)
                        }
                    }
                }
            }
        }

        fun clearBackupMessages() {
            _uiState.update { it.copy(backupMessage = null, backupErrorMessage = null) }
        }

        fun onHealthConnectInstallUnavailable() {
            _uiState.update { it.copy(healthConnectErrorMessage = "Unable to open Health Connect") }
        }

        fun clearHealthConnectMessages() {
            _uiState.update { it.copy(healthConnectErrorMessage = null) }
        }

        fun refreshHealthConnectStatus() {
            viewModelScope.launch {
                _uiState.update {
                    it.copy(healthConnectStatus = healthConnectService.getStatus().toUiState())
                }
            }
        }

        fun createHealthConnectIntent(target: HealthConnectIntentTarget): Intent = when (target) {
            HealthConnectIntentTarget.INSTALL -> healthConnectService.createInstallIntent()
            HealthConnectIntentTarget.MANAGE_DATA -> healthConnectService.createManageDataIntent()
        }
    }

private fun showPurchaseLaunchFailure(reason: String, uiState: MutableStateFlow<SettingsUiState>) {
    uiState.update {
        it.copy(
            isPurchaseLaunching = false,
            purchaseMessage = null,
            purchaseErrorMessage = reason,
        )
    }
}

private fun handlePurchaseEvent(event: PurchaseEvent, uiState: MutableStateFlow<SettingsUiState>) {
    uiState.update { current ->
        when (event) {
            PurchaseEvent.Completed ->
                current.copy(
                    isPurchaseLaunching = false,
                    purchaseMessage = "dBcheck Pro unlocked",
                    purchaseErrorMessage = null,
                )

            PurchaseEvent.Pending ->
                current.copy(
                    isPurchaseLaunching = false,
                    purchaseMessage = "Purchase pending. Complete payment in Google Play to unlock dBcheck Pro",
                    purchaseErrorMessage = null,
                )

            PurchaseEvent.AlreadyOwned ->
                current.copy(
                    isPurchaseLaunching = false,
                    purchaseMessage = "dBcheck Pro already unlocked",
                    purchaseErrorMessage = null,
                )

            PurchaseEvent.Cancelled ->
                current.copy(
                    isPurchaseLaunching = false,
                    purchaseErrorMessage = null,
                )

            is PurchaseEvent.Failed ->
                current.copy(
                    isPurchaseLaunching = false,
                    purchaseMessage = null,
                    purchaseErrorMessage = event.reason,
                )
        }
    }
}

private fun refreshLocalBackups(backupService: BackupService, uiState: MutableStateFlow<SettingsUiState>) {
    runCatching { backupService.listBackups() }
        .onSuccess { backups ->
            uiState.update { it.copy(localBackups = backups.map { backup -> backup.toUiState() }) }
        }.onFailure { error ->
            uiState.update {
                it.copy(backupErrorMessage = error.toUserFacingMessage("Unable to load backups"))
            }
        }
}

private fun HealthConnectServiceStatus.toUiState(): HealthConnectUiState = HealthConnectUiState(
        availability =
            when (availability) {
                HealthConnectServiceAvailability.AVAILABLE -> HealthConnectAvailabilityUi.AVAILABLE
                HealthConnectServiceAvailability.UNAVAILABLE -> HealthConnectAvailabilityUi.UNAVAILABLE
                HealthConnectServiceAvailability.UPDATE_REQUIRED -> HealthConnectAvailabilityUi.UPDATE_REQUIRED
            },
        noiseSyncGranted = noiseSyncGranted,
        heartRateReadGranted = heartRateReadGranted,
        noiseSyncPermissions = noiseSyncPermissions,
        heartRateReadPermissions = heartRateReadPermissions,
    )

private fun LocalBackupInfo.toUiState(): LocalBackupUiState = LocalBackupUiState(
        filePath = filePath,
        fileName = fileName,
        createdAtMillis = createdAtMillis,
        sizeBytes = sizeBytes,
    )

private fun LocalBackupUiState.toBackupInfo(): LocalBackupInfo = LocalBackupInfo(
        filePath = filePath,
        fileName = fileName,
        createdAtMillis = createdAtMillis,
        sizeBytes = sizeBytes,
    )

private fun ensureBackupAllowed(
    audioSessionManager: AudioSessionManager,
    uiState: MutableStateFlow<SettingsUiState>,
): Boolean {
    if (!audioSessionManager.isRecording.value) return true

    uiState.update {
        it.copy(
            isBackupCreating = false,
            isBackupRestoring = false,
            restoreCandidate = null,
            backupMessage = null,
            backupErrorMessage = "Stop recording before managing backups",
        )
    }
    return false
}

private fun SettingsUiState.isBackupOperationRunning(): Boolean = isBackupCreating || isBackupRestoring
