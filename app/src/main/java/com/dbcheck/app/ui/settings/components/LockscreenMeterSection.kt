package com.dbcheck.app.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dbcheck.app.R
import com.dbcheck.app.ui.components.DbCheckToggle
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

    SettingsLockedCardSection(
        title = stringResource(R.string.lockscreen_meter_section_title),
        isLocked = !isProUser,
        onUpgradeClick = onUpgradeClick,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.lockscreen_meter_title),
                    style = typography.bodyLg,
                    color = colors.material.onSurface,
                )
                Text(
                    stringResource(R.string.lockscreen_meter_subtitle),
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
