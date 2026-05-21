package com.dbcheck.app.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.ProLockOverlay
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
internal fun SettingsDescriptionRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    leadingIcon: SettingsDescriptionIcon? = null,
    titleStyle: TextStyle = DbCheckTheme.typography.bodyLg,
    subtitleStyle: TextStyle = DbCheckTheme.typography.bodyMd,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingIcon?.let { leading ->
            Icon(
                imageVector = leading.icon,
                contentDescription = null,
                tint = leading.tint ?: colors.material.primary,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
            Text(title, style = titleStyle, color = colors.material.onSurface)
            Text(subtitle, style = subtitleStyle, color = colors.material.onSurfaceVariant)
        }
        trailingContent?.invoke()
    }
}

internal data class SettingsDescriptionIcon(val icon: ImageVector, val tint: Color? = null)

@Composable
internal fun SettingsLockedCardSection(
    title: String,
    isLocked: Boolean,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = typography.labelMd,
            color = colors.material.onSurfaceVariant,
        )
        Spacer(Modifier.height(spacing.space3))

        ProLockOverlay(
            isLocked = isLocked,
            onUpgradeClick = onUpgradeClick,
        ) {
            DbCheckCard(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}
