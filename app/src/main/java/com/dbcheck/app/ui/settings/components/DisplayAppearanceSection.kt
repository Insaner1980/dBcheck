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
import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.data.local.preferences.model.WaveformStyle
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.DbCheckChip
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun DisplayAppearanceSection(
    themeMode: String,
    waveformStyle: WaveformStyle,
    refreshRate: MeterRefreshRate,
    onThemeModeChange: (String) -> Unit,
    onWaveformStyleChange: (WaveformStyle) -> Unit,
    onRefreshRateChange: (MeterRefreshRate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "\uD83C\uDFA8 DISPLAY & APPEARANCE",
            style = typography.labelMd,
            color = colors.material.onSurfaceVariant,
        )
        Spacer(Modifier.height(spacing.space3))

        DbCheckCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
                SettingsChipGroup(label = "Dark Mode") {
                    listOf("system" to "System", "dark" to "Dark", "light" to "Light").forEach { (value, label) ->
                        DbCheckChip(
                            text = label,
                            selected = themeMode == value,
                            onClick = { onThemeModeChange(value) },
                        )
                    }
                }

                SettingsChipGroup(label = "Waveform Style") {
                    WaveformStyle.entries.forEach { style ->
                        DbCheckChip(
                            text = style.displayName,
                            selected = waveformStyle == style,
                            onClick = { onWaveformStyleChange(style) },
                        )
                    }
                }

                SettingsChipGroup(
                    label = "Refresh Rate",
                    helperText =
                        "Lower rates reduce screen updates only, not microphone sampling or saved measurement cadence.",
                ) {
                    MeterRefreshRate.entries.forEach { rate ->
                        DbCheckChip(
                            text = rate.displayName,
                            selected = refreshRate == rate,
                            onClick = { onRefreshRateChange(rate) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsChipGroup(
    label: String,
    helperText: String? = null,
    chips: @Composable () -> Unit,
) {
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
