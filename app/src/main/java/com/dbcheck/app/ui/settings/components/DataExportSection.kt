package com.dbcheck.app.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckCard
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
)

data class DataExportSectionActions(
    val onExportCsv: () -> Unit,
    val onCreateBackup: () -> Unit,
    val onRequestRestoreBackup: (LocalBackupUiState) -> Unit,
    val onConfirmRestoreBackup: () -> Unit,
    val onDismissRestoreBackup: () -> Unit,
    val onUpgradeClick: () -> Unit,
)

@Composable
fun DataExportSection(
    state: DataExportSectionState,
    actions: DataExportSectionActions,
    modifier: Modifier = Modifier,
) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "DATA & EXPORT",
            style = typography.labelMd,
            color = colors.material.onSurfaceVariant,
        )
        Spacer(Modifier.height(spacing.space3))

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
        state.restoreCandidate?.let { backup ->
            RestoreBackupDialog(
                backup = backup,
                isRestoring = state.isBackupRestoring,
                onConfirm = actions.onConfirmRestoreBackup,
                onDismiss = actions.onDismissRestoreBackup,
            )
        }
    }
}

@Composable
private fun DataExportCard(
    isCsvExporting: Boolean,
    onExportCsv: () -> Unit,
) {
    val spacing = DbCheckTheme.spacing

    DbCheckCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
            SettingsDescriptionRow(
                title = "CSV export",
                subtitle = "Share session names, tags, summaries, and raw readings as CSV files",
                leadingIcon = Icons.Outlined.FileDownload,
            )
            DbCheckButton(
                text = if (isCsvExporting) "Preparing..." else "Export CSV",
                onClick = onExportCsv,
                enabled = !isCsvExporting,
                style = DbCheckButtonStyle.Secondary,
                height = spacing.space12,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BackupSection(
    state: DataExportSectionState,
    actions: DataExportSectionActions,
) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing

    DbCheckCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
            SettingsDescriptionRow(
                title = "Local backups",
                subtitle = "Create an on-device copy of sessions, readings, and hearing test results",
                leadingIcon = Icons.Outlined.Backup,
            )

            DbCheckButton(
                text = if (state.isBackupCreating) "Creating..." else "Create backup",
                onClick = actions.onCreateBackup,
                enabled = !state.isBackupCreating && !state.isBackupRestoring,
                style = DbCheckButtonStyle.Secondary,
                height = spacing.space12,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.localBackups.isEmpty()) {
                Text(
                    text = "No local backups yet",
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
        subtitle = "${backup.fileName} · ${formatBackupSize(backup.sizeBytes)}",
        leadingIcon = Icons.Outlined.Restore,
        leadingIconTint = colors.material.onSurfaceVariant,
        titleStyle = typography.bodyMd.copy(fontWeight = FontWeight.SemiBold),
        subtitleStyle = typography.labelMd,
    ) {
        TextButton(
            onClick = { onRestore(backup) },
            enabled = restoreEnabled,
        ) {
            Text(if (isRestoring) "Restoring..." else "Restore")
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
    val colors = DbCheckTheme.colorScheme

    AlertDialog(
        onDismissRequest = {
            if (!isRestoring) {
                onDismiss()
            }
        },
        title = {
            Text("Restore backup?")
        },
        text = {
            Text(
                "This will replace current measurement history and hearing test results with ${backup.fileName}. " +
                    "A safety backup of the current database will be created first.",
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isRestoring,
            ) {
                Text(
                    text = if (isRestoring) "Restoring..." else "Restore",
                    color = colors.material.error,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isRestoring,
            ) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun DataExportMessage(
    text: String,
    isError: Boolean,
) {
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

private fun formatBackupDate(createdAtMillis: Long): String =
    DateFormat
        .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(createdAtMillis))

private fun formatBackupSize(sizeBytes: Long): String =
    when {
        sizeBytes >= BYTES_IN_MEBIBYTE ->
            String.format(Locale.getDefault(), "%.1f MB", sizeBytes.toDouble() / BYTES_IN_MEBIBYTE)

        sizeBytes >= BYTES_IN_KIBIBYTE ->
            String.format(Locale.getDefault(), "%.1f KB", sizeBytes.toDouble() / BYTES_IN_KIBIBYTE)

        else -> "$sizeBytes B"
    }

private const val BYTES_IN_KIBIBYTE = 1024
private const val BYTES_IN_MEBIBYTE = BYTES_IN_KIBIBYTE * 1024
