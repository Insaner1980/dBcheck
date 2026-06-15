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
        // Reset button
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.material.surfaceContainerHighest)
                    .clickable(onClick = onReset),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = stringResource(R.string.a11y_reset),
                tint = colors.material.onSurface,
                modifier = Modifier.size(24.dp),
            )
        }

        // Play/Pause FAB
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

        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.material.surfaceContainerHighest)
                    .clickable(onClick = onCameraOverlayClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoCamera,
                contentDescription =
                    if (isCameraOverlayEnabled) {
                        stringResource(R.string.a11y_open_camera_overlay)
                    } else {
                        stringResource(R.string.a11y_camera_overlay_locked)
                    },
                tint = colors.material.onSurface.copy(alpha = if (isCameraOverlayEnabled) 1f else 0.55f),
                modifier = Modifier.size(24.dp),
            )
        }

        // Share button
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.material.surfaceContainerHighest)
                    .clickable(enabled = isShareEnabled, onClick = onShare),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = stringResource(R.string.a11y_share),
                tint = colors.material.onSurface.copy(alpha = if (isShareEnabled) 1f else 0.4f),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
