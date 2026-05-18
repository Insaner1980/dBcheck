package com.dbcheck.app.ui.settings.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.health.connect.client.PermissionController
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.DbCheckToggle
import com.dbcheck.app.ui.components.ProLockOverlay
import com.dbcheck.app.ui.settings.state.HealthConnectUiState
import com.dbcheck.app.ui.theme.DbCheckTheme

data class HealthSyncSectionState(
    val healthConnectEnabled: Boolean,
    val heartRateOverlayEnabled: Boolean,
    val isProUser: Boolean,
    val status: HealthConnectUiState,
    val healthConnectErrorMessage: String? = null,
) {
    val isNoiseSyncActive: Boolean
        get() = healthConnectEnabled && status.isAvailable && status.noiseSyncGranted

    val isHeartRateOverlayActive: Boolean
        get() = heartRateOverlayEnabled && status.isAvailable && status.heartRateReadGranted
}

data class HealthSyncSectionActions(
    val onHealthConnectChange: (Boolean) -> Unit,
    val onHeartRateOverlayChange: (Boolean) -> Unit,
    val onPermissionsChanged: () -> Unit,
    val onOpenHealthConnectInstall: () -> Unit,
    val onOpenHealthConnectManageData: () -> Unit,
    val onUpgradeClick: () -> Unit = {},
)

@Composable
fun HealthSyncSection(
    state: HealthSyncSectionState,
    actions: HealthSyncSectionActions,
    modifier: Modifier = Modifier,
) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing
    var pendingRequest by remember { mutableStateOf<HealthPermissionRequest?>(null) }
    val permissionsLauncher =
        rememberLauncherForActivityResult(
            contract = PermissionController.createRequestPermissionResultContract(),
        ) { granted ->
            handlePermissionResult(pendingRequest, granted, actions)
            pendingRequest = null
            actions.onPermissionsChanged()
        }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "HEALTH & SYNC",
            style = typography.labelMd,
            color = colors.material.onSurfaceVariant,
        )
        Spacer(Modifier.height(spacing.space3))

        HealthSyncCard(
            state = state,
            actions = actions,
            onPermissionRequest = { pendingRequest = it },
        )
        state.healthConnectErrorMessage?.let { message ->
            Spacer(Modifier.height(spacing.space2))
            Text(
                text = message,
                style = typography.bodyMd,
                color = colors.material.error,
            )
        }
    }

    pendingRequest?.let { request ->
        HealthPermissionDialog(
            request = request,
            onConfirm = { permissionsLauncher.launch(request.permissions) },
            onDismiss = { pendingRequest = null },
        )
    }
}

@Composable
private fun HealthSyncCard(
    state: HealthSyncSectionState,
    actions: HealthSyncSectionActions,
    onPermissionRequest: (HealthPermissionRequest) -> Unit,
) {
    val spacing = DbCheckTheme.spacing

    DbCheckCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
            HealthConnectStatusRow(
                status = state.status,
                onOpenHealthConnectInstall = actions.onOpenHealthConnectInstall,
                onOpenHealthConnectManageData = actions.onOpenHealthConnectManageData,
            )
            NoiseSyncToggle(
                state = state,
                actions = actions,
                onPermissionRequest = onPermissionRequest,
            )
            HeartRateOverlayToggle(
                state = state,
                actions = actions,
                onPermissionRequest = onPermissionRequest,
            )
        }
    }
}

@Composable
private fun NoiseSyncToggle(
    state: HealthSyncSectionState,
    actions: HealthSyncSectionActions,
    onPermissionRequest: (HealthPermissionRequest) -> Unit,
) {
    HealthToggleRow(
        title = "Sync to Health Connect",
        subtitle = "Save completed noise exposure sessions",
        checked = state.isNoiseSyncActive,
        enabled = state.status.isAvailable,
        onCheckedChange = { enabled ->
            when {
                !enabled -> actions.onHealthConnectChange(false)
                state.status.noiseSyncGranted -> actions.onHealthConnectChange(true)
                else -> onPermissionRequest(state.status.noiseSyncRequest())
            }
        },
    )
}

@Composable
private fun HeartRateOverlayToggle(
    state: HealthSyncSectionState,
    actions: HealthSyncSectionActions,
    onPermissionRequest: (HealthPermissionRequest) -> Unit,
) {
    ProLockOverlay(
        isLocked = !state.isProUser,
        onUpgradeClick = actions.onUpgradeClick,
    ) {
        HealthToggleRow(
            title = "Heart rate overlay",
            subtitle = "Read Health Connect heart rate samples",
            checked = state.isHeartRateOverlayActive,
            enabled = state.status.isAvailable,
            onCheckedChange = { enabled ->
                when {
                    !enabled -> actions.onHeartRateOverlayChange(false)
                    state.status.heartRateReadGranted -> actions.onHeartRateOverlayChange(true)
                    else -> onPermissionRequest(state.status.heartRateOverlayRequest())
                }
            },
        )
    }
}

@Composable
private fun HealthPermissionDialog(
    request: HealthPermissionRequest,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                tint = colors.material.primary,
            )
        },
        title = { Text("Health Connect permissions", style = typography.headlineMd) },
        text = {
            Text(
                text = request.rationale,
                style = typography.bodyMd,
                color = colors.material.onSurfaceVariant,
            )
        },
    )
}

private fun handlePermissionResult(
    request: HealthPermissionRequest?,
    granted: Set<String>,
    actions: HealthSyncSectionActions,
) {
    when (request?.target) {
        HealthPermissionTarget.NOISE_SYNC -> {
            if (granted.containsAll(request.permissions)) {
                actions.onHealthConnectChange(true)
            }
        }

        HealthPermissionTarget.HEART_RATE_OVERLAY -> {
            if (granted.containsAll(request.permissions)) {
                actions.onHeartRateOverlayChange(true)
            }
        }

        null -> Unit
    }
}

@Composable
private fun HealthConnectStatusRow(
    status: HealthConnectUiState,
    onOpenHealthConnectInstall: () -> Unit,
    onOpenHealthConnectManageData: () -> Unit,
) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
            Text("Health Connect", style = typography.bodyLg, color = colors.material.onSurface)
            Text(status.label, style = typography.bodyMd, color = colors.material.onSurfaceVariant)
        }

        if (status.requiresInstall) {
            DbCheckButton(
                text = "Install",
                onClick = onOpenHealthConnectInstall,
                style = DbCheckButtonStyle.Secondary,
                height = spacing.space10,
            )
        } else if (status.isAvailable) {
            DbCheckButton(
                text = "Manage",
                onClick = onOpenHealthConnectManageData,
                style = DbCheckButtonStyle.Secondary,
                height = spacing.space10,
            )
        }
    }
}

@Composable
private fun HealthToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingsDescriptionRow(
        title = title,
        subtitle = subtitle,
    ) {
        DbCheckToggle(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
        )
    }
}

private data class HealthPermissionRequest(
    val target: HealthPermissionTarget,
    val permissions: Set<String>,
    val rationale: String,
)

private enum class HealthPermissionTarget {
    NOISE_SYNC,
    HEART_RATE_OVERLAY,
}

private fun HealthConnectUiState.noiseSyncRequest(): HealthPermissionRequest =
    HealthPermissionRequest(
        target = HealthPermissionTarget.NOISE_SYNC,
        permissions = noiseSyncPermissions,
        rationale =
            "dBcheck writes completed noise exposure sessions as Health Connect exercise entries " +
                "because Health Connect does not provide a native noise exposure data type.",
    )

private fun HealthConnectUiState.heartRateOverlayRequest(): HealthPermissionRequest =
    HealthPermissionRequest(
        target = HealthPermissionTarget.HEART_RATE_OVERLAY,
        permissions = heartRateReadPermissions,
        rationale =
            "dBcheck reads heart rate samples only for the selected measurement window so Pro analytics " +
                "can compare pulse response with noise exposure.",
    )
