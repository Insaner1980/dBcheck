package com.dbcheck.app.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dbcheck.app.BuildConfig
import com.dbcheck.app.R
import com.dbcheck.app.domain.noise.NoiseNotificationSchedule
import com.dbcheck.app.ui.common.findActivity
import com.dbcheck.app.ui.common.hasRecordAudioPermission
import com.dbcheck.app.ui.common.requestPostNotificationsPermissionIfNeeded
import com.dbcheck.app.ui.components.DbCheckTopAppBar
import com.dbcheck.app.ui.settings.components.AudioCalibrationSection
import com.dbcheck.app.ui.settings.components.AudioCalibrationSectionActions
import com.dbcheck.app.ui.settings.components.AudioCalibrationSectionState
import com.dbcheck.app.ui.settings.components.DataExportSection
import com.dbcheck.app.ui.settings.components.DataExportSectionActions
import com.dbcheck.app.ui.settings.components.DataExportSectionState
import com.dbcheck.app.ui.settings.components.DisplayAndFeaturesSection
import com.dbcheck.app.ui.settings.components.DisplayAndFeaturesSectionActions
import com.dbcheck.app.ui.settings.components.DisplayAndFeaturesSectionState
import com.dbcheck.app.ui.settings.components.HealthSyncSection
import com.dbcheck.app.ui.settings.components.HealthSyncSectionActions
import com.dbcheck.app.ui.settings.components.HealthSyncSectionState
import com.dbcheck.app.ui.settings.components.NoiseNotificationsSection
import com.dbcheck.app.ui.settings.components.ProUpsellCard
import com.dbcheck.app.ui.settings.components.ProUpsellCardActions
import com.dbcheck.app.ui.settings.components.ProUpsellCardState
import com.dbcheck.app.ui.settings.state.SettingsUiState
import com.dbcheck.app.ui.theme.DbCheckTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

@Composable
@Suppress("LongMethod")
fun SettingsScreen(
    modifier: Modifier = Modifier,
    scrollToProCard: Boolean = false,
    onRestartAfterRestore: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context.findActivity()
    val scrollState = rememberScrollState()
    val passiveNotificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) {
            Unit
        }
    val passiveMicPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (granted) {
                requestPostNotificationsPermissionIfNeeded(context, passiveNotificationPermissionLauncher)
                viewModel.startPassiveMonitoring()
            } else {
                viewModel.onPassiveMonitoringPermissionDenied()
            }
        }
    val onStartProPurchase = {
        if (activity != null) {
            viewModel.launchProPurchase(activity)
        } else {
            viewModel.onPurchaseActivityUnavailable()
        }
    }
    val onExportCsv: () -> Unit = {
        viewModel.createCsvExportIntent()
    }
    val csvShareChooserTitle = stringResource(R.string.settings_export_csv_chooser)
    val contentActions =
        settingsContentActions(
            viewModel = viewModel,
            context = context,
            onExportCsv = onExportCsv,
            onStartPassiveMonitoring = {
                handleStartPassiveMonitoring(
                    context = context,
                    micPermissionLauncher = passiveMicPermissionLauncher,
                    notificationPermissionLauncher = passiveNotificationPermissionLauncher,
                    viewModel = viewModel,
                )
            },
            onStartProPurchase = onStartProPurchase,
        )

    LaunchedEffect(csvShareChooserTitle) {
        viewModel.csvExportIntents.collect { intent ->
            runCatching {
                context.startActivity(Intent.createChooser(intent, csvShareChooserTitle))
            }.onSuccess {
                viewModel.onCsvShareStarted()
            }.onFailure {
                viewModel.onCsvShareUnavailable()
            }
        }
    }

    SettingsEffects(
        scrollToProCard = scrollToProCard,
        scrollState = scrollState,
        uiState = uiState,
        events = viewModel.events,
        actions =
            SettingsEffectActions(
                onClearPurchaseMessages = viewModel::clearPurchaseMessages,
                onClearCsvExportMessages = viewModel::clearCsvExportMessages,
                onClearBackupMessages = viewModel::clearBackupMessages,
                onClearHealthConnectMessages = viewModel::clearHealthConnectMessages,
                onClearCalibrationProfileMessages = viewModel::clearCalibrationProfileMessages,
                onClearPassiveMonitoringMessages = viewModel::clearPassiveMonitoringMessages,
                onRestartAfterRestore = onRestartAfterRestore,
            ),
    )

    Column(modifier = modifier.fillMaxSize()) {
        DbCheckTopAppBar()

        SettingsContent(
            uiState = uiState,
            scrollState = scrollState,
            actions = contentActions,
        )
    }
}

private data class SettingsContentActions(
    val audioCalibration: AudioCalibrationSectionActions,
    val onExposureAlertsChange: (Boolean) -> Unit,
    val onPeakWarningsChange: (Boolean) -> Unit,
    val onThresholdChange: (Int) -> Unit,
    val onScheduleChange: (NoiseNotificationSchedule) -> Unit,
    val onAudibleAlarmChange: (Boolean) -> Unit,
    val onTtsRiskPromptChange: (Boolean) -> Unit,
    val onAudibleAlarmPreview: () -> Unit,
    val onStartPassiveMonitoring: () -> Unit,
    val onStopPassiveMonitoring: () -> Unit,
    val onUpgradeClick: () -> Unit,
    val healthSync: HealthSyncSectionActions,
    val dataExport: DataExportSectionActions,
    val displayAndFeatures: DisplayAndFeaturesSectionActions,
    val proUpsell: ProUpsellCardActions,
)

private fun settingsContentActions(
    viewModel: SettingsViewModel,
    context: Context,
    onExportCsv: () -> Unit,
    onStartPassiveMonitoring: () -> Unit,
    onStartProPurchase: () -> Unit,
): SettingsContentActions = SettingsContentActions(
        audioCalibration = audioCalibrationActions(viewModel, onStartProPurchase),
        onExposureAlertsChange = {
            viewModel.updateNoiseNotification(NoiseNotificationUpdate.ExposureAlerts(it))
        },
        onPeakWarningsChange = {
            viewModel.updateNoiseNotification(NoiseNotificationUpdate.PeakWarnings(it))
        },
        onThresholdChange = {
            viewModel.updateNoiseNotification(NoiseNotificationUpdate.NotificationThreshold(it))
        },
        onScheduleChange = {
            viewModel.updateNoiseNotification(NoiseNotificationUpdate.NotificationSchedule(it))
        },
        onAudibleAlarmChange = {
            viewModel.updateNoiseNotification(NoiseNotificationUpdate.AudibleAlarm(it))
        },
        onTtsRiskPromptChange = {
            viewModel.updateNoiseNotification(NoiseNotificationUpdate.TtsRiskPrompt(it))
        },
        onAudibleAlarmPreview = viewModel::previewAudibleAlarm,
        onStartPassiveMonitoring = onStartPassiveMonitoring,
        onStopPassiveMonitoring = viewModel::stopPassiveMonitoring,
        onUpgradeClick = onStartProPurchase,
        healthSync = healthSyncActions(viewModel, context, onStartProPurchase),
        dataExport = dataExportActions(viewModel, onExportCsv, onStartProPurchase),
        displayAndFeatures = displayAndFeaturesActions(viewModel, onStartProPurchase),
        proUpsell = proUpsellActions(viewModel, onStartProPurchase),
    )

private fun audioCalibrationActions(
    viewModel: SettingsViewModel,
    onStartProPurchase: () -> Unit,
): AudioCalibrationSectionActions = AudioCalibrationSectionActions(
        onSensitivityChange = viewModel::updateMicSensitivity,
        onWeightingChange = viewModel::updateFrequencyWeighting,
        onSelectAudioInputDevice = viewModel::selectAudioInputDevice,
        onCreateProfile = viewModel::createCalibrationProfile,
        onSelectProfile = viewModel::selectCalibrationProfile,
        onRenameProfile = viewModel::renameCalibrationProfile,
        onDeleteProfile = viewModel::deleteCalibrationProfile,
        onOctaveBandOffsetChange = viewModel::updateOctaveBandOffset,
        onResetOctaveBandOffsets = viewModel::resetOctaveBandOffsets,
        onUpgradeClick = onStartProPurchase,
    )

private fun healthSyncActions(
    viewModel: SettingsViewModel,
    context: Context,
    onStartProPurchase: () -> Unit,
): HealthSyncSectionActions = HealthSyncSectionActions(
        onHealthConnectChange = viewModel::updateHealthConnectEnabled,
        onHeartRateOverlayChange = viewModel::updateHeartRateOverlayEnabled,
        onPermissionsChanged = viewModel::refreshHealthConnectStatus,
        onOpenHealthConnectInstall = {
            runCatching {
                context.startActivity(
                    viewModel.createHealthConnectIntent(HealthConnectIntentTarget.INSTALL),
                )
            }.onFailure {
                viewModel.onHealthConnectInstallUnavailable()
            }
        },
        onOpenHealthConnectManageData = {
            runCatching {
                context.startActivity(
                    viewModel.createHealthConnectIntent(HealthConnectIntentTarget.MANAGE_DATA),
                )
            }.onFailure {
                viewModel.onHealthConnectInstallUnavailable()
            }
        },
        onUpgradeClick = onStartProPurchase,
    )

private fun dataExportActions(
    viewModel: SettingsViewModel,
    onExportCsv: () -> Unit,
    onStartProPurchase: () -> Unit,
): DataExportSectionActions = DataExportSectionActions(
        onExportCsv = onExportCsv,
        onCreateBackup = viewModel::createLocalBackup,
        onRequestRestoreBackup = viewModel::requestRestoreBackup,
        onConfirmRestoreBackup = viewModel::confirmRestoreBackup,
        onDismissRestoreBackup = viewModel::dismissRestoreBackup,
        onWavRecordingDefaultChange = viewModel::updateWavRecordingDefaultEnabled,
        onRequestClearHistory = viewModel::requestClearHistory,
        onConfirmClearHistory = viewModel::confirmClearHistory,
        onDismissClearHistory = viewModel::dismissClearHistory,
        onUpgradeClick = onStartProPurchase,
    )

private fun displayAndFeaturesActions(
    viewModel: SettingsViewModel,
    onStartProPurchase: () -> Unit,
): DisplayAndFeaturesSectionActions = DisplayAndFeaturesSectionActions(
        onThemeModeChange = viewModel::updateThemeMode,
        onWaveformStyleChange = {
            viewModel.updateDisplayPreference(DisplayPreferenceUpdate.WaveformStyleChange(it))
        },
        onRefreshRateChange = {
            viewModel.updateDisplayPreference(DisplayPreferenceUpdate.RefreshRateChange(it))
        },
        onLockscreenMeterChange = viewModel::updateLockscreenMeter,
        onShowLockscreenMeterPubliclyChange = viewModel::updateShowLockscreenMeterPublicly,
        onTechnicalMetadataChange = {
            viewModel.updateFeatureToggle(FeatureToggleUpdate.TechnicalMetadata(it))
        },
        onDosimeterCardChange = {
            viewModel.updateFeatureToggle(FeatureToggleUpdate.DosimeterCard(it))
        },
        onSoundDetectionChange = {
            viewModel.updateFeatureToggle(FeatureToggleUpdate.SoundDetection(it))
        },
        onSleepCardChange = {
            viewModel.updateFeatureToggle(FeatureToggleUpdate.SleepCard(it))
        },
        onCalibrateVoiceBaseline = viewModel::calibrateVoiceBaseline,
        onUpgradeClick = onStartProPurchase,
    )

private fun proUpsellActions(viewModel: SettingsViewModel, onStartProPurchase: () -> Unit): ProUpsellCardActions =
    ProUpsellCardActions(
        onUpgradeClick = onStartProPurchase,
        onDebugForceFreeChange = viewModel::updateDebugForceFree,
    )

@Composable
private fun SettingsEffects(
    scrollToProCard: Boolean,
    scrollState: androidx.compose.foundation.ScrollState,
    uiState: SettingsUiState,
    events: Flow<SettingsEvent>,
    actions: SettingsEffectActions,
) {
    val currentOnRestartAfterRestore by rememberUpdatedState(actions.onRestartAfterRestore)

    LaunchedEffect(scrollToProCard) {
        if (scrollToProCard) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }
    SettingsMessageEffects(
        uiState = uiState,
        onClearPurchaseMessages = actions.onClearPurchaseMessages,
        onClearCsvExportMessages = actions.onClearCsvExportMessages,
        onClearBackupMessages = actions.onClearBackupMessages,
        onClearHealthConnectMessages = actions.onClearHealthConnectMessages,
        onClearCalibrationProfileMessages = actions.onClearCalibrationProfileMessages,
        onClearPassiveMonitoringMessages = actions.onClearPassiveMonitoringMessages,
    )
    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                SettingsEvent.RestartAfterRestore -> currentOnRestartAfterRestore()
            }
        }
    }
}

private data class SettingsEffectActions(
    val onClearPurchaseMessages: () -> Unit,
    val onClearCsvExportMessages: () -> Unit,
    val onClearBackupMessages: () -> Unit,
    val onClearHealthConnectMessages: () -> Unit,
    val onClearCalibrationProfileMessages: () -> Unit,
    val onClearPassiveMonitoringMessages: () -> Unit,
    val onRestartAfterRestore: () -> Unit,
)

@Composable
private fun SettingsMessageEffects(
    uiState: SettingsUiState,
    onClearPurchaseMessages: () -> Unit,
    onClearCsvExportMessages: () -> Unit,
    onClearBackupMessages: () -> Unit,
    onClearHealthConnectMessages: () -> Unit,
    onClearCalibrationProfileMessages: () -> Unit,
    onClearPassiveMonitoringMessages: () -> Unit,
) {
    val currentOnClearPurchaseMessages by rememberUpdatedState(onClearPurchaseMessages)
    val currentOnClearCsvExportMessages by rememberUpdatedState(onClearCsvExportMessages)
    val currentOnClearBackupMessages by rememberUpdatedState(onClearBackupMessages)
    val currentOnClearHealthConnectMessages by rememberUpdatedState(onClearHealthConnectMessages)
    val currentOnClearCalibrationProfileMessages by rememberUpdatedState(onClearCalibrationProfileMessages)
    val currentOnClearPassiveMonitoringMessages by rememberUpdatedState(onClearPassiveMonitoringMessages)

    LaunchedEffect(uiState.purchaseMessage, uiState.purchaseErrorMessage) {
        if (uiState.purchaseMessage != null || uiState.purchaseErrorMessage != null) {
            delay(3_000L)
            currentOnClearPurchaseMessages()
        }
    }
    LaunchedEffect(uiState.csvExportMessage, uiState.csvExportErrorMessage) {
        if (uiState.csvExportMessage != null || uiState.csvExportErrorMessage != null) {
            delay(3_000L)
            currentOnClearCsvExportMessages()
        }
    }
    LaunchedEffect(uiState.backupMessage, uiState.backupErrorMessage) {
        if (uiState.backupMessage != null || uiState.backupErrorMessage != null) {
            delay(3_000L)
            currentOnClearBackupMessages()
        }
    }
    LaunchedEffect(uiState.healthConnectErrorMessage) {
        if (uiState.healthConnectErrorMessage != null) {
            delay(3_000L)
            currentOnClearHealthConnectMessages()
        }
    }
    LaunchedEffect(uiState.calibrationProfileErrorMessage) {
        if (uiState.calibrationProfileErrorMessage != null) {
            delay(3_000L)
            currentOnClearCalibrationProfileMessages()
        }
    }
    LaunchedEffect(uiState.passiveMonitoringErrorMessage) {
        if (uiState.passiveMonitoringErrorMessage != null) {
            delay(3_000L)
            currentOnClearPassiveMonitoringMessages()
        }
    }
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    scrollState: androidx.compose.foundation.ScrollState,
    actions: SettingsContentActions,
) {
    val spacing = DbCheckTheme.spacing

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.space4),
    ) {
        SettingsHeader()
        SettingsAudioAndNotificationSections(uiState, actions)
        SettingsHealthSyncSection(uiState, actions.healthSync)
        SettingsDataExportSection(uiState, actions.dataExport)
        SettingsDisplayAndFeaturesSection(uiState, actions.displayAndFeatures)
        SettingsProUpsellCard(
            uiState = uiState,
            actions = actions.proUpsell,
        )
        SettingsFooter(modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun SettingsAudioAndNotificationSections(uiState: SettingsUiState, actions: SettingsContentActions) {
    AudioCalibrationSection(
        state =
            AudioCalibrationSectionState(
                sensitivityOffset = uiState.micSensitivityOffset,
                frequencyWeighting = uiState.frequencyWeighting,
                isProUser = uiState.isProUser,
                profiles = uiState.calibrationProfiles,
                selectedProfileId = uiState.selectedCalibrationProfileId,
                profileErrorMessage = uiState.calibrationProfileErrorMessage,
                audioInputDevices = uiState.audioInputDevices,
                selectedAudioInputDeviceId = uiState.selectedAudioInputDeviceId,
            ),
        actions = actions.audioCalibration,
    )
    NoiseNotificationsSection(
        exposureAlertsEnabled = uiState.exposureAlertsEnabled,
        peakWarningsEnabled = uiState.peakWarningsEnabled,
        notificationThreshold = uiState.notificationThreshold,
        notificationSchedule = uiState.notificationSchedule,
        audibleAlarmEnabled = uiState.audibleAlarmEnabled,
        ttsRiskPromptEnabled = uiState.ttsRiskPromptEnabled,
        passiveMonitoringActive = uiState.passiveMonitoringActive,
        passiveMonitoringDailySummary = uiState.passiveMonitoringDailySummary,
        passiveMonitoringErrorMessage = uiState.passiveMonitoringErrorMessage,
        isProUser = uiState.isProUser,
        onExposureAlertsChange = actions.onExposureAlertsChange,
        onPeakWarningsChange = actions.onPeakWarningsChange,
        onThresholdChange = actions.onThresholdChange,
        onScheduleChange = actions.onScheduleChange,
        onAudibleAlarmChange = actions.onAudibleAlarmChange,
        onTtsRiskPromptChange = actions.onTtsRiskPromptChange,
        onAudibleAlarmPreview = actions.onAudibleAlarmPreview,
        onStartPassiveMonitoring = actions.onStartPassiveMonitoring,
        onStopPassiveMonitoring = actions.onStopPassiveMonitoring,
        onUpgradeClick = actions.onUpgradeClick,
    )
}

@Composable
private fun SettingsDisplayAndFeaturesSection(uiState: SettingsUiState, actions: DisplayAndFeaturesSectionActions) {
    DisplayAndFeaturesSection(
        state =
            DisplayAndFeaturesSectionState(
                themeMode = uiState.themeMode,
                waveformStyle = uiState.waveformStyle,
                refreshRate = uiState.refreshRate,
                lockscreenMeterEnabled = uiState.lockscreenMeterEnabled,
                showLockscreenMeterPublicly = uiState.showLockscreenMeterPublicly,
                technicalMetadataEnabled = uiState.technicalMetadataEnabled,
                dosimeterCardEnabled = uiState.dosimeterCardEnabled,
                soundDetectionEnabled = uiState.soundDetectionEnabled,
                sleepCardEnabled = uiState.sleepCardEnabled,
                voiceBaselineLevelDb = uiState.voiceBaselineLevelDb,
                voiceBaselineSampleCount = uiState.voiceBaselineSampleCount,
                canCalibrateVoiceBaseline = uiState.canCalibrateVoiceBaseline,
                isProUser = uiState.isProUser,
            ),
        actions = actions,
    )
}

@Composable
private fun SettingsDataExportSection(uiState: SettingsUiState, actions: DataExportSectionActions) {
    DataExportSection(
        state =
            DataExportSectionState(
                isProUser = uiState.isProUser,
                isCsvExporting = uiState.isCsvExporting,
                csvExportMessage = uiState.csvExportMessage,
                csvExportErrorMessage = uiState.csvExportErrorMessage,
                localBackups = uiState.localBackups,
                isBackupCreating = uiState.isBackupCreating,
                isBackupRestoring = uiState.isBackupRestoring,
                restoreCandidate = uiState.restoreCandidate,
                backupMessage = uiState.backupMessage,
                backupErrorMessage = uiState.backupErrorMessage,
                wavRecordingDefaultEnabled = uiState.wavRecordingDefaultEnabled,
                clearHistoryConfirmationVisible = uiState.clearHistoryConfirmationVisible,
                isHistoryClearing = uiState.isHistoryClearing,
                historyClearMessage = uiState.historyClearMessage,
                historyClearErrorMessage = uiState.historyClearErrorMessage,
            ),
        actions = actions,
    )
}

@Composable
private fun SettingsProUpsellCard(uiState: SettingsUiState, actions: ProUpsellCardActions) {
    if (!uiState.shouldShowProUpsell()) return

    ProUpsellCard(
        state =
            ProUpsellCardState(
                isPurchaseLaunching = uiState.isPurchaseLaunching,
                purchaseMessage = uiState.purchaseMessage,
                purchaseErrorMessage = uiState.purchaseErrorMessage,
                showDebugForceFree = BuildConfig.DEBUG,
                debugForceFreeEnabled = uiState.debugForceFreeEnabled,
            ),
        actions = actions,
    )
}

private fun SettingsUiState.shouldShowProUpsell(): Boolean = !isProUser ||
        BuildConfig.DEBUG ||
        purchaseMessage != null ||
        purchaseErrorMessage != null

private fun handleStartPassiveMonitoring(
    context: Context,
    micPermissionLauncher: ActivityResultLauncher<String>,
    notificationPermissionLauncher: ActivityResultLauncher<String>,
    viewModel: SettingsViewModel,
) {
    if (!context.hasRecordAudioPermission()) {
        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        return
    }

    requestPostNotificationsPermissionIfNeeded(context, notificationPermissionLauncher)
    viewModel.startPassiveMonitoring()
}

@Composable
private fun SettingsHeader() {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val spacing = DbCheckTheme.spacing

    Text(
        text = stringResource(R.string.settings_preferences_title),
        style = typography.labelMd,
        color = colors.material.onSurfaceVariant,
    )
    Text(
        text = stringResource(R.string.settings_title),
        style = typography.headlineLg,
        color = colors.material.onSurface,
    )
    Spacer(Modifier.height(spacing.space2))
}

@Composable
private fun SettingsFooter(modifier: Modifier = Modifier) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val spacing = DbCheckTheme.spacing

    Text(
        text = stringResource(R.string.settings_footer, BuildConfig.VERSION_NAME),
        style = typography.labelSm,
        color = colors.material.onSurfaceVariant,
        modifier = modifier.padding(vertical = spacing.space6),
    )
}

@Composable
private fun SettingsHealthSyncSection(uiState: SettingsUiState, actions: HealthSyncSectionActions) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        actions.onPermissionsChanged()
    }
    HealthSyncSection(
        state =
            HealthSyncSectionState(
                healthConnectEnabled = uiState.healthConnectEnabled,
                heartRateOverlayEnabled = uiState.heartRateOverlayEnabled,
                isProUser = uiState.isProUser,
                status = uiState.healthConnectStatus,
                healthConnectErrorMessage = uiState.healthConnectErrorMessage,
            ),
        actions = actions,
    )
}
