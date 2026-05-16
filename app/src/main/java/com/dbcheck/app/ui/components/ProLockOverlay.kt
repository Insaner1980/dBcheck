package com.dbcheck.app.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dbcheck.app.ui.theme.DbCheckOpacity
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun ProLockOverlay(
    isLocked: Boolean,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (!isLocked) {
        content()
        return
    }

    val colors = DbCheckTheme.colorScheme
    val shapes = DbCheckTheme.shapes
    val spacing = DbCheckTheme.spacing

    Box(modifier = modifier) {
        // Dimmed/blurred content preview
        Box(
            modifier =
                Modifier
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.blur(4.dp)
                        } else {
                            Modifier.alpha(DbCheckOpacity.DIMMED_PREVIEW)
                        },
                    ),
        ) {
            content()
        }

        // Lock overlay
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(shapes.extraLarge)
                    .background(colors.material.surface.copy(alpha = DbCheckOpacity.OVERLAY_SURFACE))
                    .padding(spacing.space6),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = colors.material.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = "Unlock with dBcheck Pro",
                style = DbCheckTheme.typography.bodyMd,
                color = colors.material.onSurfaceVariant,
                modifier = Modifier.padding(top = spacing.space2, bottom = spacing.space4),
            )
            DbCheckButton(
                text = "Upgrade",
                onClick = onUpgradeClick,
                style = DbCheckButtonStyle.Primary,
                height = DbCheckButtonDefaults.SmallHeight,
            )
        }
    }
}
