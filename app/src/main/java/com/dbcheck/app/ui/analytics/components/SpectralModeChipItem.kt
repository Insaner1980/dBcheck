package com.dbcheck.app.ui.analytics.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dbcheck.app.R
import com.dbcheck.app.ui.analytics.state.SpectralMode
import com.dbcheck.app.ui.theme.DbCheckTheme

internal data class SpectralModeChipItem(val mode: SpectralMode, val labelResId: Int, val isSelected: Boolean)

internal fun spectralModeChipItems(selectedMode: SpectralMode): List<SpectralModeChipItem> =
    enumValues<SpectralMode>().map { mode ->
        SpectralModeChipItem(
            mode = mode,
            labelResId = mode.labelResId,
            isSelected = mode == selectedMode,
        )
    }

@Composable
fun SpectralModeChipRow(
    selectedMode: SpectralMode,
    onModeSelect: (SpectralMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space2),
    ) {
        spectralModeChipItems(selectedMode).forEach { item ->
            val label = stringResource(item.labelResId)
            AnalyticsSelectableChip(
                text = label,
                selected = item.isSelected,
                locked = false,
                chipContentDescription =
                    spectralModeContentDescription(
                        label = label,
                        isSelected = item.isSelected,
                    ),
                onClick = { onModeSelect(item.mode) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun spectralModeContentDescription(label: String, isSelected: Boolean): String = if (isSelected) {
        stringResource(R.string.a11y_spectral_mode_selected, label)
    } else {
        stringResource(R.string.a11y_spectral_mode, label)
    }

private val SpectralMode.labelResId: Int
    get() =
        when (this) {
            SpectralMode.BARS -> R.string.spectral_mode_bars
            SpectralMode.SPECTROGRAM -> R.string.spectral_mode_spectrogram
            SpectralMode.RTA -> R.string.spectral_mode_rta
        }
