@file:Suppress("ViewModelForwarding")

package com.dbcheck.app.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dbcheck.app.BuildConfig
import com.dbcheck.app.R
import com.dbcheck.app.ui.common.findActivity
import com.dbcheck.app.ui.common.hasRecordAudioPermission
import com.dbcheck.app.ui.common.openAppPermissionSettings
import com.dbcheck.app.ui.common.requestPostNotificationsPermissionIfNeeded
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.DbCheckTopAppBar
import com.dbcheck.app.ui.navigation.Screen
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
import com.dbcheck.app.ui.settings.components.LockscreenMeterSection
import com.dbcheck.app.ui.settings.components.LockscreenMeterSectionActions
import com.dbcheck.app.ui.settings.components.LockscreenMeterSectionState
import com.dbcheck.app.ui.settings.components.NoiseNotificationsSection
import com.dbcheck.app.ui.settings.components.NoiseNotificationsSectionActions
import com.dbcheck.app.ui.settings.components.NoiseNotificationsSectionState
import com.dbcheck.app.ui.settings.components.OctaveCalibrationSection
import com.dbcheck.app.ui.settings.components.ProUpsellCard
import com.dbcheck.app.ui.settings.components.ProUpsellCardActions
import com.dbcheck.app.ui.settings.components.ProUpsellCardState
import com.dbcheck.app.ui.settings.state.SettingsUiState
import com.dbcheck.app.ui.theme.DbCheckTheme
import com.dbcheck.app.util.hasCoarseLocationPermission
import kotlinx.coroutines.delay

@Composable
fun SettingsHomePage(onNavigate: (String) -> Unit, viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = DbCheckTheme.spacing

    Column(modifier = modifier.fillMaxSize()) {
        DbCheckTopAppBar()
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.pageMargin),
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = DbCheckTheme.typography.headlineLg,
                color = DbCheckTheme.colorScheme.material.onSurface,
            )
            SettingsHubRow(
                title = stringResource(R.string.settings_page_calibration),
                icon = Icons.Outlined.Tune,
                route = Screen.Settings.CALIBRATION_ROUTE,
                onNavigate = onNavigate,
            )
            SettingsHubRow(
                title = stringResource(R.string.settings_page_notifications),
                icon = Icons.Outlined.Notifications,
                route = Screen.Settings.NOTIFICATIONS_ROUTE,
                onNavigate = onNavigate,
            )
            SettingsHubRow(
                title = stringResource(R.string.settings_page_data_privacy),
                icon = Icons.Outlined.Security,
                route = Screen.Settings.DATA_PRIVACY_ROUTE,
                onNavigate = onNavigate,
            )
            SettingsHubRow(
                title = stringResource(R.string.settings_page_display),
                icon = Icons.Outlined.Palette,
                route = Screen.Settings.DISPLAY_ROUTE,
                onNavigate = onNavigate,
            )
            SettingsHubRow(
                title = stringResource(R.string.settings_page_pro_about),
                icon = Icons.Outlined.Info,
                route = Screen.Settings.PRO_ABOUT_ROUTE,
                onNavigate = onNavigate,
            )
            Spacer(Modifier.height(spacing.space5))
        }
    }
}

@Composable
fun SettingsCalibrationPage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenOctaveCalibration: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onStartProPurchase = rememberProPurchaseAction(viewModel)
    SettingsPageScaffold(
        title = stringResource(R.string.settings_page_calibration),
        onBack = onBack,
        modifier = modifier,
    ) {
        AudioCalibrationSection(
            state =
                AudioCalibrationSectionState(
                    sensitivityOffset = uiState.micSensitivityOffset,
                    frequencyWeighting = uiState.frequencyWeighting,
                    responseTime = uiState.responseTime,
                    isProUser = uiState.isProUser,
                    profiles = uiState.calibrationProfiles,
                    selectedProfileId = uiState.selectedCalibrationProfileId,
                    profileErrorMessage = uiState.calibrationProfileErrorMessage,
                    audioInputDevices = uiState.audioInputDevices,
                    selectedAudioInputDeviceId = uiState.selectedAudioInputDeviceId,
                ),
            actions =
                AudioCalibrationSectionActions(
                    onSensitivityChange = viewModel::updateMicSensitivity,
                    onWeightingChange = viewModel::updateFrequencyWeighting,
                    onResponseTimeChange = viewModel::updateResponseTime,
                    onSelectAudioInputDevice = viewModel::selectAudioInputDevice,
                    onCreateProfile = viewModel::createCalibrationProfile,
                    onSelectProfile = viewModel::selectCalibrationProfile,
                    onRenameProfile = viewModel::renameCalibrationProfile,
                    onDeleteProfile = viewModel::deleteCalibrationProfile,
                    onOpenOctaveCalibration = onOpenOctaveCalibration,
                    onUpgradeClick = onStartProPurchase,
                ),
        )
    }
    SettingsMessageEffects(uiState = uiState, viewModel = viewModel)
}

@Composable
fun SettingsOctaveCalibrationPage(viewModel: SettingsViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profile =
        uiState.calibrationProfiles.firstOrNull {
            it.isSelected || it.id == uiState.selectedCalibrationProfileId
        }
    SettingsPageScaffold(
        title = stringResource(R.string.settings_page_octave_calibration),
        onBack = onBack,
        modifier = modifier,
    ) {
        OctaveCalibrationSection(
            profile = profile,
            onOffsetChange = viewModel::updateOctaveBandOffset,
            onReset = viewModel::resetOctaveBandOffsets,
        )
    }
    SettingsMessageEffects(uiState = uiState, viewModel = viewModel)
}

@Composable
fun SettingsNotificationsPage(viewModel: SettingsViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var passiveMonitoringPermissionDenied by rememberSaveable { mutableStateOf(false) }
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { Unit }
    val micPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                passiveMonitoringPermissionDenied = false
                requestPostNotificationsPermissionIfNeeded(context, notificationPermissionLauncher)
                viewModel.startPassiveMonitoring()
            } else {
                passiveMonitoringPermissionDenied = true
                viewModel.onPassiveMonitoringPermissionDenied()
            }
        }
    val onStartProPurchase = rememberProPurchaseAction(viewModel)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (context.hasRecordAudioPermission()) passiveMonitoringPermissionDenied = false
    }
    SettingsPageScaffold(
        title = stringResource(R.string.settings_page_notifications),
        onBack = onBack,
        modifier = modifier,
    ) {
        NoiseNotificationsSection(
            state =
                NoiseNotificationsSectionState(
                    exposureAlertsEnabled = uiState.exposureAlertsEnabled,
                    peakWarningsEnabled = uiState.peakWarningsEnabled,
                    notificationThreshold = uiState.notificationThreshold,
                    notificationSchedule = uiState.notificationSchedule,
                    audibleAlarmEnabled = uiState.audibleAlarmEnabled,
                    ttsRiskPromptEnabled = uiState.ttsRiskPromptEnabled,
                    passiveMonitoringActive = uiState.passiveMonitoringActive,
                    passiveMonitoringDailySummary = uiState.passiveMonitoringDailySummary,
                    passiveMonitoringErrorMessage = uiState.passiveMonitoringErrorMessage,
                    passiveMonitoringPermissionDenied = passiveMonitoringPermissionDenied,
                    isProUser = uiState.isProUser,
                ),
            actions =
                noiseNotificationActions(
                    viewModel = viewModel,
                    onStartPassiveMonitoring = {
                        handleStartPassiveMonitoring(
                            context = context,
                            micPermissionLauncher = micPermissionLauncher,
                            notificationPermissionLauncher = notificationPermissionLauncher,
                            viewModel = viewModel,
                        )
                    },
                    onOpenMicrophoneSettings = context::openAppPermissionSettings,
                    onStartProPurchase = onStartProPurchase,
                ),
        )
    }
    SettingsMessageEffects(uiState = uiState, viewModel = viewModel)
}

@Composable
fun SettingsDataPrivacyPage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onRestartAfterRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var coarseLocationPermissionGranted by rememberSaveable {
        mutableStateOf(context.hasCoarseLocationPermission())
    }
    var coarseLocationPermissionDenied by rememberSaveable { mutableStateOf(false) }
    val locationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            coarseLocationPermissionGranted = granted
            coarseLocationPermissionDenied = !granted
        }
    val csvShareChooserTitle = stringResource(R.string.settings_export_csv_chooser)
    val onStartProPurchase = rememberProPurchaseAction(viewModel)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        coarseLocationPermissionGranted = context.hasCoarseLocationPermission()
        if (coarseLocationPermissionGranted) coarseLocationPermissionDenied = false
        viewModel.refreshHealthConnectStatus()
    }
    LaunchedEffect(csvShareChooserTitle) {
        viewModel.csvExportIntents.collect { intent ->
            runCatching { context.startActivity(Intent.createChooser(intent, csvShareChooserTitle)) }
                .onSuccess { viewModel.onCsvShareStarted() }
                .onFailure { viewModel.onCsvShareUnavailable() }
        }
    }

    SettingsPageScaffold(
        title = stringResource(R.string.settings_page_data_privacy),
        onBack = onBack,
        modifier = modifier,
    ) {
        HealthSyncSection(
            state =
                HealthSyncSectionState(
                    healthConnectEnabled = uiState.healthConnectEnabled,
                    heartRateOverlayEnabled = uiState.heartRateOverlayEnabled,
                    isProUser = uiState.isProUser,
                    status = uiState.healthConnectStatus,
                    healthConnectErrorMessage = uiState.healthConnectErrorMessage,
                ),
            actions = healthSyncActions(viewModel, context, onStartProPurchase),
        )
        DataExportSection(
            state = dataExportState(uiState, coarseLocationPermissionGranted, coarseLocationPermissionDenied),
            actions =
                dataExportActions(
                    viewModel = viewModel,
                    onRestartAfterRestore = onRestartAfterRestore,
                    onRequestLocationPermission = {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    },
                    onOpenLocationSettings = context::openAppPermissionSettings,
                    onStartProPurchase = onStartProPurchase,
                ),
        )
        LockscreenMeterSection(
            state =
                LockscreenMeterSectionState(
                    lockscreenMeterEnabled = uiState.lockscreenMeterEnabled,
                    showLockscreenMeterPublicly = uiState.showLockscreenMeterPublicly,
                    isProUser = uiState.isProUser,
                ),
            actions =
                LockscreenMeterSectionActions(
                    onLockscreenMeterChange = viewModel::updateLockscreenMeter,
                    onShowLockscreenMeterPubliclyChange = viewModel::updateShowLockscreenMeterPublicly,
                    onUpgradeClick = onStartProPurchase,
                ),
        )
    }
    SettingsMessageEffects(uiState = uiState, viewModel = viewModel)
}

@Composable
fun SettingsDisplayPage(viewModel: SettingsViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onStartProPurchase = rememberProPurchaseAction(viewModel)
    SettingsPageScaffold(
        title = stringResource(R.string.settings_page_display),
        onBack = onBack,
        modifier = modifier,
    ) {
        DisplayAndFeaturesSection(
            state =
                DisplayAndFeaturesSectionState(
                    themeMode = uiState.themeMode,
                    waveformStyle = uiState.waveformStyle,
                    refreshRate = uiState.refreshRate,
                    technicalMetadataEnabled = uiState.technicalMetadataEnabled,
                    dosimeterCardEnabled = uiState.dosimeterCardEnabled,
                    soundDetectionEnabled = uiState.soundDetectionEnabled,
                    sleepCardEnabled = uiState.sleepCardEnabled,
                    isProUser = uiState.isProUser,
                ),
            actions =
                DisplayAndFeaturesSectionActions(
                    onThemeModeChange = viewModel::updateThemeMode,
                    onWaveformStyleChange = {
                        viewModel.updateDisplayPreference(DisplayPreferenceUpdate.WaveformStyleChange(it))
                    },
                    onRefreshRateChange = {
                        viewModel.updateDisplayPreference(DisplayPreferenceUpdate.RefreshRateChange(it))
                    },
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
                ),
        )
    }
    SettingsMessageEffects(uiState = uiState, viewModel = viewModel)
}

@Composable
fun SettingsProAboutPage(viewModel: SettingsViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onStartProPurchase = rememberProPurchaseAction(viewModel)
    SettingsPageScaffold(
        title = stringResource(R.string.settings_page_pro_about),
        onBack = onBack,
        modifier = modifier,
    ) {
        if (uiState.shouldShowProUpsell()) {
            ProUpsellCard(
                state =
                    ProUpsellCardState(
                        isPurchaseLaunching = uiState.isPurchaseLaunching,
                        purchaseMessage = uiState.purchaseMessage,
                        purchaseErrorMessage = uiState.purchaseErrorMessage,
                        showDebugForceFree = BuildConfig.DEBUG,
                        debugForceFreeEnabled = uiState.debugForceFreeEnabled,
                    ),
                actions =
                    ProUpsellCardActions(
                        onUpgradeClick = onStartProPurchase,
                        onDebugForceFreeChange = viewModel::updateDebugForceFree,
                    ),
            )
        }
        SettingsFooter()
    }
    SettingsMessageEffects(uiState = uiState, viewModel = viewModel)
}

@Composable
private fun SettingsHubRow(title: String, icon: ImageVector, route: String, onNavigate: (String) -> Unit) {
    val spacing = DbCheckTheme.spacing
    DbCheckCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = spacing.space12)
                .clickable { onNavigate(route) },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = DbCheckTheme.colorScheme.material.primary)
            Text(
                text = title,
                style = DbCheckTheme.typography.bodyLg,
                color = DbCheckTheme.colorScheme.material.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = DbCheckTheme.colorScheme.material.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsPageScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = DbCheckTheme.spacing
    Column(modifier = modifier.fillMaxSize()) {
        DbCheckTopAppBar(title = title, onBackClick = onBack)
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.pageMargin),
            verticalArrangement = Arrangement.spacedBy(spacing.space4),
            content = content,
        )
    }
}

@Composable
private fun rememberProPurchaseAction(viewModel: SettingsViewModel): () -> Unit {
    val activity = LocalContext.current.findActivity()
    return {
        if (activity != null) viewModel.launchProPurchase(activity) else viewModel.onPurchaseActivityUnavailable()
    }
}

private fun noiseNotificationActions(
    viewModel: SettingsViewModel,
    onStartPassiveMonitoring: () -> Unit,
    onOpenMicrophoneSettings: () -> Unit,
    onStartProPurchase: () -> Unit,
) = NoiseNotificationsSectionActions(
    onExposureAlertsChange = { viewModel.updateNoiseNotification(NoiseNotificationUpdate.ExposureAlerts(it)) },
    onPeakWarningsChange = { viewModel.updateNoiseNotification(NoiseNotificationUpdate.PeakWarnings(it)) },
    onThresholdChange = { viewModel.updateNoiseNotification(NoiseNotificationUpdate.NotificationThreshold(it)) },
    onScheduleChange = { viewModel.updateNoiseNotification(NoiseNotificationUpdate.NotificationSchedule(it)) },
    onAudibleAlarmChange = { viewModel.updateNoiseNotification(NoiseNotificationUpdate.AudibleAlarm(it)) },
    onTtsRiskPromptChange = { viewModel.updateNoiseNotification(NoiseNotificationUpdate.TtsRiskPrompt(it)) },
    onAudibleAlarmPreview = viewModel::previewAudibleAlarm,
    onStartPassiveMonitoring = onStartPassiveMonitoring,
    onStopPassiveMonitoring = viewModel::stopPassiveMonitoring,
    onOpenMicrophoneSettings = onOpenMicrophoneSettings,
    onUpgradeClick = onStartProPurchase,
)

private fun healthSyncActions(viewModel: SettingsViewModel, context: Context, onStartProPurchase: () -> Unit) =
    HealthSyncSectionActions(
        onHealthConnectChange = viewModel::updateHealthConnectEnabled,
        onHeartRateOverlayChange = viewModel::updateHeartRateOverlayEnabled,
        onPermissionsChanged = viewModel::refreshHealthConnectStatus,
        onOpenHealthConnectInstall = {
            runCatching {
                context.startActivity(viewModel.createHealthConnectIntent(HealthConnectIntentTarget.INSTALL))
            }
                .onFailure { viewModel.onHealthConnectInstallUnavailable() }
        },
        onOpenHealthConnectManageData = {
            runCatching {
                context.startActivity(viewModel.createHealthConnectIntent(HealthConnectIntentTarget.MANAGE_DATA))
            }.onFailure { viewModel.onHealthConnectInstallUnavailable() }
        },
        onUpgradeClick = onStartProPurchase,
    )

private fun dataExportActions(
    viewModel: SettingsViewModel,
    onRestartAfterRestore: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onStartProPurchase: () -> Unit,
) = DataExportSectionActions(
    onExportCsv = viewModel::createCsvExportIntent,
    onCreateBackup = viewModel::createLocalBackup,
    onRequestRestoreBackup = viewModel::requestRestoreBackup,
    onConfirmRestoreBackup = { viewModel.confirmRestoreBackup(onRestartAfterRestore) },
    onDismissRestoreBackup = viewModel::dismissRestoreBackup,
    onWavRecordingDefaultChange = viewModel::updateWavRecordingDefaultEnabled,
    onRequestClearHistory = viewModel::requestClearHistory,
    onConfirmClearHistory = viewModel::confirmClearHistory,
    onDismissClearHistory = viewModel::dismissClearHistory,
    onRequestLocationPermission = onRequestLocationPermission,
    onOpenLocationSettings = onOpenLocationSettings,
    onUpgradeClick = onStartProPurchase,
)

private fun dataExportState(
    uiState: SettingsUiState,
    coarseLocationPermissionGranted: Boolean,
    coarseLocationPermissionDenied: Boolean,
) = DataExportSectionState(
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
    coarseLocationPermissionGranted = coarseLocationPermissionGranted,
    coarseLocationPermissionDenied = coarseLocationPermissionDenied,
)

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
private fun SettingsMessageEffects(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    TimedMessageEffect(uiState.purchaseMessage, uiState.purchaseErrorMessage, viewModel::clearPurchaseMessages)
    TimedMessageEffect(uiState.csvExportMessage, uiState.csvExportErrorMessage, viewModel::clearCsvExportMessages)
    TimedMessageEffect(uiState.backupMessage, uiState.backupErrorMessage, viewModel::clearBackupMessages)
    TimedMessageEffect(uiState.healthConnectErrorMessage, onClear = viewModel::clearHealthConnectMessages)
    TimedMessageEffect(uiState.calibrationProfileErrorMessage, onClear = viewModel::clearCalibrationProfileMessages)
    TimedMessageEffect(uiState.passiveMonitoringErrorMessage, onClear = viewModel::clearPassiveMonitoringMessages)
}

@Composable
private fun TimedMessageEffect(message: String?, error: String? = null, onClear: () -> Unit) {
    val currentOnClear by rememberUpdatedState(onClear)
    LaunchedEffect(message, error) {
        if (message != null || error != null) {
            delay(3_000L)
            currentOnClear()
        }
    }
}

private fun SettingsUiState.shouldShowProUpsell(): Boolean =
    !isProUser || BuildConfig.DEBUG || purchaseMessage != null || purchaseErrorMessage != null

@Composable
private fun SettingsFooter() {
    Text(
        text = stringResource(R.string.settings_footer, BuildConfig.VERSION_NAME),
        style = DbCheckTheme.typography.labelSm,
        color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(vertical = DbCheckTheme.spacing.space6),
    )
}
