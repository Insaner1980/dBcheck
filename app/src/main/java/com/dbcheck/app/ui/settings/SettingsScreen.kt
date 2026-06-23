package com.dbcheck.app.ui.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
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
fun SettingsScreen(
    scrollToProCard: Boolean = false,
    onRestartAfterRestore: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context.findActivity()
    val scrollState = rememberScrollState()
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
        onClearPurchaseMessages = viewModel::clearPurchaseMessages,
        onClearCsvExportMessages = viewModel::clearCsvExportMessages,
        onClearBackupMessages = viewModel::clearBackupMessages,
        onClearHealthConnectMessages = viewModel::clearHealthConnectMessages,
        onClearCalibrationProfileMessages = viewModel::clearCalibrationProfileMessages,
        onRestartAfterRestore = onRestartAfterRestore,
    )

    Column(modifier = Modifier.fillMaxSize()) {
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
    val healthSync: HealthSyncSectionActions,
    val dataExport: DataExportSectionActions,
    val displayAndFeatures: DisplayAndFeaturesSectionActions,
    val proUpsell: ProUpsellCardActions,
)

private fun settingsContentActions(
    viewModel: SettingsViewModel,
    context: Context,
    onExportCsv: () -> Unit,
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
    onClearPurchaseMessages: () -> Unit,
    onClearCsvExportMessages: () -> Unit,
    onClearBackupMessages: () -> Unit,
    onClearHealthConnectMessages: () -> Unit,
    onClearCalibrationProfileMessages: () -> Unit,
    onRestartAfterRestore: () -> Unit,
) {
    val currentOnRestartAfterRestore by rememberUpdatedState(onRestartAfterRestore)

    LaunchedEffect(scrollToProCard) {
        if (scrollToProCard) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }
    SettingsMessageEffects(
        uiState = uiState,
        onClearPurchaseMessages = onClearPurchaseMessages,
        onClearCsvExportMessages = onClearCsvExportMessages,
        onClearBackupMessages = onClearBackupMessages,
        onClearHealthConnectMessages = onClearHealthConnectMessages,
        onClearCalibrationProfileMessages = onClearCalibrationProfileMessages,
    )
    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                SettingsEvent.RestartAfterRestore -> currentOnRestartAfterRestore()
            }
        }
    }
}

@Composable
private fun SettingsMessageEffects(
    uiState: SettingsUiState,
    onClearPurchaseMessages: () -> Unit,
    onClearCsvExportMessages: () -> Unit,
    onClearBackupMessages: () -> Unit,
    onClearHealthConnectMessages: () -> Unit,
    onClearCalibrationProfileMessages: () -> Unit,
) {
    val currentOnClearPurchaseMessages by rememberUpdatedState(onClearPurchaseMessages)
    val currentOnClearCsvExportMessages by rememberUpdatedState(onClearCsvExportMessages)
    val currentOnClearBackupMessages by rememberUpdatedState(onClearBackupMessages)
    val currentOnClearHealthConnectMessages by rememberUpdatedState(onClearHealthConnectMessages)
    val currentOnClearCalibrationProfileMessages by rememberUpdatedState(onClearCalibrationProfileMessages)

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
            ),
        actions = actions.audioCalibration,
    )
    NoiseNotificationsSection(
        exposureAlertsEnabled = uiState.exposureAlertsEnabled,
        peakWarningsEnabled = uiState.peakWarningsEnabled,
        notificationThreshold = uiState.notificationThreshold,
        onExposureAlertsChange = actions.onExposureAlertsChange,
        onPeakWarningsChange = actions.onPeakWarningsChange,
        onThresholdChange = actions.onThresholdChange,
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
                technicalMetadataEnabled = uiState.technicalMetadataEnabled,
                dosimeterCardEnabled = uiState.dosimeterCardEnabled,
                soundDetectionEnabled = uiState.soundDetectionEnabled,
                sleepCardEnabled = uiState.sleepCardEnabled,
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
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
