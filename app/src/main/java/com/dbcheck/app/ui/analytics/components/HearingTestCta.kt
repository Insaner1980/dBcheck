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
    modifier: Modifier = Modifier,
    isLocked: Boolean = false,
    onUpgradeClick: () -> Unit = {},
) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing

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
                Spacer(Modifier.height(spacing.space2))
                Text(
                    text = "Quick 3-minute assessment",
                    style = typography.bodyMd,
                    color = colors.material.onSurfaceVariant,
                )
                Spacer(Modifier.height(spacing.space4))
                DbCheckButton(
                    text = "Start Test \u2192",
                    onClick = {
                        runHearingTestCtaStartClick(
                            isLocked = isLocked,
                            onStartTest = onStartTest,
                            onUpgradeClick = onUpgradeClick,
                        )
                    },
                    style = DbCheckButtonStyle.Primary,
                    height = 44.dp,
                )
            }
        }
    }
}

internal fun runHearingTestCtaStartClick(isLocked: Boolean, onStartTest: () -> Unit, onUpgradeClick: () -> Unit) {
    if (isLocked) {
        onUpgradeClick()
    } else {
        onStartTest()
    }
}
