package com.dbcheck.app.ui.analytics.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dbcheck.app.R
import com.dbcheck.app.domain.audio.SpectralBandwidth
import com.dbcheck.app.ui.analytics.state.RtaUiState
import com.dbcheck.app.ui.analytics.state.SpectralAnalysisUiState
import com.dbcheck.app.ui.analytics.state.SpectralMode
import com.dbcheck.app.ui.analytics.state.SpectrogramRowUiState
import com.dbcheck.app.ui.analytics.state.SpectrogramUiState
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.ProLockOverlay
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
internal fun SpectralAnalysisCard(
    state: SpectralAnalysisCardState,
    modifier: Modifier = Modifier,
    actions: SpectralAnalysisCardActions = SpectralAnalysisCardActions(),
) {
    ProLockOverlay(
        isLocked = state.isLocked,
        onUpgradeClick = actions.onUpgradeClick,
        modifier = modifier,
    ) {
        val visibleState =
            if (state.isLocked) {
                SpectralAnalysisUiState.LockedPreview
            } else {
                state.spectralState
            }
        val visibleSpectrogramState =
            if (state.isLocked) {
                SpectrogramUiState.LockedPreview
            } else {
                state.spectrogramState
            }
        val visibleRtaState =
            if (state.isLocked) {
                RtaUiState.LockedPreview
            } else {
                state.rtaState
            }
        SpectralContent(
            visibleState = visibleState,
            spectrogramState = visibleSpectrogramState,
            rtaState = visibleRtaState,
            selectedMode = state.selectedMode,
            onModeSelect = actions.onModeSelect,
        )
    }
}

internal data class SpectralAnalysisCardState(
    val spectralState: SpectralAnalysisUiState,
    val selectedMode: SpectralMode,
    val isLocked: Boolean,
    val spectrogramState: SpectrogramUiState = SpectrogramUiState.Empty,
    val rtaState: RtaUiState = RtaUiState.Empty,
)

internal data class SpectralAnalysisCardActions(
    val onModeSelect: (SpectralMode) -> Unit = {},
    val onUpgradeClick: () -> Unit = {},
)

@Composable
private fun SpectralContent(
    visibleState: SpectralAnalysisUiState,
    spectrogramState: SpectrogramUiState,
    rtaState: RtaUiState,
    selectedMode: SpectralMode,
    onModeSelect: (SpectralMode) -> Unit,
) {
    DbCheckCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SpectralHeader(visibleState)

            Spacer(Modifier.height(16.dp))

            SpectralModeChipRow(
                selectedMode = selectedMode,
                onModeSelect = onModeSelect,
            )

            Spacer(Modifier.height(16.dp))

            when (selectedMode) {
                SpectralMode.BARS -> {
                    SpectralBars(
                        barHeights = barHeightsFor(visibleState),
                        contentDescription = spectralBarsContentDescription(visibleState),
                    )
                    FrequencyLabels()
                }

                SpectralMode.SPECTROGRAM -> {
                    SpectrogramWaterfall(
                        rows = spectrogramRowsFor(spectrogramState),
                        contentDescription = spectrogramContentDescription(spectrogramState),
                    )
                    FrequencyLabels()
                }

                SpectralMode.RTA -> {
                    RtaBars(
                        bars = rtaBarsFor(rtaState),
                        contentDescription = rtaBarsContentDescription(rtaState),
                    )

                    Spacer(Modifier.height(12.dp))

                    SpectralStatPillRows(pills = rtaStatPillsFor(rtaState))
                }
            }

            Spacer(Modifier.height(16.dp))

            SpectralStatPillRows(pills = spectralStatPillsFor(visibleState))
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
private fun SpectrogramWaterfall(rows: List<SpectrogramRowUiState>, contentDescription: String) {
    val colors = DbCheckTheme.colorScheme.material
    val lowColor = colors.primary.copy(alpha = 0.14f)
    val highColor = colors.tertiary.copy(alpha = 0.86f)
    val backgroundColor = colors.surfaceContainerHighest.copy(alpha = 0.46f)

    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .semantics {
                    this.contentDescription = contentDescription
                },
    ) {
        drawRoundRect(
            color = backgroundColor,
            size = size,
            cornerRadius = CornerRadius(8f, 8f),
        )

        if (rows.isEmpty()) return@Canvas

        val columnCount = rows.maxOf { it.bands.size }.coerceAtLeast(1)
        val rowCount = rows.size.coerceAtLeast(1)
        val gap = 1f
        val cellWidth = ((size.width - gap * (columnCount - 1)) / columnCount).coerceAtLeast(0f)
        val cellHeight = ((size.height - gap * (rowCount - 1)) / rowCount).coerceAtLeast(0f)

        spectrogramCanvasCells(rows).forEach { cell ->
            drawRect(
                color = lerp(lowColor, highColor, cell.normalizedAmplitude),
                topLeft =
                    Offset(
                        x = cell.columnIndex * (cellWidth + gap),
                        y = cell.rowIndex * (cellHeight + gap),
                    ),
                size = Size(cellWidth, cellHeight),
            )
        }
    }
}

@Composable
private fun RtaBars(bars: List<RtaBarCanvasItem>, contentDescription: String) {
    val colors = DbCheckTheme.colorScheme.material
    val lowColor = colors.primary.copy(alpha = 0.24f)
    val highColor = colors.secondary.copy(alpha = 0.92f)
    val backgroundColor = colors.surfaceContainerHighest.copy(alpha = 0.46f)

    Column {
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .semantics {
                        this.contentDescription = contentDescription
                    },
        ) {
            drawRoundRect(
                color = backgroundColor,
                size = size,
                cornerRadius = CornerRadius(8f, 8f),
            )

            if (bars.isEmpty()) return@Canvas

            val gap = 4f
            val barWidth = ((size.width - gap * (bars.size - 1)) / bars.size).coerceAtLeast(0f)
            bars.forEach { bar ->
                val barHeight = (bar.normalizedAmplitude * size.height).coerceIn(0f, size.height)
                drawRoundRect(
                    color = lerp(lowColor, highColor, bar.normalizedAmplitude),
                    topLeft =
                        Offset(
                            x = bar.index * (barWidth + gap),
                            y = size.height - barHeight,
                        ),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(5f, 5f),
                )
            }
        }

        RtaFrequencyLabels(bars)
    }
}

@Composable
private fun RtaFrequencyLabels(bars: List<RtaBarCanvasItem>) {
    val typography = DbCheckTheme.typography
    val labelColor = DbCheckTheme.colorScheme.material.onSurfaceVariant
    val labels =
        if (bars.isEmpty()) {
            listOf("--", "--", "--")
        } else {
            val oneKilohertzBand = bars.minBy { kotlin.math.abs(it.centerFrequencyHz - 1_000f) }
            listOf(
                formatSpectralFrequency(bars.first().centerFrequencyHz),
                formatSpectralFrequency(oneKilohertzBand.centerFrequencyHz),
                formatSpectralFrequency(bars.last().centerFrequencyHz),
            )
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        labels.forEach { label ->
            Text(label, style = typography.labelSm, color = labelColor)
        }
    }
}

@Composable
private fun SpectralStatPillRows(pills: List<SpectralStatPill>) {
    Column(verticalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space2)) {
        pills.chunked(STAT_PILLS_PER_ROW).forEach { rowPills ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space2),
            ) {
                rowPills.forEach { pill ->
                    SpectralStatPillView(
                        pill = pill,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowPills.size < STAT_PILLS_PER_ROW) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SpectralStatPillView(pill: SpectralStatPill, modifier: Modifier = Modifier) {
    val colors = DbCheckTheme.colorScheme.material
    val typography = DbCheckTheme.typography
    val value = pill.value ?: stringResource(pill.valueResId ?: error("Missing spectral stat value"))

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceContainerHigh)
                .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column {
            Text(
                text = stringResource(pill.labelResId),
                style = typography.labelSm,
                color = colors.onSurfaceVariant,
            )
            Text(
                text = value,
                style = typography.labelMd,
                color = colors.onSurface,
            )
        }
    }
}

private fun barHeightsFor(state: SpectralAnalysisUiState): List<Float> = spectralBandAmplitudesFor(state)

@Composable
private fun spectralBarsContentDescription(state: SpectralAnalysisUiState): String = when (state) {
    SpectralAnalysisUiState.Idle -> stringResource(R.string.a11y_spectral_analysis_bars_idle)

    SpectralAnalysisUiState.LockedPreview -> stringResource(R.string.a11y_spectral_analysis_bars_locked)

    is SpectralAnalysisUiState.Live -> {
        val resources = LocalResources.current
        resources.getQuantityString(
            R.plurals.a11y_spectral_analysis_bars_live,
            state.bands.size,
            formatSpectralFrequency(state.dominantFrequencyHz),
            formatBandwidth(state.bandwidth),
            state.bands.size,
        )
    }
}

@Composable
private fun spectrogramContentDescription(state: SpectrogramUiState): String = when (state) {
    SpectrogramUiState.Empty -> stringResource(R.string.a11y_spectrogram_idle)

    SpectrogramUiState.LockedPreview -> stringResource(R.string.a11y_spectrogram_locked)

    is SpectrogramUiState.Data -> {
        val rowCount = state.rows.size
        if (rowCount <= 0) {
            stringResource(R.string.a11y_spectrogram_idle)
        } else {
            val bandCount = state.rows.maxOf { it.bands.size }
            LocalResources.current.getQuantityString(
                R.plurals.a11y_spectrogram_live,
                rowCount,
                rowCount,
                bandCount,
            )
        }
    }
}

@Composable
private fun rtaBarsContentDescription(state: RtaUiState): String = when (state) {
    RtaUiState.Empty -> stringResource(R.string.a11y_rta_bars_idle)

    RtaUiState.LockedPreview -> stringResource(R.string.a11y_rta_bars_locked)

    is RtaUiState.Data -> {
        val bars = rtaBarsFor(state)
        if (bars.isEmpty()) {
            stringResource(R.string.a11y_rta_bars_idle)
        } else {
            val peakBand = bars.maxBy { it.normalizedAmplitude }
            LocalResources.current.getQuantityString(
                R.plurals.a11y_rta_bars_live,
                bars.size,
                bars.size,
                formatSpectralFrequency(peakBand.centerFrequencyHz),
            )
        }
    }
}

@Composable
private fun formatBandwidth(bandwidth: SpectralBandwidth): String = when (bandwidth) {
        SpectralBandwidth.UNKNOWN -> "--"
        else -> stringResource(spectralBandwidthResId(bandwidth) ?: error("Missing spectral bandwidth label"))
    }

private const val STAT_PILLS_PER_ROW = 2
