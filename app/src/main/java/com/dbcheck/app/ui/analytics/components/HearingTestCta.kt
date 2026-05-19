package com.dbcheck.app.ui.analytics.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dbcheck.app.R
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.ProLockOverlay
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun HearingTestCta(
    onStartTest: () -> Unit,
    modifier: Modifier = Modifier,
    isLocked: Boolean = false,
    onUpgradeClick: () -> Unit = {},
) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme

    ProLockOverlay(
        isLocked = isLocked,
        onUpgradeClick = onUpgradeClick,
        modifier = modifier,
    ) {
        DbCheckCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.hearing_test_cta_title),
                    style = typography.headlineMd,
                    color = colors.material.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.hearing_test_cta_subtitle),
                    style = typography.bodyMd,
                    color = colors.material.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                DbCheckButton(
                    text = stringResource(R.string.action_start_test_arrow),
                    onClick = onStartTest,
                    style = DbCheckButtonStyle.Primary,
                    height = 44.dp,
                )
            }
        }
    }
}
