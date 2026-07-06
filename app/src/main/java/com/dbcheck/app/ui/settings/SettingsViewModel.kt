package com.dbcheck.app.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dbcheck.app.R
import com.dbcheck.app.billing.BillingGateway
import com.dbcheck.app.billing.PurchaseEvent
import com.dbcheck.app.billing.PurchaseLaunchResult
import com.dbcheck.app.data.export.ExportCsvUseCase
import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.data.local.preferences.model.ProAudioPreferencePolicy
import com.dbcheck.app.data.local.preferences.model.UserPreferenceDefaults
import com.dbcheck.app.data.local.preferences.model.WaveformStyle
import com.dbcheck.app.data.repository.CalibrationProfileDeleteResult
import com.dbcheck.app.data.repository.CalibrationProfileRepository
import com.dbcheck.app.data.repository.PassiveMonitoringRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.audio.AudioInputDevice
import com.dbcheck.app.domain.audio.ResponseTime
import com.dbcheck.app.domain.calibration.CalibrationProfile
import com.dbcheck.app.domain.calibration.OctaveCalibrationOffsets
import com.dbcheck.app.domain.noise.DosimeterStandard
import com.dbcheck.app.domain.noise.NoiseNotificationSchedule
import com.dbcheck.app.domain.passive.PassiveMonitoringDailySummary
import com.dbcheck.app.service.AudioInputDeviceDiscoveryPort
import com.dbcheck.app.service.AudioSessionManager
import com.dbcheck.app.service.BackupService
import com.dbcheck.app.service.HealthConnectService
import com.dbcheck.app.service.HealthConnectServiceAvailability
import com.dbcheck.app.service.HealthConnectServiceStatus
import com.dbcheck.app.service.HistoryClearService
import com.dbcheck.app.service.LocalBackupInfo
import com.dbcheck.app.service.LocalBackupResult
import com.dbcheck.app.service.LocalRestoreResult
import com.dbcheck.app.service.PassiveMonitoringManager
import com.dbcheck.app.service.PassiveMonitoringServiceController
import com.dbcheck.app.ui.settings.state.AudioInputDeviceUiState
import com.dbcheck.app.ui.settings.state.CalibrationProfileUiState
import com.dbcheck.app.ui.settings.state.HealthConnectAvailabilityUi
import com.dbcheck.app.ui.settings.state.HealthConnectUiState
import com.dbcheck.app.ui.settings.state.LocalBackupUiState
import com.dbcheck.app.ui.settings.state.OctaveCalibrationBandUiState
import com.dbcheck.app.ui.settings.state.PassiveMonitoringDailySummaryUiState
import com.dbcheck.app.ui.settings.state.SettingsUiState
import com.dbcheck.app.util.toUserFacingMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

sealed interface DisplayPreferenceUpdate {
    data class WaveformStyleChange(val style: WaveformStyle) : DisplayPreferenceUpdate

    data class RefreshRateChange(val rate: MeterRefreshRate) : DisplayPreferenceUpdate
}

sealed interface FeatureToggleUpdate {
    data class TechnicalMetadata(val enabled: Boolean) : FeatureToggleUpdate

    data class DosimeterCard(val enabled: Boolean) : FeatureToggleUpdate

    data class SoundDetection(val enabled: Boolean) : FeatureToggleUpdate

    data class SleepCard(val enabled: Boolean) : FeatureToggleUpdate
}

sealed interface NoiseNotificationUpdate {
    data class ExposureAlerts(val enabled: Boolean) : NoiseNotificationUpdate

    data class PeakWarnings(val enabled: Boolean) : NoiseNotificationUpdate

    data class NotificationThreshold(val threshold: Int) : NoiseNotificationUpdate

    data class NotificationSchedule(val schedule: NoiseNotificationSchedule) : NoiseNotificationUpdate

    data class AudibleAlarm(val enabled: Boolean) : NoiseNotificationUpdate

    data class TtsRiskPrompt(val enabled: Boolean) : NoiseNotificationUpdate
}

enum class HealthConnectIntentTarget {
    INSTALL,
    MANAGE_DATA,
}

@HiltViewModel
@Suppress("LargeClass", "TooManyFunctions")
class SettingsViewModel
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val preferencesRepository: PreferencesRepository,
        private val calibrationProfileRepository: CalibrationProfileRepository,
        private val healthConnectService: HealthConnectService,
        private val billingGateway: BillingGateway,
        private val exportCsvUseCase: ExportCsvUseCase,
        private val backupService: BackupService,
        private val audioSessionManager: AudioSessionManager,
        private val passiveMonitoringManager: PassiveMonitoringManager,
        private val passiveMonitoringRepository: PassiveMonitoringRepository,
        private val passiveMonitoringServiceController: PassiveMonitoringServiceController,
        private val historyClearService: HistoryClearService,
        private val audioInputDeviceDiscoveryPort: AudioInputDeviceDiscoveryPort,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SettingsUiState())
        val uiState: StateFlow<SettingsUiState> = _uiState
        private val _csvExportIntents = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
        val csvExportIntents: SharedFlow<Intent> = _csvExportIntents.asSharedFlow()
        private var calibrationProfiles: List<CalibrationProfile> = emptyList()
        private var calibrationProfilesLoaded = false
        private var defaultCalibrationProfileCreateRequested = false
        private var selectedAudioInputDevicePreferenceId: Int? = null
        private var currentIsProUser = false

        init {
            viewModelScope.launch {
                preferencesRepository.userPreferences.collect { prefs ->
                    val isProUser = prefs.isProUser
                    currentIsProUser = isProUser
                    selectedAudioInputDevicePreferenceId = prefs.selectedAudioInputDeviceId
                    _uiState.update {
                        it.copy(
                            themeMode = prefs.themeMode,
                            exposureAlertsEnabled = prefs.exposureAlertsEnabled,
                            peakWarningsEnabled = prefs.peakWarningsEnabled,
                            notificationThreshold = prefs.notificationThreshold,
                            notificationSchedule = prefs.notificationSchedule,
                            micSensitivityOffset = ProAudioPreferencePolicy.micOffset(prefs),
                            frequencyWeighting = ProAudioPreferencePolicy.weighting(prefs),
                            selectedCalibrationProfileId = prefs.selectedCalibrationProfileId,
                            selectedAudioInputDeviceId =
                                resolveSelectedAudioInputDeviceId(
                                    preferredDeviceId = prefs.selectedAudioInputDeviceId,
                                    isProUser = isProUser,
                                    devices = it.audioInputDevices,
                                ),
                            responseTime = ProAudioPreferencePolicy.responseTime(
                                isProUser = isProUser,
                                responseTime = prefs.responseTime,
                            ),
                            dosimeterStandard = ProAudioPreferencePolicy.dosimeterStandard(
                                isProUser = isProUser,
                                dosimeterStandard = prefs.dosimeterStandard,
                            ),
                            waveformStyle = prefs.waveformStyle,
                            refreshRate = prefs.refreshRate,
                            lockscreenMeterEnabled = prefs.lockscreenMeterEnabled && isProUser,
                            showLockscreenMeterPublicly =
                                prefs.showLockscreenMeterPublicly &&
                                    prefs.lockscreenMeterEnabled &&
                                    isProUser,
                            healthConnectEnabled = prefs.healthConnectEnabled,
                            heartRateOverlayEnabled = prefs.heartRateOverlayEnabled && isProUser,
                            technicalMetadataEnabled = prefs.technicalMetadataEnabled && isProUser,
                            dosimeterCardEnabled = prefs.dosimeterCardEnabled && isProUser,
                            soundDetectionEnabled = prefs.soundDetectionEnabled && isProUser,
                            sleepCardEnabled = prefs.sleepCardEnabled && isProUser,
                            wavRecordingDefaultEnabled = prefs.wavRecordingDefaultEnabled && isProUser,
                            audibleAlarmEnabled = prefs.audibleAlarmEnabled && isProUser,
                            ttsRiskPromptEnabled = prefs.ttsRiskPromptEnabled && isProUser,
                            voiceBaselineLevelDb = prefs.voiceBaselineLevelDb.takeIf { isProUser },
                            voiceBaselineSampleCount =
                                if (isProUser && prefs.voiceBaselineLevelDb != null) {
                                    prefs.voiceBaselineSampleCount
                                } else {
                                    0
                                },
                            voiceBaselineCapturedAtMs = prefs.voiceBaselineCapturedAtMs.takeIf { isProUser },
                            debugForceFreeEnabled = prefs.debugForceFreeEnabled,
                            isProUser = isProUser,
                        ).withCalibrationProfiles(calibrationProfiles)
                    }
                    ensureDefaultCalibrationProfileIfNeeded()
                }
            }
            viewModelScope.launch {
                audioSessionManager.isRecording.collect { isRecording ->
                    _uiState.update { it.copy(isRecording = isRecording) }
                }
            }
            viewModelScope.launch {
                passiveMonitoringManager.isMonitoring.collect { isMonitoring ->
                    _uiState.update { it.copy(passiveMonitoringActive = isMonitoring) }
                }
            }
            viewModelScope.launch {
                val range = todayRangeMillis()
                passiveMonitoringRepository
                    .observeDailySummary(
                        startTimeMs = range.startTimeMs,
                        endTimeMs = range.endTimeMs,
                    ).collect { summary ->
                        _uiState.update {
                            it.copy(passiveMonitoringDailySummary = summary.toUiState())
                        }
                    }
            }
            viewModelScope.launch {
                calibrationProfileRepository.observeProfiles().collect { profiles ->
                    calibrationProfilesLoaded = true
                    calibrationProfiles = profiles
                    _uiState.update { it.withCalibrationProfiles(profiles) }
                    ensureDefaultCalibrationProfileIfNeeded()
                }
            }
            viewModelScope.launch {
                billingGateway.purchaseEvents.collect { event ->
                    handlePurchaseEvent(event, _uiState, context)
                }
            }
            viewModelScope.launch {
                refreshLocalBackups(backupService, _uiState, context)
            }
            refreshHealthConnectStatus()
            refreshAudioInputDevices()
        }

        private fun ensureDefaultCalibrationProfileIfNeeded() {
            val state = _uiState.value
            if (!shouldCreateDefaultCalibrationProfile(state)) {
                return
            }

            defaultCalibrationProfileCreateRequested = true
            viewModelScope.launch {
                val profileId =
                    calibrationProfileRepository.createProfile(
                        name = context.getString(R.string.settings_calibration_profile_default_name),
                        micSensitivityOffset = state.micSensitivityOffset,
                        isDefault = true,
                        timestampMillis = System.currentTimeMillis(),
                    )
                preferencesRepository.updateSelectedCalibrationProfileId(profileId)
            }
        }

        private suspend fun selectFallbackCalibrationProfileIfNeeded(deletedProfileId: Long) {
            if (_uiState.value.selectedCalibrationProfileId != deletedProfileId) return

            val fallbackProfileId =
                calibrationProfiles
                    .filterNot { it.id == deletedProfileId }
                    .firstOrNull { it.isDefault }
                    ?.id
                    ?: calibrationProfiles.firstOrNull { it.id != deletedProfileId }?.id
            preferencesRepository.updateSelectedCalibrationProfileId(fallbackProfileId)
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

                    is NoiseNotificationUpdate.NotificationSchedule ->
                        preferencesRepository.updateNotificationSchedule(update.schedule)

                    is NoiseNotificationUpdate.AudibleAlarm -> {
                        if (update.enabled && !_uiState.value.isProUser) return@launch
                        preferencesRepository.updateAudibleAlarmEnabled(update.enabled)
                    }

                    is NoiseNotificationUpdate.TtsRiskPrompt -> {
                        if (update.enabled && !_uiState.value.isProUser) return@launch
                        preferencesRepository.updateTtsRiskPromptEnabled(update.enabled)
                    }
                }
            }
        }

        fun startPassiveMonitoring() {
            if (_uiState.value.passiveMonitoringActive) return

            viewModelScope.launch {
                val started = passiveMonitoringServiceController.startPassiveMonitoring()
                if (!started) {
                    _uiState.update {
                        it.copy(
                            passiveMonitoringErrorMessage =
                                context.getString(R.string.settings_passive_monitoring_start_failed),
                        )
                    }
                }
            }
        }

        fun stopPassiveMonitoring() {
            viewModelScope.launch {
                val stopped = passiveMonitoringServiceController.stopPassiveMonitoring()
                if (!stopped) {
                    _uiState.update {
                        it.copy(
                            passiveMonitoringErrorMessage =
                                context.getString(R.string.settings_passive_monitoring_stop_failed),
                        )
                    }
                }
            }
        }

        fun onPassiveMonitoringPermissionDenied() {
            _uiState.update {
                it.copy(
                    passiveMonitoringErrorMessage =
                        context.getString(R.string.settings_passive_monitoring_mic_required),
                )
            }
        }

        fun clearPassiveMonitoringMessages() {
            _uiState.update { it.copy(passiveMonitoringErrorMessage = null) }
        }

        private fun shouldCreateDefaultCalibrationProfile(state: SettingsUiState): Boolean {
            val canCreateForCurrentUser = state.isProUser && calibrationProfilesLoaded
            val needsFirstProfile = calibrationProfiles.isEmpty()
            return canCreateForCurrentUser && needsFirstProfile && !defaultCalibrationProfileCreateRequested
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

        fun selectAudioInputDevice(deviceId: Int) {
            val state = _uiState.value
            if (!state.isProUser) return
            if (state.audioInputDevices.none { it.id == deviceId }) return

            viewModelScope.launch { preferencesRepository.updateSelectedAudioInputDeviceId(deviceId) }
        }

        fun createCalibrationProfile(name: String) {
            if (!_uiState.value.isProUser) return

            val normalizedName = name.trim()
            if (normalizedName.isBlank()) return

            viewModelScope.launch {
                val profileId =
                    calibrationProfileRepository.createProfile(
                        name = normalizedName,
                        micSensitivityOffset = _uiState.value.micSensitivityOffset,
                        isDefault = false,
                        timestampMillis = System.currentTimeMillis(),
                    )
                preferencesRepository.updateSelectedCalibrationProfileId(profileId)
            }
        }

        fun selectCalibrationProfile(profileId: Long) {
            if (!_uiState.value.isProUser) return

            viewModelScope.launch { preferencesRepository.updateSelectedCalibrationProfileId(profileId) }
        }

        fun renameCalibrationProfile(profileId: Long, name: String) {
            if (!_uiState.value.isProUser) return

            val normalizedName = name.trim()
            if (normalizedName.isBlank()) return

            viewModelScope.launch {
                calibrationProfileRepository.renameProfile(
                    profileId = profileId,
                    name = normalizedName,
                    timestampMillis = System.currentTimeMillis(),
                )
            }
        }

        fun updateOctaveBandOffset(profileId: Long, centerFrequencyHz: Float, offsetDb: Float) {
            if (!ProAudioPreferencePolicy.canUseProAudioPreferences(_uiState.value.isProUser)) return

            val profile = calibrationProfiles.firstOrNull { it.id == profileId } ?: return
            val offsets = profile.octaveCalibrationOffsets.withOffset(centerFrequencyHz, offsetDb)
            viewModelScope.launch {
                calibrationProfileRepository.updateOctaveBandOffsets(
                    profileId = profileId,
                    offsets = offsets,
                    timestampMillis = System.currentTimeMillis(),
                )
            }
        }

        fun resetOctaveBandOffsets(profileId: Long) {
            if (!ProAudioPreferencePolicy.canUseProAudioPreferences(_uiState.value.isProUser)) return
            if (calibrationProfiles.none { it.id == profileId }) return

            viewModelScope.launch {
                calibrationProfileRepository.resetOctaveBandOffsets(
                    profileId = profileId,
                    timestampMillis = System.currentTimeMillis(),
                )
            }
        }

        fun deleteCalibrationProfile(profileId: Long) {
            if (!_uiState.value.isProUser) return

            viewModelScope.launch {
                when (calibrationProfileRepository.deleteProfile(profileId)) {
                    CalibrationProfileDeleteResult.Deleted -> selectFallbackCalibrationProfileIfNeeded(profileId)

                    CalibrationProfileDeleteResult.BlockedLastDefault ->
                        _uiState.update {
                            it.copy(
                                calibrationProfileErrorMessage =
                                    context.getString(R.string.settings_calibration_profile_last_default_error),
                            )
                        }

                    CalibrationProfileDeleteResult.NotFound -> Unit
                }
            }
        }

        fun clearCalibrationProfileMessages() {
            _uiState.update { it.copy(calibrationProfileErrorMessage = null) }
        }

        fun updateResponseTime(responseTime: ResponseTime) {
            if (!ProAudioPreferencePolicy.canUseProAudioPreferences(_uiState.value.isProUser)) return

            viewModelScope.launch { preferencesRepository.updateResponseTime(responseTime) }
        }

        fun updateDosimeterStandard(standard: DosimeterStandard) {
            if (!ProAudioPreferencePolicy.canUseProAudioPreferences(_uiState.value.isProUser)) return

            viewModelScope.launch { preferencesRepository.updateDosimeterStandard(standard) }
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
            if (enabled && !_uiState.value.isProUser) return

            viewModelScope.launch {
                preferencesRepository.updateLockscreenMeterEnabled(enabled)
                if (!enabled) {
                    preferencesRepository.updateShowLockscreenMeterPublicly(false)
                }
            }
        }

        fun updateShowLockscreenMeterPublicly(enabled: Boolean) {
            val state = _uiState.value
            if (enabled && (!state.isProUser || !state.lockscreenMeterEnabled)) return

            viewModelScope.launch { preferencesRepository.updateShowLockscreenMeterPublicly(enabled) }
        }

        fun updateHealthConnectEnabled(enabled: Boolean) {
            viewModelScope.launch { preferencesRepository.updateHealthConnectEnabled(enabled) }
        }

        fun updateHeartRateOverlayEnabled(enabled: Boolean) {
            if (enabled && !_uiState.value.isProUser) return

            viewModelScope.launch { preferencesRepository.updateHeartRateOverlayEnabled(enabled) }
        }

        fun updateWavRecordingDefaultEnabled(enabled: Boolean) {
            if (enabled && !_uiState.value.isProUser) return

            viewModelScope.launch { preferencesRepository.updateWavRecordingDefaultEnabled(enabled) }
        }

        fun previewAudibleAlarm() {
            if (!_uiState.value.isProUser) return

            audioSessionManager.previewAudibleAlarm(isProUser = true)
        }

        fun calibrateVoiceBaseline() {
            val state = _uiState.value
            if (!state.isProUser || !audioSessionManager.isRecording.value || !state.soundDetectionEnabled) return
            val capture = audioSessionManager.captureVoiceBaseline(isProUser = true) ?: return

            viewModelScope.launch {
                preferencesRepository.updateVoiceBaseline(
                    levelDb = capture.levelDb,
                    sampleCount = capture.sampleCount,
                    capturedAtMs = capture.capturedAtMs,
                )
            }
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
                val result =
                    runCatching {
                        billingGateway.launchPurchaseFlow(activity)
                    }.getOrElse { error ->
                        if (error is CancellationException) throw error
                        showPurchaseLaunchFailure(context.getString(R.string.billing_start_purchase_failed), _uiState)
                        return@launch
                    }
                when (result) {
                    PurchaseLaunchResult.Started ->
                        _uiState.update { it.copy(isPurchaseLaunching = false) }

                    PurchaseLaunchResult.AlreadyOwned ->
                        _uiState.update {
                            it.copy(
                                isPurchaseLaunching = false,
                                purchaseMessage = context.getString(R.string.billing_pro_already_unlocked),
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
                    purchaseErrorMessage = context.getString(R.string.billing_unable_to_open_purchase_flow),
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
                            csvExportErrorMessage = context.getString(R.string.settings_csv_export_requires_pro),
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
                                        csvExportErrorMessage =
                                            error.toUserFacingMessage(
                                                context.getString(R.string.settings_csv_export_failed),
                                            ),
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
                    csvExportMessage = context.getString(R.string.settings_csv_export_ready),
                    csvExportErrorMessage = null,
                )
            }
        }

        fun onCsvShareUnavailable() {
            _uiState.update {
                it.copy(
                    isCsvExporting = false,
                    csvExportMessage = null,
                    csvExportErrorMessage = context.getString(R.string.settings_csv_no_export_app),
                )
            }
        }

        fun clearCsvExportMessages() {
            _uiState.update { it.copy(csvExportMessage = null, csvExportErrorMessage = null) }
        }

        fun createLocalBackup() {
            if (!ensureBackupAllowed(audioSessionManager, _uiState, context) || _uiState.value.isBackupCreating) return

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
                        refreshLocalBackups(backupService, _uiState, context)
                        _uiState.update {
                            it.copy(
                                isBackupCreating = false,
                                backupMessage = context.getString(R.string.settings_backup_created),
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
            if (!ensureBackupAllowed(audioSessionManager, _uiState, context)) return

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

        fun confirmRestoreBackup(onRestartAfterRestore: () -> Unit = {}) {
            val backup = _uiState.value.restoreCandidate?.toBackupInfo() ?: return
            if (
                !ensureBackupAllowed(audioSessionManager, _uiState, context) ||
                _uiState.value.isBackupRestoring
            ) {
                return
            }

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
                        refreshLocalBackups(backupService, _uiState, context)
                        _uiState.update {
                            it.copy(
                                isBackupRestoring = false,
                                restoreCandidate = null,
                                backupMessage = context.getString(R.string.settings_backup_restored),
                                backupErrorMessage = null,
                            )
                        }
                        onRestartAfterRestore()
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
                            onRestartAfterRestore()
                        }
                    }
                }
            }
        }

        fun clearBackupMessages() {
            _uiState.update { it.copy(backupMessage = null, backupErrorMessage = null) }
        }

        fun requestClearHistory() {
            if (!ensureHistoryClearAllowed(audioSessionManager, _uiState, context)) return

            _uiState.update {
                it.copy(
                    clearHistoryConfirmationVisible = true,
                    historyClearMessage = null,
                    historyClearErrorMessage = null,
                )
            }
        }

        fun updateFeatureToggle(update: FeatureToggleUpdate) {
            val enabled =
                when (update) {
                    is FeatureToggleUpdate.TechnicalMetadata -> update.enabled
                    is FeatureToggleUpdate.DosimeterCard -> update.enabled
                    is FeatureToggleUpdate.SoundDetection -> update.enabled
                    is FeatureToggleUpdate.SleepCard -> update.enabled
                }
            if (enabled && !_uiState.value.isProUser) return

            viewModelScope.launch {
                when (update) {
                    is FeatureToggleUpdate.TechnicalMetadata ->
                        preferencesRepository.updateTechnicalMetadataEnabled(update.enabled)

                    is FeatureToggleUpdate.DosimeterCard ->
                        preferencesRepository.updateDosimeterCardEnabled(update.enabled)

                    is FeatureToggleUpdate.SoundDetection ->
                        preferencesRepository.updateSoundDetectionEnabled(update.enabled)

                    is FeatureToggleUpdate.SleepCard ->
                        preferencesRepository.updateSleepCardEnabled(update.enabled)
                }
            }
        }

        fun dismissClearHistory() {
            if (_uiState.value.isHistoryClearing) return
            _uiState.update { it.copy(clearHistoryConfirmationVisible = false) }
        }

        fun confirmClearHistory() {
            if (
                !ensureHistoryClearAllowed(audioSessionManager, _uiState, context) ||
                _uiState.value.isHistoryClearing
            ) {
                return
            }

            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        isHistoryClearing = true,
                        historyClearMessage = null,
                        historyClearErrorMessage = null,
                    )
                }
                runCatching { historyClearService.clearHistory() }
                    .onSuccess {
                        _uiState.update { state ->
                            state.copy(
                                clearHistoryConfirmationVisible = false,
                                isHistoryClearing = false,
                                historyClearMessage = context.getString(R.string.settings_clear_history_done),
                                historyClearErrorMessage = null,
                            )
                        }
                    }.onFailure { error ->
                        if (error is CancellationException) throw error
                        _uiState.update {
                            it.copy(
                                clearHistoryConfirmationVisible = false,
                                isHistoryClearing = false,
                                historyClearMessage = null,
                                historyClearErrorMessage =
                                    error.toUserFacingMessage(
                                        context.getString(R.string.settings_clear_history_failed),
                                    ),
                            )
                        }
                    }
            }
        }

        fun clearHistoryMessages() {
            _uiState.update { it.copy(historyClearMessage = null, historyClearErrorMessage = null) }
        }

        fun onHealthConnectInstallUnavailable() {
            _uiState.update {
                it.copy(healthConnectErrorMessage = context.getString(R.string.health_connect_unable_to_open))
            }
        }

        fun clearHealthConnectMessages() {
            _uiState.update { it.copy(healthConnectErrorMessage = null) }
        }

        fun refreshHealthConnectStatus() {
            viewModelScope.launch {
                runCatching { healthConnectService.getStatus() }
                    .onSuccess { status ->
                        _uiState.update {
                            it.copy(
                                healthConnectStatus = status.toUiState(),
                                healthConnectErrorMessage = status.errorMessage,
                            )
                        }
                    }.onFailure { error ->
                        if (error is CancellationException) throw error
                        _uiState.update {
                            it.copy(
                                healthConnectErrorMessage =
                                    error.toUserFacingMessage(
                                        context.getString(R.string.health_connect_status_check_failed),
                                    ),
                            )
                        }
                    }
            }
        }

        fun refreshAudioInputDevices() {
            viewModelScope.launch {
                runCatching { audioInputDeviceDiscoveryPort.listInputDevices() }
                    .onSuccess { devices ->
                        _uiState.update {
                            val deviceStates = devices.toUiState()
                            it.copy(
                                audioInputDevices = deviceStates,
                                selectedAudioInputDeviceId =
                                    resolveSelectedAudioInputDeviceId(
                                        preferredDeviceId = selectedAudioInputDevicePreferenceId,
                                        isProUser = currentIsProUser,
                                        devices = deviceStates,
                                    ),
                            )
                        }
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

private fun handlePurchaseEvent(event: PurchaseEvent, uiState: MutableStateFlow<SettingsUiState>, context: Context) {
    uiState.update { current ->
        when (event) {
            PurchaseEvent.Completed ->
                current.copy(
                    isPurchaseLaunching = false,
                    purchaseMessage = context.getString(R.string.billing_pro_unlocked),
                    purchaseErrorMessage = null,
                )

            PurchaseEvent.Pending ->
                current.copy(
                    isPurchaseLaunching = false,
                    purchaseMessage = context.getString(R.string.billing_purchase_pending),
                    purchaseErrorMessage = null,
                )

            PurchaseEvent.AlreadyOwned ->
                current.copy(
                    isPurchaseLaunching = false,
                    purchaseMessage = context.getString(R.string.billing_pro_already_unlocked),
                    purchaseErrorMessage = null,
                )

            PurchaseEvent.Cancelled ->
                current.copy(
                    isPurchaseLaunching = false,
                    purchaseMessage = null,
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

private fun SettingsUiState.withCalibrationProfiles(profiles: List<CalibrationProfile>): SettingsUiState {
    val selectedProfileId =
        selectedCalibrationProfileId
            ?: profiles.firstOrNull { it.isDefault }?.id
            ?: profiles.firstOrNull()?.id
    val defaultProfileCount = profiles.count { it.isDefault }
    return copy(
        calibrationProfiles =
            profiles.map { profile ->
                CalibrationProfileUiState(
                    id = profile.id,
                    name = profile.name,
                    micSensitivityOffset = profile.micSensitivityOffset,
                    octaveBandOffsets = profile.octaveCalibrationOffsets.toUiState(),
                    isDefault = profile.isDefault,
                    isSelected = profile.id == selectedProfileId,
                    canDelete = !profile.isDefault || defaultProfileCount > 1,
                )
            },
    )
}

private fun OctaveCalibrationOffsets.toUiState(): List<OctaveCalibrationBandUiState> =
    OctaveCalibrationOffsets.supportedCenterFrequenciesHz.map { centerFrequencyHz ->
        OctaveCalibrationBandUiState(
            centerFrequencyHz = centerFrequencyHz,
            offsetDb = offsetFor(centerFrequencyHz),
        )
    }

private fun List<AudioInputDevice>.toUiState(): List<AudioInputDeviceUiState> = map { device ->
        AudioInputDeviceUiState(
            id = device.id,
            displayName = device.displayName,
            type = device.type,
            isExternal = device.isExternal,
            sampleRatesHz = device.sampleRatesHz,
            channelCounts = device.channelCounts,
        )
    }

private data class TimeRangeMillis(val startTimeMs: Long, val endTimeMs: Long)

private fun todayRangeMillis(zoneId: ZoneId = ZoneId.systemDefault()): TimeRangeMillis {
    val todayStart =
        Instant
            .ofEpochMilli(System.currentTimeMillis())
            .atZone(zoneId)
            .toLocalDate()
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    return TimeRangeMillis(
        startTimeMs = todayStart,
        endTimeMs = todayStart + DAY_MS,
    )
}

private fun PassiveMonitoringDailySummary.toUiState(): PassiveMonitoringDailySummaryUiState =
    PassiveMonitoringDailySummaryUiState(
        hasSamples = hasSamples,
        sampleCount = sampleCount,
        readingCount = readingCount,
        averageDb = averageDb,
        peakDb = peakDb,
    )

private fun resolveSelectedAudioInputDeviceId(
    preferredDeviceId: Int?,
    isProUser: Boolean,
    devices: List<AudioInputDeviceUiState>,
): Int? = when {
        !isProUser -> null
        preferredDeviceId == null -> null
        devices.any { it.id == preferredDeviceId } -> preferredDeviceId
        else -> devices.firstOrNull { !it.isExternal }?.id
    }

private suspend fun refreshLocalBackups(
    backupService: BackupService,
    uiState: MutableStateFlow<SettingsUiState>,
    context: Context,
) {
    runCatching { backupService.listBackups() }
        .onSuccess { backups ->
            val backupStates = backups.map { backup -> backup.toUiState() }
            uiState.update { it.copy(localBackups = backupStates) }
        }.onFailure { error ->
            uiState.update {
                it.copy(
                    backupErrorMessage =
                        error.toUserFacingMessage(
                            context.getString(R.string.settings_backup_unable_to_load),
                        ),
                )
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
        displayName = displayName,
        createdAtMillis = createdAtMillis,
        sizeBytes = sizeBytes,
    )

private fun LocalBackupUiState.toBackupInfo(): LocalBackupInfo = LocalBackupInfo(
        filePath = filePath,
        fileName = fileName,
        displayName = displayName,
        createdAtMillis = createdAtMillis,
        sizeBytes = sizeBytes,
    )

private fun ensureBackupAllowed(
    audioSessionManager: AudioSessionManager,
    uiState: MutableStateFlow<SettingsUiState>,
    context: Context,
): Boolean {
    if (!audioSessionManager.isRecording.value) return true

    uiState.update {
        it.copy(
            isBackupCreating = false,
            isBackupRestoring = false,
            restoreCandidate = null,
            backupMessage = null,
            backupErrorMessage = context.getString(R.string.settings_backup_stop_recording),
        )
    }
    return false
}

private fun ensureHistoryClearAllowed(
    audioSessionManager: AudioSessionManager,
    uiState: MutableStateFlow<SettingsUiState>,
    context: Context,
): Boolean {
    if (!audioSessionManager.isRecording.value) return true

    uiState.update {
        it.copy(
            clearHistoryConfirmationVisible = false,
            isHistoryClearing = false,
            historyClearMessage = null,
            historyClearErrorMessage = context.getString(R.string.settings_clear_history_stop_recording),
        )
    }
    return false
}

private const val DAY_MS = 24L * 60L * 60L * 1_000L
