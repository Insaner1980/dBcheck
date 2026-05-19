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
import androidx.compose.ui.res.stringResource
import com.dbcheck.app.R
import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.data.local.preferences.model.ThemeMode
import com.dbcheck.app.data.local.preferences.model.WaveformStyle
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.DbCheckChip
import com.dbcheck.app.ui.theme.DbCheckTheme
import com.dbcheck.app.util.displayNameStringRes

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
            text = stringResource(R.string.display_appearance_title),
            style = typography.labelMd,
            color = colors.material.onSurfaceVariant,
        )
        Spacer(Modifier.height(spacing.space3))

        DbCheckCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
                SettingsChipGroup(label = stringResource(R.string.display_dark_mode)) {
                    ThemeMode.entries.forEach { mode ->
                        DbCheckChip(
                            text = stringResource(mode.displayNameStringRes()),
                            selected = themeMode == mode.preferenceValue,
                            onClick = { onThemeModeChange(mode.preferenceValue) },
                        )
                    }
                }

                SettingsChipGroup(label = stringResource(R.string.display_waveform_style)) {
                    WaveformStyle.entries.forEach { style ->
                        DbCheckChip(
                            text = stringResource(style.displayNameStringRes()),
                            selected = waveformStyle == style,
                            onClick = { onWaveformStyleChange(style) },
                        )
                    }
                }

                SettingsChipGroup(
                    label = stringResource(R.string.display_refresh_rate),
                    helperText = stringResource(R.string.display_refresh_rate_helper),
                ) {
                    MeterRefreshRate.entries.forEach { rate ->
                        DbCheckChip(
                            text = stringResource(rate.displayNameStringRes()),
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
private fun SettingsChipGroup(label: String, helperText: String? = null, chips: @Composable () -> Unit) {
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
