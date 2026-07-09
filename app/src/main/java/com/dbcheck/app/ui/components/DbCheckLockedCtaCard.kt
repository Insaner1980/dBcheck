package com.dbcheck.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun DbCheckLockedCtaCard(
    title: String,
    subtitle: String,
    buttonText: String,
    onClick: () -> Unit,
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
                    text = title,
                    style = typography.headlineMd,
                    color = colors.material.onSurface,
                )
                Spacer(Modifier.height(spacing.space2))
                Text(
                    text = subtitle,
                    style = typography.bodyMd,
                    color = colors.material.onSurfaceVariant,
                )
                Spacer(Modifier.height(spacing.space4))
                DbCheckButton(
                    text = buttonText,
                    onClick = onClick,
                    style = DbCheckButtonStyle.Primary,
                    height = spacing.space12,
                )
            }
        }
    }
}
