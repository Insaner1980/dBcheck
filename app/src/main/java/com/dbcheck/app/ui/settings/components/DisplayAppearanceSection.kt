package com.dbcheck.app.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.DbCheckChip
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun DisplayAppearanceSection(
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "\uD83C\uDFA8 DISPLAY & APPEARANCE",
            style = typography.labelMd,
            color = colors.material.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        DbCheckCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("Dark Mode", style = typography.bodyLg, color = colors.material.onSurface)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("system" to "System", "dark" to "Dark", "light" to "Light").forEach { (value, label) ->
                            DbCheckChip(
                                text = label,
                                selected = themeMode == value,
                                onClick = { onThemeModeChange(value) },
                            )
                        }
                    }
                }

                SettingsRow(label = "Waveform Style")
                SettingsRow(label = "Refresh Rate")
            }
        }
    }
}

@Composable
private fun SettingsRow(label: String) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = typography.bodyLg, color = colors.material.onSurface)
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.material.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
    }
}
