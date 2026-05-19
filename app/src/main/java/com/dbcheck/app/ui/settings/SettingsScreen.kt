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
import com.dbcheck.app.ui.settings.components.DataExportSection
import com.dbcheck.app.ui.settings.components.DataExportSectionActions
import com.dbcheck.app.ui.settings.components.DataExportSectionState
import com.dbcheck.app.ui.settings.components.DisplayAppearanceSection
import com.dbcheck.app.ui.settings.components.HealthSyncSection
import com.dbcheck.app.ui.settings.components.HealthSyncSectionActions
import com.dbcheck.app.ui.settings.components.HealthSyncSectionState
import com.dbcheck.app.ui.settings.components.LockscreenMeterSection
import com.dbcheck.app.ui.settings.components.NoiseNotificationsSection
import com.dbcheck.app.ui.settings.components.ProUpsellCard
import com.dbcheck.app.ui.settings.components.ProUpsellCardActions
import com.dbcheck.app.ui.settings.components.ProUpsellCardState
import com.dbcheck.app.ui.settings.state.SettingsUiState
import com.dbcheck.app.ui.theme.DbCheckTheme
import kotlinx.coroutines.delay

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
        viewModel = viewModel,
        onRestartAfterRestore = onRestartAfterRestore,
    )

    Column(modifier = Modifier.fillMaxSize()) {
        DbCheckTopAppBar()

        SettingsContent(
            uiState = uiState,
            viewModel = viewModel,
            context = context,
            scrollState = scrollState,
            onStartProPurchase = onStartProPurchase,
            onExportCsv = onExportCsv,
        )
    }
}

@Composable
private fun SettingsEffects(
    scrollToProCard: Boolean,
    scrollState: androidx.compose.foundation.ScrollState,
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onRestartAfterRestore: () -> Unit,
) {
    LaunchedEffect(scrollToProCard) {
        if (scrollToProCard) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }
    SettingsMessageEffects(uiState, viewModel)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SettingsEvent.RestartAfterRestore -> onRestartAfterRestore()
            }
        }
    }
}

@Composable
private fun SettingsMessageEffects(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    LaunchedEffect(uiState.purchaseMessage, uiState.purchaseErrorMessage) {
        if (uiState.purchaseMessage != null || uiState.purchaseErrorMessage != null) {
            delay(3_000L)
            viewModel.clearPurchaseMessages()
        }
    }
    LaunchedEffect(uiState.csvExportMessage, uiState.csvExportErrorMessage) {
        if (uiState.csvExportMessage != null || uiState.csvExportErrorMessage != null) {
            delay(3_000L)
            viewModel.clearCsvExportMessages()
        }
    }
    LaunchedEffect(uiState.backupMessage, uiState.backupErrorMessage) {
        if (uiState.backupMessage != null || uiState.backupErrorMessage != null) {
            delay(3_000L)
            viewModel.clearBackupMessages()
        }
    }
    LaunchedEffect(uiState.healthConnectErrorMessage) {
        if (uiState.healthConnectErrorMessage != null) {
            delay(3_000L)
            viewModel.clearHealthConnectMessages()
        }
    }
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    context: Context,
    scrollState: androidx.compose.foundation.ScrollState,
    onStartProPurchase: () -> Unit,
    onExportCsv: () -> Unit,
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
        SettingsAudioAndNotificationSections(uiState, viewModel, onStartProPurchase)
        SettingsHealthSyncSection(uiState, viewModel, context, onStartProPurchase)
        SettingsDataExportSection(uiState, viewModel, onExportCsv, onStartProPurchase)
        DisplayAppearanceSection(
            themeMode = uiState.themeMode,
            waveformStyle = uiState.waveformStyle,
            refreshRate = uiState.refreshRate,
            onThemeModeChange = viewModel::updateThemeMode,
            onWaveformStyleChange = {
                viewModel.updateDisplayPreference(DisplayPreferenceUpdate.WaveformStyleChange(it))
            },
            onRefreshRateChange = {
                viewModel.updateDisplayPreference(DisplayPreferenceUpdate.RefreshRateChange(it))
            },
        )
        SettingsProUpsellCard(
            uiState = uiState,
            onStartProPurchase = onStartProPurchase,
            onDebugForceFreeChange = viewModel::updateDebugForceFree,
        )
        SettingsFooter(modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun SettingsAudioAndNotificationSections(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onStartProPurchase: () -> Unit,
) {
    AudioCalibrationSection(
        sensitivityOffset = uiState.micSensitivityOffset,
        frequencyWeighting = uiState.frequencyWeighting,
        isProUser = uiState.isProUser,
        onSensitivityChange = viewModel::updateMicSensitivity,
        onWeightingChange = viewModel::updateFrequencyWeighting,
        onUpgradeClick = onStartProPurchase,
    )
    NoiseNotificationsSection(
        exposureAlertsEnabled = uiState.exposureAlertsEnabled,
        peakWarningsEnabled = uiState.peakWarningsEnabled,
        notificationThreshold = uiState.notificationThreshold,
        onExposureAlertsChange = {
            viewModel.updateNoiseNotification(NoiseNotificationUpdate.ExposureAlerts(it))
        },
        onPeakWarningsChange = {
            viewModel.updateNoiseNotification(NoiseNotificationUpdate.PeakWarnings(it))
        },
        onThresholdChange = {
            viewModel.updateNoiseNotification(NoiseNotificationUpdate.NotificationThreshold(it))
        },
    )
    LockscreenMeterSection(
        lockscreenMeterEnabled = uiState.lockscreenMeterEnabled,
        isProUser = uiState.isProUser,
        onLockscreenMeterChange = viewModel::updateLockscreenMeter,
        onUpgradeClick = onStartProPurchase,
    )
}

@Composable
private fun SettingsDataExportSection(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onExportCsv: () -> Unit,
    onStartProPurchase: () -> Unit,
) {
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
            ),
        actions =
            DataExportSectionActions(
                onExportCsv = onExportCsv,
                onCreateBackup = viewModel::createLocalBackup,
                onRequestRestoreBackup = viewModel::requestRestoreBackup,
                onConfirmRestoreBackup = viewModel::confirmRestoreBackup,
                onDismissRestoreBackup = viewModel::dismissRestoreBackup,
                onUpgradeClick = onStartProPurchase,
            ),
    )
}

@Composable
private fun SettingsProUpsellCard(
    uiState: SettingsUiState,
    onStartProPurchase: () -> Unit,
    onDebugForceFreeChange: (Boolean) -> Unit,
) {
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
        actions =
            ProUpsellCardActions(
                onUpgradeClick = onStartProPurchase,
                onDebugForceFreeChange = onDebugForceFreeChange,
            ),
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
private fun SettingsHealthSyncSection(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    context: Context,
    onNavigateToUpgrade: () -> Unit,
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshHealthConnectStatus()
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
        actions =
            HealthSyncSectionActions(
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
                onUpgradeClick = onNavigateToUpgrade,
            ),
    )
}
