package com.dbcheck.app.ui.meter

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dbcheck.app.R
import com.dbcheck.app.ui.common.KeepScreenOnController
import com.dbcheck.app.ui.common.KeepScreenOnEffect
import com.dbcheck.app.ui.common.PostNotificationPermissionPolicy
import com.dbcheck.app.ui.common.findActivity
import com.dbcheck.app.ui.common.hasRecordAudioPermission
import com.dbcheck.app.ui.common.requestPostNotificationsPermissionIfNeeded
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckChip
import com.dbcheck.app.ui.components.DbCheckTopAppBar
import com.dbcheck.app.ui.components.shouldUseCompactHeightScrolling
import com.dbcheck.app.ui.meter.components.CircularGauge
import com.dbcheck.app.ui.meter.components.DosimeterGaugeCard
import com.dbcheck.app.ui.meter.components.LiveSoundLevelChart
import com.dbcheck.app.ui.meter.components.MeterControls
import com.dbcheck.app.ui.meter.components.MeterControlsActions
import com.dbcheck.app.ui.meter.components.MeterControlsState
import com.dbcheck.app.ui.meter.components.MeterSessionInfoBar
import com.dbcheck.app.ui.meter.components.SoundReferenceCard
import com.dbcheck.app.ui.meter.components.StatCard
import com.dbcheck.app.ui.meter.components.WaveformVisualization
import com.dbcheck.app.ui.meter.state.MeasurementMode
import com.dbcheck.app.ui.meter.state.MeterUiState
import com.dbcheck.app.ui.sleep.SleepSetupEntryDestination
import com.dbcheck.app.ui.sleep.SleepSetupEntryPolicy
import com.dbcheck.app.ui.sleep.components.SleepSetupCta
import com.dbcheck.app.ui.theme.DbCheckTheme

@Suppress("LongMethod")
@Composable
fun MeterScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToSessionDetail: (Long) -> Unit,
    onNavigateToCameraOverlay: () -> Unit = {},
    onNavigateToSleepSetup: () -> Unit = {},
    onNavigateToUpgrade: () -> Unit = onNavigateToSettings,
    viewModel: MeterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val window = context.findActivity()?.window
    val shareChooserTitle = stringResource(R.string.meter_share_chooser)
    val lifecycleOwner = LocalLifecycleOwner.current

    KeepScreenOnEffect(enabled = uiState.isRecording, window = window)

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            viewModel.onMicPermissionResult(granted)
            if (!granted) {
                viewModel.onMicPermissionDenied()
            }
        }

    // Android 13+ notification-lupa pyydetään vasta, kun käyttäjä käynnistää mittauksen.
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) {
            viewModel.onNotificationPermissionRequested()
        }

    LaunchedEffect(Unit) {
        requestMicPermissionIfNeeded(context, viewModel, permissionLauncher)
    }

    DisposableEffect(lifecycleOwner, context, viewModel) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    refreshMicPermissionState(context, viewModel)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState.completedSessionId) {
        uiState.completedSessionId?.let { sessionId ->
            onNavigateToSessionDetail(sessionId)
            viewModel.onSessionDetailOpened()
        }
    }

    LaunchedEffect(shareChooserTitle) {
        viewModel.shareIntents.collect { intent ->
            runCatching {
                context.startActivity(Intent.createChooser(intent, shareChooserTitle))
            }.onFailure {
                viewModel.onShareUnavailable()
            }
        }
    }

    MeterScreenBody(
        uiState = uiState,
        actions =
            MeterScreenActions(
                onNavigateToSettings = onNavigateToSettings,
                onOpenMicSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        },
                    )
                },
                onRequestMicPermission = {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                onToggleRecording = {
                    handleToggleRecording(
                        context = context,
                        uiState = uiState,
                        micPermissionLauncher = permissionLauncher,
                        notificationPermissionLauncher = notificationPermissionLauncher,
                        viewModel = viewModel,
                    )
                },
                onReset = viewModel::resetMeasurement,
                onShare = viewModel::createShareIntent,
                onSelectMeasurementMode = viewModel::setMeasurementMode,
                onCameraOverlayClick = {
                    when (MeterCameraOverlayEntryPolicy.destination(uiState.isProUser)) {
                        MeterCameraOverlayEntryDestination.CameraOverlay -> onNavigateToCameraOverlay()
                        MeterCameraOverlayEntryDestination.Upgrade -> onNavigateToUpgrade()
                    }
                },
                onSleepSetupClick = {
                    when (SleepSetupEntryPolicy.destination(uiState.isProUser)) {
                        SleepSetupEntryDestination.SleepSetup -> onNavigateToSleepSetup()
                        SleepSetupEntryDestination.Upgrade -> onNavigateToUpgrade()
                    }
                },
            ),
    )
}

private data class MeterScreenActions(
    val onNavigateToSettings: () -> Unit,
    val onOpenMicSettings: () -> Unit,
    val onRequestMicPermission: () -> Unit,
    val onToggleRecording: () -> Unit,
    val onReset: () -> Unit,
    val onShare: () -> Unit,
    val onSelectMeasurementMode: (MeasurementMode) -> Unit,
    val onCameraOverlayClick: () -> Unit,
    val onSleepSetupClick: () -> Unit,
)

@Composable
private fun MeterScreenBody(uiState: MeterUiState, actions: MeterScreenActions) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DbCheckTopAppBar(
            actionIcon = Icons.Outlined.Settings,
            actionContentDescription = stringResource(R.string.a11y_open_settings),
            onActionClick = actions.onNavigateToSettings,
        )

        if (uiState.showMicDeniedPrompt) {
            // Kokoruudun mikrofoniestokehotus specin kohdan 11 mukaan.
            MicPermissionDeniedPrompt(
                onOpenSettings = actions.onOpenMicSettings,
                onRetry = actions.onRequestMicPermission,
            )
        } else {
            MeterContent(
                uiState = uiState,
                actions = actions,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun handleToggleRecording(
    context: android.content.Context,
    uiState: MeterUiState,
    micPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    notificationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    viewModel: MeterViewModel,
) {
    if (!uiState.isMicPermissionGranted) {
        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    } else {
        requestPostNotificationsPermissionIfNeeded(
            context = context,
            launcher = notificationPermissionLauncher,
            notificationPermissionAlreadyRequested = uiState.notificationPermissionAlreadyRequested,
        )
        viewModel.toggleRecording()
    }
}

private fun refreshMicPermissionState(context: android.content.Context, viewModel: MeterViewModel) {
    viewModel.onMicPermissionResult(context.hasRecordAudioPermission())
}

private fun requestMicPermissionIfNeeded(
    context: android.content.Context,
    viewModel: MeterViewModel,
    launcher: androidx.activity.result.ActivityResultLauncher<String>,
) {
    val granted = context.hasRecordAudioPermission()
    viewModel.onMicPermissionResult(granted)
    if (MeterStartupPermissionPolicy.startupRequest(granted).requestMicrophone) {
        launcher.launch(Manifest.permission.RECORD_AUDIO)
    }
}

internal data class MeterStartupPermissionRequest(val requestMicrophone: Boolean)

internal object MeterStartupPermissionPolicy {
    fun startupRequest(microphoneGranted: Boolean): MeterStartupPermissionRequest = MeterStartupPermissionRequest(
            requestMicrophone = !microphoneGranted,
        )
}

internal object MeterNotificationPermissionPolicy {
    fun shouldRequestNotificationPermission(
        sdkInt: Int,
        notificationPermissionGranted: Boolean,
        notificationPermissionAlreadyRequested: Boolean,
    ): Boolean = PostNotificationPermissionPolicy.shouldRequestNotificationPermission(
        sdkInt = sdkInt,
        notificationPermissionGranted = notificationPermissionGranted,
        notificationPermissionAlreadyRequested = notificationPermissionAlreadyRequested,
    )
}

internal typealias MeterKeepScreenOnController = KeepScreenOnController

@Composable
private fun MicPermissionDeniedPrompt(onOpenSettings: () -> Unit, onRetry: () -> Unit) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val spacing = DbCheckTheme.spacing

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Mic,
            contentDescription = null,
            tint = colors.material.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(spacing.space6))
        Text(
            text = stringResource(R.string.meter_microphone_permission_title),
            style = typography.headlineMd,
            color = colors.material.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.space3))
        Text(
            text = stringResource(R.string.meter_microphone_permission_description),
            style = typography.bodyMd,
            color = colors.material.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.space8))
        DbCheckButton(
            text = stringResource(R.string.action_open_settings),
            onClick = onOpenSettings,
            height = 48.dp,
        )
        Spacer(Modifier.height(spacing.space3))
        DbCheckButton(
            text = stringResource(R.string.action_try_again),
            onClick = onRetry,
            style = DbCheckButtonStyle.Secondary,
            height = 48.dp,
        )
    }
}

@Composable
private fun MeterContent(uiState: MeterUiState, actions: MeterScreenActions, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val useScrollableContent = shouldUseCompactHeightScrolling(maxHeight.value)
        if (useScrollableContent) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MeterReadoutContent(
                    uiState = uiState,
                    onSelectMeasurementMode = actions.onSelectMeasurementMode,
                    onLockedDosimeterClick = actions.onNavigateToSettings,
                    onSleepSetupClick = actions.onSleepSetupClick,
                )
                Spacer(Modifier.height(DbCheckTheme.spacing.space6))
                MeterControlsSection(uiState = uiState, actions = actions)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MeterReadoutContent(
                    uiState = uiState,
                    onSelectMeasurementMode = actions.onSelectMeasurementMode,
                    onLockedDosimeterClick = actions.onNavigateToSettings,
                    onSleepSetupClick = actions.onSleepSetupClick,
                )
                Spacer(Modifier.weight(1f))
                MeterControlsSection(uiState = uiState, actions = actions)
            }
        }
    }
}

@Composable
private fun MeterControlsSection(uiState: MeterUiState, actions: MeterScreenActions) {
    MeterControls(
        state =
            MeterControlsState(
                isRecording = uiState.isRecording,
                isShareEnabled = uiState.canShare,
                isCameraOverlayEnabled = uiState.isProUser,
            ),
        actions =
            MeterControlsActions(
                onToggleRecording = actions.onToggleRecording,
                onReset = actions.onReset,
                onShare = actions.onShare,
                onCameraOverlayClick = actions.onCameraOverlayClick,
            ),
        modifier = Modifier.padding(bottom = DbCheckTheme.spacing.space6),
    )
}

@Composable
private fun MeterReadoutContent(
    uiState: MeterUiState,
    onSelectMeasurementMode: (MeasurementMode) -> Unit,
    onLockedDosimeterClick: () -> Unit,
    onSleepSetupClick: () -> Unit,
) {
    var soundReferenceExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(DbCheckTheme.spacing.space8))

        MeterModeChipRow(
            measurementMode = uiState.measurementMode,
            isProUser = uiState.isProUser,
            dosimeterCardEnabled = uiState.dosimeterCardEnabled,
            onSelectMode = onSelectMeasurementMode,
            onLockedDosimeterClick = onLockedDosimeterClick,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(Modifier.height(DbCheckTheme.spacing.space6))

        if (uiState.sessionInfo.isRecording) {
            MeterSessionInfoBar(
                sessionInfo = uiState.sessionInfo,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(DbCheckTheme.spacing.space4))
        }

        CircularGauge(
            currentDb = uiState.currentDb,
            noiseLevel = uiState.noiseLevel,
        )

        Spacer(Modifier.height(DbCheckTheme.spacing.space6))

        if (uiState.dosimeterCardEnabled && uiState.measurementMode == MeasurementMode.DOSIMETER) {
            DosimeterGaugeCard(
                dosimeter = uiState.dosimeter,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(DbCheckTheme.spacing.space4))
        } else {
            LiveSoundLevelChart(
                points = uiState.liveChartPoints,
                isRecording = uiState.isRecording,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(DbCheckTheme.spacing.space4))
        }

        SoundReferenceCard(
            currentDb = uiState.currentDb,
            markers = uiState.soundReferenceMarkers,
            nearestMarker = uiState.nearestSoundReferenceMarker,
            currentPosition = uiState.soundReferenceCurrentPosition,
            expanded = soundReferenceExpanded,
            onExpandedChange = { soundReferenceExpanded = it },
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(Modifier.height(DbCheckTheme.spacing.space4))

        WaveformVisualization(
            data = uiState.waveformData,
            style = uiState.waveformStyle,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(Modifier.height(DbCheckTheme.spacing.space4))

        MeterErrorMessage(error = uiState.error)

        MeterStatsRow(uiState = uiState)

        if (uiState.sleepCardEnabled) {
            Spacer(Modifier.height(DbCheckTheme.spacing.space4))
            SleepSetupCta(
                onOpenSleepSetup = onSleepSetupClick,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
}

@Composable
private fun MeterStatsRow(uiState: MeterUiState) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatCard(
            label = stringResource(R.string.report_metric_min),
            value = uiState.minDb,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label =
                if (uiState.isProUser) {
                    uiState.equivalentLevelLabel
                } else {
                    stringResource(R.string.session_stat_avg)
                },
            value = uiState.equivalentLevelDb ?: uiState.avgDb,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = stringResource(R.string.report_metric_max),
            value = uiState.maxDb,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MeterErrorMessage(error: String?) {
    if (error == null) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = error,
            style = DbCheckTheme.typography.bodyMd,
            color = DbCheckTheme.colorScheme.material.error,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(DbCheckTheme.spacing.space3))
    }
}

@Composable
internal fun MeterModeChipRow(
    measurementMode: MeasurementMode,
    isProUser: Boolean,
    dosimeterCardEnabled: Boolean,
    onSelectMode: (MeasurementMode) -> Unit,
    onLockedDosimeterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val effectiveMeasurementMode = effectiveMeterMode(measurementMode, dosimeterCardEnabled)
    val dbMeterDescription = dbMeterModeDescription(effectiveMeasurementMode == MeasurementMode.DB_METER)
    val dosimeterSelected = dosimeterCardEnabled && effectiveMeasurementMode == MeasurementMode.DOSIMETER
    val dosimeterText = dosimeterModeText(isProUser)
    val dosimeterDescription = dosimeterModeDescription(dosimeterCardEnabled, dosimeterSelected)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space3),
    ) {
        DbCheckChip(
            text = stringResource(R.string.meter_mode_db_meter),
            selected = effectiveMeasurementMode == MeasurementMode.DB_METER,
            onClick = { onSelectMode(MeasurementMode.DB_METER) },
            modifier =
                Modifier.semantics {
                    contentDescription = dbMeterDescription
                },
        )

        if (dosimeterCardEnabled || !isProUser) {
            DbCheckChip(
                text = dosimeterText,
                selected = dosimeterSelected,
                onClick = {
                    handleDosimeterModeClick(dosimeterCardEnabled, onSelectMode, onLockedDosimeterClick)
                },
                modifier =
                    Modifier.semantics {
                        contentDescription = dosimeterDescription
                    },
            )
        }
    }
}

private fun effectiveMeterMode(measurementMode: MeasurementMode, dosimeterCardEnabled: Boolean): MeasurementMode =
    if (dosimeterCardEnabled) measurementMode else MeasurementMode.DB_METER

@Composable
private fun dbMeterModeDescription(isSelected: Boolean): String = stringResource(
        if (isSelected) {
            R.string.a11y_meter_mode_db_meter_selected
        } else {
            R.string.a11y_meter_mode_db_meter
        },
    )

@Composable
private fun dosimeterModeText(isProUser: Boolean): String = stringResource(
        if (isProUser) {
            R.string.meter_mode_dosimeter
        } else {
            R.string.meter_mode_dosimeter_locked
        },
    )

@Composable
private fun dosimeterModeDescription(dosimeterCardEnabled: Boolean, dosimeterSelected: Boolean): String = when {
        !dosimeterCardEnabled -> stringResource(R.string.a11y_meter_mode_dosimeter_locked)
        dosimeterSelected -> stringResource(R.string.a11y_meter_mode_dosimeter_selected)
        else -> stringResource(R.string.a11y_meter_mode_dosimeter)
    }

private fun handleDosimeterModeClick(
    dosimeterCardEnabled: Boolean,
    onSelectMode: (MeasurementMode) -> Unit,
    onLockedDosimeterClick: () -> Unit,
) {
    if (dosimeterCardEnabled) {
        onSelectMode(MeasurementMode.DOSIMETER)
    } else {
        onLockedDosimeterClick()
    }
}
