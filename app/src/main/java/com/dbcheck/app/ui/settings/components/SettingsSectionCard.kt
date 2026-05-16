package com.dbcheck.app.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.ProLockOverlay
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
internal fun SettingsSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    contentSpacing: Dp? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    SettingsSectionContainer(title = title, modifier = modifier) {
        DbCheckCard(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionContent(contentSpacing = contentSpacing, content = content)
        }
    }
}

@Composable
internal fun SettingsActionRow(
    modifier: Modifier = Modifier,
    leading: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
    trailing: @Composable RowScope.() -> Unit,
) {
    val spacing = DbCheckTheme.spacing

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke(this)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
            content()
        }
        trailing()
    }
}

@Composable
internal fun LockedSettingsSectionCard(
    title: String,
    isLocked: Boolean,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentSpacing: Dp? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    SettingsSectionContainer(title = title, modifier = modifier) {
        ProLockOverlay(isLocked = isLocked, onUpgradeClick = onUpgradeClick) {
            DbCheckCard(modifier = Modifier.fillMaxWidth()) {
                SettingsSectionContent(contentSpacing = contentSpacing, content = content)
            }
        }
    }
}

@Composable
private fun SettingsSectionContainer(
    title: String,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing
    val typography = DbCheckTheme.typography

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = typography.labelMd,
            color = colors.material.onSurfaceVariant,
        )
        Spacer(Modifier.height(spacing.space3))
        content()
    }
}

@Composable
private fun SettingsSectionContent(
    contentSpacing: Dp?,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = DbCheckTheme.spacing

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(contentSpacing ?: spacing.space3),
        content = content,
    )
}
