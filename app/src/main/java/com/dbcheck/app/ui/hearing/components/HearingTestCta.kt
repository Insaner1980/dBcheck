package com.dbcheck.app.ui.hearing.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dbcheck.app.R
import com.dbcheck.app.ui.components.DbCheckLockedCtaCard

@Composable
fun HearingTestCta(
    onStartTest: () -> Unit,
    modifier: Modifier = Modifier,
    isLocked: Boolean = false,
    onUpgradeClick: () -> Unit = {},
) {
    DbCheckLockedCtaCard(
        title = stringResource(R.string.hearing_test_cta_title),
        subtitle = stringResource(R.string.hearing_test_cta_subtitle),
        buttonText = stringResource(R.string.action_start_test_arrow),
        onClick = onStartTest,
        onUpgradeClick = onUpgradeClick,
        modifier = modifier,
        isLocked = isLocked,
    )
}
