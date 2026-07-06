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

data class LockscreenMeterSectionState(
    val lockscreenMeterEnabled: Boolean,
    val showLockscreenMeterPublicly: Boolean,
    val isProUser: Boolean,
)

data class LockscreenMeterSectionActions(
    val onLockscreenMeterChange: (Boolean) -> Unit,
    val onShowLockscreenMeterPubliclyChange: (Boolean) -> Unit,
    val onUpgradeClick: () -> Unit = {},
)

@Composable
fun LockscreenMeterSection(
    state: LockscreenMeterSectionState,
    actions: LockscreenMeterSectionActions,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
) {
    if (!showTitle) {
        ProLockOverlay(
            isLocked = !state.isProUser,
            onUpgradeClick = actions.onUpgradeClick,
            modifier = modifier,
        ) {
            DbCheckCard(modifier = Modifier.fillMaxWidth()) {
                LockscreenMeterControls(
                    lockscreenMeterEnabled = state.lockscreenMeterEnabled,
                    showLockscreenMeterPublicly = state.showLockscreenMeterPublicly,
                    onLockscreenMeterChange = actions.onLockscreenMeterChange,
                    onShowLockscreenMeterPubliclyChange = actions.onShowLockscreenMeterPubliclyChange,
                )
            }
        }
        return
    }

    SettingsLockedCardSection(
        title = stringResource(R.string.lockscreen_meter_section_title),
        isLocked = !state.isProUser,
        onUpgradeClick = actions.onUpgradeClick,
        modifier = modifier,
    ) {
        LockscreenMeterControls(
            lockscreenMeterEnabled = state.lockscreenMeterEnabled,
            showLockscreenMeterPublicly = state.showLockscreenMeterPublicly,
            onLockscreenMeterChange = actions.onLockscreenMeterChange,
            onShowLockscreenMeterPubliclyChange = actions.onShowLockscreenMeterPubliclyChange,
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
