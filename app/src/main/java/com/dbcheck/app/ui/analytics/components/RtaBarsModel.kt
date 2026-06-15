package com.dbcheck.app.ui.analytics.components

import com.dbcheck.app.R
import com.dbcheck.app.ui.analytics.state.RtaBandUiState
import com.dbcheck.app.ui.analytics.state.RtaUiState

internal data class RtaBarCanvasItem(
    val index: Int,
    val centerFrequencyHz: Float,
    val normalizedAmplitude: Float,
)

internal fun rtaBarsFor(state: RtaUiState): List<RtaBarCanvasItem> = when (state) {
    RtaUiState.Empty -> emptyList()
    RtaUiState.LockedPreview -> PREVIEW_RTA_BANDS
    is RtaUiState.Data -> state.bands.toCanvasItems()
}

internal fun rtaStatPillsFor(state: RtaUiState): List<SpectralStatPill> {
    val bars = rtaBarsFor(state)
    val peakBand = bars.maxByOrNull { it.normalizedAmplitude }
    return listOf(
        SpectralStatPill(
            labelResId = R.string.spectral_rta_peak,
            value = peakBand?.let { formatSpectralFrequency(it.centerFrequencyHz) } ?: "--",
        ),
        SpectralStatPill(
            labelResId = R.string.spectral_rta_bands,
            value = bars.takeIf { it.isNotEmpty() }?.size?.toString() ?: "--",
        ),
    )
}

private fun List<RtaBandUiState>.toCanvasItems(): List<RtaBarCanvasItem> =
    mapIndexed { index, band ->
        RtaBarCanvasItem(
            index = index,
            centerFrequencyHz = band.centerFrequencyHz,
            normalizedAmplitude = band.normalizedAmplitude.coerceIn(0f, 1f),
        )
    }

private val PREVIEW_RTA_BANDS =
    listOf(
        RtaBandUiState(centerFrequencyHz = 31.62f, normalizedAmplitude = 0.22f),
        RtaBandUiState(centerFrequencyHz = 63.10f, normalizedAmplitude = 0.28f),
        RtaBandUiState(centerFrequencyHz = 125.89f, normalizedAmplitude = 0.42f),
        RtaBandUiState(centerFrequencyHz = 251.19f, normalizedAmplitude = 0.64f),
        RtaBandUiState(centerFrequencyHz = 501.19f, normalizedAmplitude = 0.82f),
        RtaBandUiState(centerFrequencyHz = 1_000f, normalizedAmplitude = 0.96f),
        RtaBandUiState(centerFrequencyHz = 1_995.26f, normalizedAmplitude = 0.72f),
        RtaBandUiState(centerFrequencyHz = 3_981.07f, normalizedAmplitude = 0.54f),
        RtaBandUiState(centerFrequencyHz = 7_943.28f, normalizedAmplitude = 0.36f),
        RtaBandUiState(centerFrequencyHz = 15_848.93f, normalizedAmplitude = 0.24f),
    ).toCanvasItems()
