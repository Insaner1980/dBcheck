package com.dbcheck.app.ui.sleep

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dbcheck.app.R
import com.dbcheck.app.ui.common.KeepScreenOnEffect
import com.dbcheck.app.ui.common.findActivity
import com.dbcheck.app.ui.common.hasRecordAudioPermission
import com.dbcheck.app.ui.common.openAppPermissionSettings
import com.dbcheck.app.ui.common.requestPostNotificationsPermissionIfNeeded
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.DbCheckChip
import com.dbcheck.app.ui.components.DbCheckSetupHeader
import com.dbcheck.app.ui.components.DbCheckSetupScaffold
import com.dbcheck.app.ui.components.DbCheckToggle
import com.dbcheck.app.ui.theme.DbCheckTheme
import com.dbcheck.app.util.DurationFormatter

@Composable
fun SleepSetupRoute(
    onBack: () -> Unit,
    onNavigateToUpgrade: () -> Unit,
    viewModel: SleepSetupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val window = context.findActivity()?.window
    val currentOnNavigateToUpgrade by rememberUpdatedState(onNavigateToUpgrade)

    KeepScreenOnEffect(
        enabled = uiState.isRecording && uiState.keepAwakeEnabled,
        window = window,
    )

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            viewModel.onMicPermissionResult(granted)
            if (granted) {
                viewModel.startSleepRecording()
            } else {
                viewModel.onMicPermissionDenied()
            }
        }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) {
            viewModel.onNotificationPermissionRequested()
        }

    LaunchedEffect(uiState.availability) {
        if (uiState.availability == SleepSetupAvailability.Locked) {
            currentOnNavigateToUpgrade()
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onMicPermissionResult(context.hasRecordAudioPermission())
    }

    SleepSetupScreen(
        uiState = uiState,
        actions =
            SleepSetupActions(
                onBack = onBack,
                onDurationChange = viewModel::updateTargetDurationMinutes,
                onKeepAwakeChange = viewModel::updateKeepAwakeEnabled,
                onStartRecording = {
                    handleStartSleepRecording(
                        context = context,
                        uiState = uiState,
                        micPermissionLauncher = permissionLauncher,
                        notificationPermissionLauncher = notificationPermissionLauncher,
                        viewModel = viewModel,
                    )
                },
                onStopRecording = viewModel::stopSleepRecording,
                onOpenMicSettings = context::openAppPermissionSettings,
            ),
    )
}

data class SleepSetupActions(
    val onBack: () -> Unit = {},
    val onDurationChange: (Int) -> Unit = {},
    val onKeepAwakeChange: (Boolean) -> Unit = {},
    val onStartRecording: () -> Unit = {},
    val onStopRecording: () -> Unit = {},
    val onOpenMicSettings: () -> Unit = {},
)

@Composable
fun SleepSetupScreen(
    modifier: Modifier = Modifier,
    uiState: SleepSetupUiState = SleepSetupUiState(availability = SleepSetupAvailability.Ready),
    actions: SleepSetupActions = SleepSetupActions(),
) {
    val spacing = DbCheckTheme.spacing

    DbCheckSetupScaffold(
        onBack = actions.onBack,
        modifier = modifier,
        contentVerticalArrangement = Arrangement.spacedBy(spacing.space4),
        header = {
            DbCheckSetupHeader(
                phase = stringResource(R.string.sleep_setup_window_title),
                title = stringResource(R.string.sleep_setup_title),
                description = stringResource(R.string.sleep_setup_description),
            )
        },
        cta = {
            SleepRecordingActionCard(
                uiState = uiState,
                actions = actions,
            )
        },
    ) {
        SleepDurationOptionsCard(
            optionsMinutes = uiState.durationOptionsMinutes,
            selectedDurationMinutes = uiState.targetDurationMinutes,
            onDurationChange = actions.onDurationChange,
        )
        SleepKeepAwakeCard(
            keepAwakeEnabled = uiState.keepAwakeEnabled,
            onKeepAwakeChange = actions.onKeepAwakeChange,
        )
        SleepSetupNotesCard()
    }
}

private fun handleStartSleepRecording(
    context: android.content.Context,
    uiState: SleepSetupUiState,
    micPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    notificationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    viewModel: SleepSetupViewModel,
) {
    if (!context.hasRecordAudioPermission()) {
        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    } else {
        requestPostNotificationsPermissionIfNeeded(
            context = context,
            launcher = notificationPermissionLauncher,
            notificationPermissionAlreadyRequested = uiState.notificationPermissionAlreadyRequested,
        )
        viewModel.onMicPermissionResult(granted = true)
        viewModel.startSleepRecording()
    }
}

@Composable
private fun SleepDurationOptionsCard(
    optionsMinutes: List<Int>,
    selectedDurationMinutes: Int,
    onDurationChange: (Int) -> Unit,
) {
    val spacing = DbCheckTheme.spacing

    DbCheckCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
            SleepSetupCardText(
                title = stringResource(R.string.sleep_setup_duration_title),
                subtitle = stringResource(R.string.sleep_setup_duration_subtitle),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                verticalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                optionsMinutes.forEach { durationMinutes ->
                    SleepSetupDurationOption(
                        durationMinutes = durationMinutes,
                        selected = durationMinutes == selectedDurationMinutes,
                        onClick = { onDurationChange(durationMinutes) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SleepSetupDurationOption(durationMinutes: Int, selected: Boolean, onClick: () -> Unit) {
    DbCheckChip(
        text = stringResource(durationLabelStringRes(durationMinutes)),
        selected = selected,
        onClick = onClick,
    )
}

@Composable
private fun SleepKeepAwakeCard(keepAwakeEnabled: Boolean, onKeepAwakeChange: (Boolean) -> Unit) {
    val spacing = DbCheckTheme.spacing

    DbCheckCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SleepSetupCardText(
                title = stringResource(R.string.sleep_setup_keep_awake_title),
                subtitle = stringResource(R.string.sleep_setup_keep_awake_subtitle),
                modifier = Modifier.weight(1f),
            )
            DbCheckToggle(
                checked = keepAwakeEnabled,
                onCheckedChange = onKeepAwakeChange,
            )
        }
    }
}

@Composable
private fun SleepRecordingActionCard(uiState: SleepSetupUiState, actions: SleepSetupActions) {
    val spacing = DbCheckTheme.spacing

    DbCheckCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
            SleepSetupCardText(
                title = sleepRecordingTitle(uiState.isRecording),
                subtitle = sleepRecordingSubtitle(uiState),
            )
            uiState.error?.let { error ->
                Text(
                    text = error,
                    style = DbCheckTheme.typography.bodyMd,
                    color = DbCheckTheme.colorScheme.material.error,
                )
            }
            SleepRecordingControls(uiState = uiState, actions = actions)
        }
    }
}

@Composable
private fun sleepRecordingTitle(isRecording: Boolean): String = stringResource(
    if (isRecording) R.string.sleep_setup_active_title else R.string.sleep_setup_start_title,
)

@Composable
private fun sleepRecordingSubtitle(uiState: SleepSetupUiState): String = if (uiState.isRecording) {
    stringResource(
        R.string.sleep_setup_active_subtitle,
        DurationFormatter.formatClockDuration(uiState.sessionDurationMs),
    )
} else {
    stringResource(R.string.sleep_setup_start_subtitle)
}

@Composable
private fun SleepRecordingControls(uiState: SleepSetupUiState, actions: SleepSetupActions) {
    if (uiState.showMicDeniedPrompt) {
        SleepMicPermissionDeniedActions(actions)
    } else {
        SleepStartStopButton(isRecording = uiState.isRecording, actions = actions)
    }
}

@Composable
private fun SleepMicPermissionDeniedActions(actions: SleepSetupActions) {
    Column(verticalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space3)) {
        Text(
            text = stringResource(R.string.meter_microphone_permission_description),
            style = DbCheckTheme.typography.bodyMd,
            color = DbCheckTheme.colorScheme.material.error,
        )
        DbCheckButton(
            text = stringResource(R.string.action_open_settings),
            onClick = actions.onOpenMicSettings,
            modifier = Modifier.fillMaxWidth(),
        )
        DbCheckButton(
            text = stringResource(R.string.action_try_again),
            onClick = actions.onStartRecording,
            style = DbCheckButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SleepStartStopButton(isRecording: Boolean, actions: SleepSetupActions) {
    DbCheckButton(
        text = stringResource(
            if (isRecording) R.string.sleep_setup_stop_recording else R.string.sleep_setup_start_recording,
        ),
        onClick = if (isRecording) actions.onStopRecording else actions.onStartRecording,
        style = if (isRecording) DbCheckButtonStyle.Secondary else DbCheckButtonStyle.Primary,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SleepSetupNotesCard() {
    val spacing = DbCheckTheme.spacing
    val colors = DbCheckTheme.colorScheme

    DbCheckCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
            SleepSetupInfoRow(
                icon = Icons.Outlined.Info,
                title = stringResource(R.string.sleep_setup_ready_title),
                body = stringResource(R.string.sleep_setup_privacy_note),
                iconColor = colors.material.primary,
            )
            SleepSetupInfoRow(
                icon = Icons.Outlined.WarningAmber,
                body = stringResource(R.string.sleep_setup_battery_note),
                iconColor = colors.warning,
                bodyColor = colors.warning,
            )
        }
    }
}

@Composable
private fun SleepSetupInfoRow(
    icon: ImageVector,
    body: String,
    iconColor: Color,
    modifier: Modifier = Modifier,
    title: String? = null,
    bodyColor: Color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space3),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(DbCheckTheme.spacing.space5),
        )
        Column(verticalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space1)) {
            title?.let {
                Text(
                    text = it,
                    style = DbCheckTheme.typography.bodyLg,
                    color = DbCheckTheme.colorScheme.material.onSurface,
                )
            }
            Text(
                text = body,
                style = DbCheckTheme.typography.bodyMd,
                color = bodyColor,
            )
        }
    }
}

@Composable
private fun SleepSetupCardText(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space1)) {
        Text(
            text = title,
            style = DbCheckTheme.typography.headlineMd,
            color = DbCheckTheme.colorScheme.material.onSurface,
        )
        Text(
            text = subtitle,
            style = DbCheckTheme.typography.bodyMd,
            color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
        )
    }
}

@StringRes
private fun durationLabelStringRes(durationMinutes: Int): Int = when (durationMinutes) {
    360 -> R.string.sleep_setup_duration_6h
    480 -> R.string.sleep_setup_duration_8h
    600 -> R.string.sleep_setup_duration_10h
    else -> R.string.sleep_setup_duration_8h
}
