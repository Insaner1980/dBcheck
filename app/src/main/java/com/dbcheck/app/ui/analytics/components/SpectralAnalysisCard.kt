package com.dbcheck.app.ui.analytics.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dbcheck.app.R
import com.dbcheck.app.domain.audio.SpectralBandwidth
import com.dbcheck.app.ui.analytics.state.SpectralAnalysisUiState
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.ProLockOverlay
import com.dbcheck.app.ui.theme.DbCheckTheme
import java.util.Locale

@Composable
fun SpectralAnalysisCard(
    spectralState: SpectralAnalysisUiState,
    isLocked: Boolean,
    modifier: Modifier = Modifier,
    onUpgradeClick: () -> Unit = {},
) {
    ProLockOverlay(
        isLocked = isLocked,
        onUpgradeClick = onUpgradeClick,
        modifier = modifier,
    ) {
        val visibleState =
            if (isLocked) {
                SpectralAnalysisUiState.LockedPreview
            } else {
                spectralState
            }
        SpectralContent(visibleState)
    }
}

@Composable
private fun SpectralContent(visibleState: SpectralAnalysisUiState) {
    DbCheckCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SpectralHeader(visibleState)

            Spacer(Modifier.height(16.dp))

            SpectralBars(
                barHeights = barHeightsFor(visibleState),
                contentDescription = spectralBarsContentDescription(visibleState),
            )
            FrequencyLabels()

            Spacer(Modifier.height(16.dp))

            SpectralMetrics(visibleState)
        }
    }
}

@Composable
private fun SpectralHeader(visibleState: SpectralAnalysisUiState) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme
    val statusLabel =
        if (visibleState is SpectralAnalysisUiState.Live) {
            stringResource(R.string.spectral_analysis_live_capture)
        } else {
            stringResource(R.string.spectral_analysis_waiting)
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.spectral_analysis_title),
            style = typography.labelMd,
            color = colors.material.onSurfaceVariant,
        )
        Text(
            text = statusLabel,
            style = typography.labelMd,
            color = colors.material.primary,
        )
    }
}

@Composable
private fun SpectralBars(barHeights: List<Float>, contentDescription: String) {
    val barColor = DbCheckTheme.colorScheme.material.primary.copy(alpha = 0.7f)

    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .semantics {
                    this.contentDescription = contentDescription
                },
    ) {
        val gap = 3f
        val barWidth = (size.width - gap * (barHeights.size - 1)) / barHeights.size

        barHeights.forEachIndexed { index, height ->
            val barH = height * size.height
            drawRoundRect(
                color = barColor,
                topLeft = Offset(index * (barWidth + gap), size.height - barH),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(4f, 4f),
            )
        }
    }
}

@Composable
private fun FrequencyLabels() {
    val typography = DbCheckTheme.typography
    val labelColor = DbCheckTheme.colorScheme.material.onSurfaceVariant

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.unit_20_hz), style = typography.labelSm, color = labelColor)
        Text(stringResource(R.string.unit_1_khz), style = typography.labelSm, color = labelColor)
        Text(stringResource(R.string.unit_20_khz), style = typography.labelSm, color = labelColor)
    }
}

@Composable
private fun SpectralMetrics(visibleState: SpectralAnalysisUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        SpectralMetric(
            label = stringResource(R.string.spectral_analysis_dominant),
            value = dominantFrequencyLabel(visibleState),
        )
        SpectralMetric(
            label = stringResource(R.string.spectral_analysis_bandwidth),
            value = bandwidthLabel(visibleState),
        )
    }
}

@Composable
private fun SpectralMetric(label: String, value: String) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme

    Column {
        Text(label, style = typography.labelMd, color = colors.material.onSurfaceVariant)
        Text(value, style = typography.dataLg, color = colors.material.onSurface)
    }
}

private fun barHeightsFor(state: SpectralAnalysisUiState): List<Float> = when (state) {
        SpectralAnalysisUiState.Idle -> List(PREVIEW_BAR_COUNT) { 0f }
        SpectralAnalysisUiState.LockedPreview -> PREVIEW_BAR_HEIGHTS
        is SpectralAnalysisUiState.Live -> state.bands.map { it.normalizedAmplitude }
    }

private fun dominantFrequencyLabel(state: SpectralAnalysisUiState): String = when (state) {
        SpectralAnalysisUiState.LockedPreview -> "2.4 kHz"
        SpectralAnalysisUiState.Idle -> "--"
        is SpectralAnalysisUiState.Live -> formatFrequency(state.dominantFrequencyHz)
    }

@Composable
private fun bandwidthLabel(state: SpectralAnalysisUiState): String = when (state) {
        SpectralAnalysisUiState.LockedPreview -> stringResource(R.string.spectral_bandwidth_wide)
        SpectralAnalysisUiState.Idle -> "--"
        is SpectralAnalysisUiState.Live -> formatBandwidth(state.bandwidth)
    }

@Composable
private fun spectralBarsContentDescription(state: SpectralAnalysisUiState): String = when (state) {
    SpectralAnalysisUiState.Idle -> stringResource(R.string.a11y_spectral_analysis_bars_idle)

    SpectralAnalysisUiState.LockedPreview -> stringResource(R.string.a11y_spectral_analysis_bars_locked)

    is SpectralAnalysisUiState.Live ->
        stringResource(
            R.string.a11y_spectral_analysis_bars_live,
            formatFrequency(state.dominantFrequencyHz),
            formatBandwidth(state.bandwidth),
            state.bands.size,
        )
}

private fun formatFrequency(frequencyHz: Float): String = when {
        frequencyHz <= 0f -> "--"
        frequencyHz >= 1000f -> String.format(Locale.getDefault(), "%.1f kHz", frequencyHz / 1000f)
        else -> "${frequencyHz.toInt()} Hz"
    }

@Composable
private fun formatBandwidth(bandwidth: SpectralBandwidth): String = when (bandwidth) {
        SpectralBandwidth.UNKNOWN -> "--"
        SpectralBandwidth.NARROW -> stringResource(R.string.spectral_bandwidth_narrow)
        SpectralBandwidth.MEDIUM -> stringResource(R.string.spectral_bandwidth_medium)
        SpectralBandwidth.WIDE -> stringResource(R.string.spectral_bandwidth_wide)
    }

private const val PREVIEW_BAR_COUNT = 24

private val PREVIEW_BAR_HEIGHTS =
    listOf(
        0.18f,
        0.22f,
        0.28f,
        0.34f,
        0.48f,
        0.66f,
        0.78f,
        0.86f,
        0.8f,
        0.68f,
        0.54f,
        0.45f,
        0.38f,
        0.34f,
        0.31f,
        0.28f,
        0.25f,
        0.21f,
        0.18f,
        0.16f,
        0.14f,
        0.12f,
        0.1f,
        0.08f,
    )
