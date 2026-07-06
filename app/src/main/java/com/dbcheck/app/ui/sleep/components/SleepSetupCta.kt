package com.dbcheck.app.ui.sleep.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dbcheck.app.R
import com.dbcheck.app.ui.components.DbCheckLockedCtaCard

@Composable
fun SleepSetupCta(
    onOpenSleepSetup: () -> Unit,
    modifier: Modifier = Modifier,
    isLocked: Boolean = false,
    onUpgradeClick: () -> Unit = {},
) {
    DbCheckLockedCtaCard(
        title = stringResource(R.string.sleep_setup_cta_title),
        subtitle = stringResource(R.string.sleep_setup_cta_subtitle),
        buttonText = stringResource(R.string.sleep_setup_action),
        onClick = onOpenSleepSetup,
        onUpgradeClick = onUpgradeClick,
        modifier = modifier,
        isLocked = isLocked,
    )
}
