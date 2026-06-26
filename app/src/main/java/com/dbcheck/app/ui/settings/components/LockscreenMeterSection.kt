package com.dbcheck.app.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dbcheck.app.R
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.ProLockOverlay
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun LockscreenMeterSection(
    lockscreenMeterEnabled: Boolean,
    showLockscreenMeterPublicly: Boolean,
    isProUser: Boolean,
    onLockscreenMeterChange: (Boolean) -> Unit,
    onShowLockscreenMeterPubliclyChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onUpgradeClick: () -> Unit = {},
    showTitle: Boolean = true,
) {
    if (!showTitle) {
        ProLockOverlay(
            isLocked = !isProUser,
            onUpgradeClick = onUpgradeClick,
            modifier = modifier,
        ) {
            DbCheckCard(modifier = Modifier.fillMaxWidth()) {
                LockscreenMeterControls(
                    lockscreenMeterEnabled = lockscreenMeterEnabled,
                    showLockscreenMeterPublicly = showLockscreenMeterPublicly,
                    onLockscreenMeterChange = onLockscreenMeterChange,
                    onShowLockscreenMeterPubliclyChange = onShowLockscreenMeterPubliclyChange,
                )
            }
        }
        return
    }

    SettingsLockedCardSection(
        title = stringResource(R.string.lockscreen_meter_section_title),
        isLocked = !isProUser,
        onUpgradeClick = onUpgradeClick,
        modifier = modifier,
    ) {
        LockscreenMeterControls(
            lockscreenMeterEnabled = lockscreenMeterEnabled,
            showLockscreenMeterPublicly = showLockscreenMeterPublicly,
            onLockscreenMeterChange = onLockscreenMeterChange,
            onShowLockscreenMeterPubliclyChange = onShowLockscreenMeterPubliclyChange,
        )
    }
}

@Composable
private fun LockscreenMeterControls(
    lockscreenMeterEnabled: Boolean,
    showLockscreenMeterPublicly: Boolean,
    onLockscreenMeterChange: (Boolean) -> Unit,
    onShowLockscreenMeterPubliclyChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space3),
    ) {
        LockscreenMeterRow(
            lockscreenMeterEnabled = lockscreenMeterEnabled,
            onLockscreenMeterChange = onLockscreenMeterChange,
        )
        PublicLockscreenMeterRow(
            showLockscreenMeterPublicly = showLockscreenMeterPublicly,
            enabled = lockscreenMeterEnabled,
            onShowLockscreenMeterPubliclyChange = onShowLockscreenMeterPubliclyChange,
        )
        Text(
            text = stringResource(R.string.lockscreen_meter_public_warning),
            style = DbCheckTheme.typography.bodyMd,
            color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
        )
    }
}

@Composable
private fun LockscreenMeterRow(lockscreenMeterEnabled: Boolean, onLockscreenMeterChange: (Boolean) -> Unit) {
    SettingsToggleDescriptionRow(
        title = stringResource(R.string.lockscreen_meter_title),
        subtitle = stringResource(R.string.lockscreen_meter_subtitle),
        checked = lockscreenMeterEnabled,
        onCheckedChange = onLockscreenMeterChange,
    )
}

@Composable
private fun PublicLockscreenMeterRow(
    showLockscreenMeterPublicly: Boolean,
    enabled: Boolean,
    onShowLockscreenMeterPubliclyChange: (Boolean) -> Unit,
) {
    SettingsToggleDescriptionRow(
        title = stringResource(R.string.lockscreen_meter_public_title),
        subtitle = stringResource(R.string.lockscreen_meter_public_subtitle),
        checked = showLockscreenMeterPublicly,
        onCheckedChange = onShowLockscreenMeterPubliclyChange,
        enabled = enabled,
    )
}
