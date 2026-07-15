package com.dbcheck.app.ui.meter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dbcheck.app.R
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun MeterControls(state: MeterControlsState, actions: MeterControlsActions, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space6),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MeterSideControlButton(
            imageVector = Icons.Outlined.Refresh,
            contentDescription = stringResource(R.string.a11y_reset),
            onClick = actions.onReset,
        )

        MeterRecordingButton(isRecording = state.isRecording, onClick = actions.onToggleRecording)
        MeterCameraControlButton(isEnabled = state.isCameraOverlayEnabled, onClick = actions.onCameraOverlayClick)
        MeterShareControlButton(isEnabled = state.isShareEnabled, onClick = actions.onShare)
    }
}

@Composable
private fun MeterRecordingButton(isRecording: Boolean, onClick: () -> Unit) {
    val colors = DbCheckTheme.colorScheme
    Box(
        modifier =
            Modifier
                .size(80.dp)
                .clip(CircleShape)
                .then(
                    if (isRecording) {
                        Modifier.background(colors.material.error)
                    } else {
                        Modifier.background(brush = colors.signatureGradient)
                    },
                ).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isRecording) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription =
                stringResource(if (isRecording) R.string.action_pause else R.string.action_play),
            tint = if (isRecording) colors.material.onError else colors.material.onPrimary,
            modifier = Modifier.size(36.dp),
        )
    }
}

@Composable
private fun MeterCameraControlButton(isEnabled: Boolean, onClick: () -> Unit) {
    MeterSideControlButton(
        imageVector = Icons.Outlined.PhotoCamera,
        contentDescription =
            stringResource(
                if (isEnabled) R.string.a11y_open_camera_overlay else R.string.a11y_camera_overlay_locked,
            ),
        onClick = onClick,
        alpha = if (isEnabled) 1f else 0.55f,
    )
}

@Composable
private fun MeterShareControlButton(isEnabled: Boolean, onClick: () -> Unit) {
    val colors = DbCheckTheme.colorScheme
    MeterSideControlButton(
        imageVector = Icons.Outlined.Share,
        contentDescription = stringResource(R.string.a11y_share),
        onClick = onClick,
        enabled = isEnabled,
        contentColor = if (isEnabled) colors.material.onSurface else colors.material.onSurfaceVariant,
    )
}

data class MeterControlsState(
    val isRecording: Boolean,
    val isShareEnabled: Boolean = true,
    val isCameraOverlayEnabled: Boolean = false,
)

data class MeterControlsActions(
    val onToggleRecording: () -> Unit,
    val onReset: () -> Unit,
    val onShare: () -> Unit,
    val onCameraOverlayClick: () -> Unit = {},
)

@Composable
private fun MeterSideControlButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    alpha: Float = 1f,
    contentColor: Color? = null,
) {
    val colors = DbCheckTheme.colorScheme
    val effectiveContentColor = contentColor ?: colors.material.onSurface
    Box(
        modifier =
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.material.surfaceContainerHighest)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = effectiveContentColor.copy(alpha = alpha),
            modifier = Modifier.size(24.dp),
        )
    }
}
