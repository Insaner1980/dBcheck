package com.dbcheck.app.ui.components

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun DbCheckToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = DbCheckTheme.colorScheme

    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedTrackColor = colors.material.primary,
            checkedThumbColor = colors.surfaceContainerLowest,
            uncheckedTrackColor = colors.material.surfaceContainerHighest,
            uncheckedThumbColor = colors.material.onSurfaceVariant,
        ),
    )
}
