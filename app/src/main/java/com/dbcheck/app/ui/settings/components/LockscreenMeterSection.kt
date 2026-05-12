package com.dbcheck.app.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.DbCheckToggle
import com.dbcheck.app.ui.components.ProLockOverlay
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun LockscreenMeterSection(
    lockscreenMeterEnabled: Boolean,
    isProUser: Boolean,
    onLockscreenMeterChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onUpgradeClick: () -> Unit = {},
) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "LOCK SCREEN METER",
            style = typography.labelMd,
            color = colors.material.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        ProLockOverlay(
            isLocked = !isProUser,
            onUpgradeClick = onUpgradeClick,
        ) {
            DbCheckCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Live Lock Screen Meter", style = typography.bodyLg, color = colors.material.onSurface)
                        Text(
                            "Show current, peak and duration",
                            style = typography.bodyMd,
                            color = colors.material.onSurfaceVariant,
                        )
                    }
                    DbCheckToggle(
                        checked = lockscreenMeterEnabled,
                        onCheckedChange = onLockscreenMeterChange,
                    )
                }
            }
        }
    }
}
