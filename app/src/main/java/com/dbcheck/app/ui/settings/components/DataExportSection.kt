package com.dbcheck.app.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.dbcheck.app.R
import com.dbcheck.app.ui.components.DbCheckAlertDialog
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckToggle
import com.dbcheck.app.ui.components.ProLockOverlay
import com.dbcheck.app.ui.settings.state.LocalBackupUiState
import com.dbcheck.app.ui.theme.DbCheckTheme
import java.text.DateFormat
import java.util.Date
import java.util.Locale

data class DataExportSectionState(
    val isProUser: Boolean,
    val isCsvExporting: Boolean,
    val csvExportMessage: String?,
    val csvExportErrorMessage: String?,
    val localBackups: List<LocalBackupUiState>,
    val isBackupCreating: Boolean,
    val isBackupRestoring: Boolean,
    val restoreCandidate: LocalBackupUiState?,
    val backupMessage: String?,
    val backupErrorMessage: String?,
    val wavRecordingDefaultEnabled: Boolean,
    val clearHistoryConfirmationVisible: Boolean,
    val isHistoryClearing: Boolean,
    val historyClearMessage: String?,
    val historyClearErrorMessage: String?,
    val coarseLocationPermissionGranted: Boolean,
    val coarseLocationPermissionDenied: Boolean = false,
)

data class DataExportSectionActions(
    val onExportCsv: () -> Unit,
    val onCreateBackup: () -> Unit,
    val onRequestRestoreBackup: (LocalBackupUiState) -> Unit,
    val onConfirmRestoreBackup: () -> Unit,
    val onDismissRestoreBackup: () -> Unit,
    val onWavRecordingDefaultChange: (Boolean) -> Unit,
    val onRequestClearHistory: () -> Unit,
    val onConfirmClearHistory: () -> Unit,
    val onDismissClearHistory: () -> Unit,
    val onRequestLocationPermission: () -> Unit,
    val onOpenLocationSettings: () -> Unit = {},
    val onUpgradeClick: () -> Unit,
)

@Composable
fun DataExportSection(
    state: DataExportSectionState,
    actions: DataExportSectionActions,
    modifier: Modifier = Modifier,
) {
    val spacing = DbCheckTheme.spacing

    Column(modifier = modifier.fillMaxWidth()) {
        SettingsSectionHeader(title = stringResource(R.string.settings_data_export_title))

        ProLockOverlay(
            isLocked = !state.isProUser,
            onUpgradeClick = actions.onUpgradeClick,
        ) {
            DataExportCard(
                isCsvExporting = state.isCsvExporting,
                onExportCsv = actions.onExportCsv,
            )
        }
        state.csvExportMessage?.let { message ->
            Spacer(Modifier.height(spacing.space3))
            DataExportMessage(text = message, isError = false)
        }
        state.csvExportErrorMessage?.let { message ->
            Spacer(Modifier.height(spacing.space3))
            DataExportMessage(text = message, isError = true)
        }
        Spacer(Modifier.height(spacing.space4))
        ProLockOverlay(
            isLocked = !state.isProUser,
            onUpgradeClick = actions.onUpgradeClick,
        ) {
            WavRecordingDefaultCard(
                enabled = state.wavRecordingDefaultEnabled,
                onEnabledChange = actions.onWavRecordingDefaultChange,
            )
        }
        Spacer(Modifier.height(spacing.space4))
        SessionLocationPermissionCard(
            permissionGranted = state.coarseLocationPermissionGranted,
            permissionDenied = state.coarseLocationPermissionDenied,
            onRequestPermission = actions.onRequestLocationPermission,
            onOpenSettings = actions.onOpenLocationSettings,
        )
        Spacer(Modifier.height(spacing.space4))
        BackupSection(
            state = state,
            actions = actions,
        )
        state.backupMessage?.let { message ->
            Spacer(Modifier.height(spacing.space3))
            DataExportMessage(text = message, isError = false)
        }
        state.backupErrorMessage?.let { message ->
            Spacer(Modifier.height(spacing.space3))
            DataExportMessage(text = message, isError = true)
        }
        Spacer(Modifier.height(spacing.space4))
        ClearHistoryCard(
            isClearing = state.isHistoryClearing,
            onRequestClearHistory = actions.onRequestClearHistory,
        )
        state.historyClearMessage?.let { message ->
            Spacer(Modifier.height(spacing.space3))
            DataExportMessage(text = message, isError = false)
        }
        state.historyClearErrorMessage?.let { message ->
            Spacer(Modifier.height(spacing.space3))
            DataExportMessage(text = message, isError = true)
        }
        DataExportDialogs(state = state, actions = actions)
    }
}

@Composable
private fun SessionLocationPermissionCard(
    permissionGranted: Boolean,
    permissionDenied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    SettingsActionCard(
        title = stringResource(R.string.settings_session_location_title),
        subtitle = stringResource(R.string.settings_session_location_subtitle),
        leadingIcon = SettingsDescriptionIcon(Icons.Outlined.LocationOn),
        buttonText =
            stringResource(
                if (permissionGranted) {
                    R.string.settings_session_location_enabled
                } else {
                    R.string.settings_session_location_allow
                },
            ),
        onClick = onRequestPermission,
        enabled = !permissionGranted,
        secondaryAction =
            if (permissionDenied) {
                SettingsCardAction(
                    text = stringResource(R.string.action_open_settings),
                    onClick = onOpenSettings,
                )
            } else {
                null
            },
    )
}

@Composable
private fun DataExportDialogs(state: DataExportSectionState, actions: DataExportSectionActions) {
    state.restoreCandidate?.let { backup ->
        RestoreBackupDialog(
            backup = backup,
            isRestoring = state.isBackupRestoring,
            onConfirm = actions.onConfirmRestoreBackup,
            onDismiss = actions.onDismissRestoreBackup,
        )
    }
    if (state.clearHistoryConfirmationVisible) {
        ClearHistoryDialog(
            isClearing = state.isHistoryClearing,
            onConfirm = actions.onConfirmClearHistory,
            onDismiss = actions.onDismissClearHistory,
        )
    }
}

@Composable
private fun WavRecordingDefaultCard(enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    val spacing = DbCheckTheme.spacing

    SettingsCardColumn(spacing = spacing.space3) {
        SettingsDescriptionRow(
            title = stringResource(R.string.settings_wav_recording_title),
            subtitle = stringResource(R.string.settings_wav_recording_subtitle),
            leadingIcon = SettingsDescriptionIcon(Icons.Outlined.GraphicEq),
        ) {
            DbCheckToggle(
                checked = enabled,
                onCheckedChange = onEnabledChange,
            )
        }
        CompactDisclosureInfo(
            fullText = stringResource(R.string.settings_wav_recording_privacy_warning),
            compactLabel = stringResource(R.string.settings_wav_recording_privacy_compact),
            dialogTitle = stringResource(R.string.settings_wav_recording_title),
            showFullInline = enabled,
        )
    }
}

@Composable
private fun DataExportCard(isCsvExporting: Boolean, onExportCsv: () -> Unit) {
    SettingsActionCard(
        title = stringResource(R.string.settings_export_csv_title),
        subtitle = stringResource(R.string.settings_export_csv_subtitle),
        leadingIcon = SettingsDescriptionIcon(Icons.Outlined.FileDownload),
        buttonText =
            if (isCsvExporting) {
                stringResource(R.string.action_preparing)
            } else {
                stringResource(R.string.action_export_csv)
            },
        onClick = onExportCsv,
        enabled = !isCsvExporting,
    )
}

@Composable
private fun BackupSection(state: DataExportSectionState, actions: DataExportSectionActions) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing

    SettingsCardColumn {
        SettingsDescriptionRow(
            title = stringResource(R.string.settings_local_backups_title),
            subtitle = stringResource(R.string.settings_local_backups_subtitle),
            leadingIcon = SettingsDescriptionIcon(Icons.Outlined.Backup),
        )

        DbCheckButton(
            text =
                if (state.isBackupCreating) {
                    stringResource(R.string.action_creating)
                } else {
                    stringResource(R.string.action_create_backup)
                },
            onClick = actions.onCreateBackup,
            enabled = !state.isBackupCreating && !state.isBackupRestoring,
            style = DbCheckButtonStyle.Secondary,
            height = spacing.space12,
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.localBackups.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_local_backups_empty),
                style = typography.bodyMd,
                color = colors.material.onSurfaceVariant,
            )
        } else {
            BackupList(
                backups = state.localBackups,
                restoreCandidate = state.restoreCandidate,
                isRestoring = state.isBackupRestoring,
                onRestore = actions.onRequestRestoreBackup,
            )
        }
    }
}

@Composable
private fun ClearHistoryCard(isClearing: Boolean, onRequestClearHistory: () -> Unit) {
    SettingsActionCard(
        title = stringResource(R.string.settings_clear_history_title),
        subtitle = stringResource(R.string.settings_clear_history_subtitle),
        leadingIcon = SettingsDescriptionIcon(Icons.Outlined.Delete),
        buttonText =
            if (isClearing) {
                stringResource(R.string.action_deleting)
            } else {
                stringResource(R.string.action_clear_history)
            },
        onClick = onRequestClearHistory,
        enabled = !isClearing,
    )
}

@Composable
private fun BackupList(
    backups: List<LocalBackupUiState>,
    restoreCandidate: LocalBackupUiState?,
    isRestoring: Boolean,
    onRestore: (LocalBackupUiState) -> Unit,
) {
    val spacing = DbCheckTheme.spacing

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
        backups.forEachIndexed { index, backup ->
            if (index > 0) {
                HorizontalDivider(color = DbCheckTheme.colorScheme.material.outlineVariant)
            }
            BackupRow(
                backup = backup,
                isRestoring = isRestoring && restoreCandidate?.filePath == backup.filePath,
                restoreEnabled = !isRestoring,
                onRestore = onRestore,
            )
        }
    }
}

@Composable
private fun BackupRow(
    backup: LocalBackupUiState,
    isRestoring: Boolean,
    restoreEnabled: Boolean,
    onRestore: (LocalBackupUiState) -> Unit,
) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme

    SettingsDescriptionRow(
        title = formatBackupDate(backup.createdAtMillis),
        subtitle = "${backup.displayName} · ${formatBackupSize(backup.sizeBytes)}",
        leadingIcon =
            SettingsDescriptionIcon(
                icon = Icons.Outlined.Restore,
                tint = colors.material.onSurfaceVariant,
            ),
        titleStyle = typography.bodyMd.copy(fontWeight = FontWeight.SemiBold),
        subtitleStyle = typography.labelMd,
    ) {
        TextButton(
            onClick = { onRestore(backup) },
            enabled = restoreEnabled,
        ) {
            Text(
                if (isRestoring) {
                    stringResource(R.string.action_restoring)
                } else {
                    stringResource(R.string.action_restore)
                },
            )
        }
    }
}

@Composable
private fun RestoreBackupDialog(
    backup: LocalBackupUiState,
    isRestoring: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    DbCheckAlertDialog(
        title = stringResource(R.string.settings_local_backups_restore_dialog_title),
        body = stringResource(R.string.settings_local_backups_restore_dialog_message, backup.displayName),
        confirmText =
            if (isRestoring) {
                stringResource(R.string.action_restoring)
            } else {
                stringResource(R.string.action_restore)
            },
        onConfirm = onConfirm,
        onDismiss = {
            if (!isRestoring) {
                onDismiss()
            }
        },
        dismissText = stringResource(R.string.action_cancel),
        onDismissClick = onDismiss,
        confirmEnabled = !isRestoring,
        dismissEnabled = !isRestoring,
        icon = Icons.Outlined.Restore,
    )
}

@Composable
private fun ClearHistoryDialog(isClearing: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    DbCheckAlertDialog(
        title = stringResource(R.string.settings_clear_history_dialog_title),
        body = stringResource(R.string.settings_clear_history_dialog_message),
        confirmText =
            if (isClearing) {
                stringResource(R.string.action_deleting)
            } else {
                stringResource(R.string.action_clear_history)
            },
        onConfirm = onConfirm,
        onDismiss = {
            if (!isClearing) {
                onDismiss()
            }
        },
        dismissText = stringResource(R.string.action_cancel),
        onDismissClick = onDismiss,
        confirmEnabled = !isClearing,
        dismissEnabled = !isClearing,
        icon = Icons.Outlined.Delete,
    )
}

@Composable
private fun DataExportMessage(text: String, isError: Boolean) {
    Text(
        text = text,
        style = DbCheckTheme.typography.bodyMd,
        color =
            if (isError) {
                DbCheckTheme.colorScheme.material.error
            } else {
                DbCheckTheme.colorScheme.success
            },
    )
}

private fun formatBackupDate(createdAtMillis: Long): String = DateFormat
        .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(createdAtMillis))

private fun formatBackupSize(sizeBytes: Long): String = when {
        sizeBytes >= BYTES_IN_MEBIBYTE ->
            String.format(Locale.getDefault(), "%.1f MB", sizeBytes.toDouble() / BYTES_IN_MEBIBYTE)

        sizeBytes >= BYTES_IN_KIBIBYTE ->
            String.format(Locale.getDefault(), "%.1f KB", sizeBytes.toDouble() / BYTES_IN_KIBIBYTE)

        else -> "$sizeBytes B"
    }

private const val BYTES_IN_KIBIBYTE = 1024
private const val BYTES_IN_MEBIBYTE = BYTES_IN_KIBIBYTE * 1024
