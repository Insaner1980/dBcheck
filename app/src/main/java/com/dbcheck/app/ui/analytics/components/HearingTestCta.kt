package com.dbcheck.app.ui.analytics.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.ProLockOverlay
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun HearingTestCta(
    onStartTest: () -> Unit,
    isLocked: Boolean = false,
    onUpgradeClick: () -> Unit = {},
    modifier: Modifier = Modifier,
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
                    text = "\uD83C\uDFA7 Check Your Hearing",
                    style = typography.headlineMd,
                    color = colors.material.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Quick 3-minute assessment",
                    style = typography.bodyMd,
                    color = colors.material.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                DbCheckButton(
                    text = "Start Test \u2192",
                    onClick = onStartTest,
                    style = DbCheckButtonStyle.Primary,
                    height = 44.dp,
                )
            }
        }
    }
}
