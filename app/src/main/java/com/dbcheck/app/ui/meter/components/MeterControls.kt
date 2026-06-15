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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dbcheck.app.R
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun MeterControls(
    isRecording: Boolean,
    onToggleRecording: () -> Unit,
    onReset: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    isShareEnabled: Boolean = true,
    onCameraOverlayClick: () -> Unit = {},
    isCameraOverlayEnabled: Boolean = false,
) {
    val colors = DbCheckTheme.colorScheme

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space6),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MeterSideControlButton(
            imageVector = Icons.Outlined.Refresh,
            contentDescription = stringResource(R.string.a11y_reset),
            onClick = onReset,
        )

        Box(
            modifier =
                Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(brush = colors.signatureGradient)
                    .clickable(onClick = onToggleRecording),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription =
                    if (isRecording) {
                        stringResource(R.string.action_pause)
                    } else {
                        stringResource(R.string.action_play)
                    },
                tint = colors.material.onPrimary,
                modifier = Modifier.size(36.dp),
            )
        }

        MeterSideControlButton(
            imageVector = Icons.Outlined.PhotoCamera,
            contentDescription =
                if (isCameraOverlayEnabled) {
                    stringResource(R.string.a11y_open_camera_overlay)
                } else {
                    stringResource(R.string.a11y_camera_overlay_locked)
                },
            onClick = onCameraOverlayClick,
            alpha = if (isCameraOverlayEnabled) 1f else 0.55f,
        )

        MeterSideControlButton(
            imageVector = Icons.Outlined.Share,
            contentDescription = stringResource(R.string.a11y_share),
            onClick = onShare,
            enabled = isShareEnabled,
            alpha = if (isShareEnabled) 1f else 0.4f,
        )
    }
}

@Composable
private fun MeterSideControlButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    alpha: Float = 1f,
) {
    val colors = DbCheckTheme.colorScheme
    Box(
        modifier =
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.material.surfaceContainerHighest)
                .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = colors.material.onSurface.copy(alpha = alpha),
            modifier = Modifier.size(24.dp),
        )
    }
}
