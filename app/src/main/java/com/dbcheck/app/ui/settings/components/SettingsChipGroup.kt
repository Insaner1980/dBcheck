package com.dbcheck.app.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
internal fun SettingsChipGroup(label: String, helperText: String? = null, chips: @Composable () -> Unit) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = typography.bodyLg, color = colors.material.onSurface)
        helperText?.let {
            Spacer(Modifier.height(spacing.space1))
            Text(it, style = typography.bodyMd, color = colors.material.onSurfaceVariant)
        }
        Spacer(Modifier.height(spacing.space2))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.space2),
            verticalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            chips()
        }
    }
}
