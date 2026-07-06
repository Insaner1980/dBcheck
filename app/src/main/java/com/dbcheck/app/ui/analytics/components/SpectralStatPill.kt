package com.dbcheck.app.ui.analytics.components

import androidx.annotation.StringRes
import com.dbcheck.app.R
import com.dbcheck.app.domain.audio.SpectralBandwidth
import com.dbcheck.app.ui.analytics.state.SpectralAnalysisUiState
import com.dbcheck.app.ui.analytics.state.SpectralBandUiState

internal data class SpectralStatPill(
    @param:StringRes val labelResId: Int,
    val value: String? = null,
    @param:StringRes val valueResId: Int? = null,
) {
    init {
        require(value != null || valueResId != null) { "Spectral stat pill requires a value" }
        require(value == null || valueResId == null) { "Spectral stat pill cannot use two value sources" }
    }
}

internal fun spectralStatPillsFor(state: SpectralAnalysisUiState): List<SpectralStatPill> = listOf(
        SpectralStatPill(
            labelResId = R.string.spectral_analysis_dominant,
            value = dominantFrequencyValue(state),
        ),
        bandwidthStatPill(state),
        SpectralStatPill(
            labelResId = R.string.spectral_stat_peak_band,
            value = peakBandValue(spectralBandsFor(state)),
        ),
        SpectralStatPill(
            labelResId = R.string.spectral_stat_status,
            valueResId = spectralStatusResId(state),
        ),
    )

internal fun spectralBandAmplitudesFor(state: SpectralAnalysisUiState): List<Float> =
    spectralBandsFor(state).map { it.normalizedAmplitude.coerceIn(0f, 1f) }

private fun dominantFrequencyValue(state: SpectralAnalysisUiState): String = when (state) {
        SpectralAnalysisUiState.Idle -> "--"
        SpectralAnalysisUiState.LockedPreview -> peakBandValue(PREVIEW_SPECTRAL_BANDS)
        is SpectralAnalysisUiState.Live -> formatSpectralFrequency(state.dominantFrequencyHz)
    }

private fun bandwidthStatPill(state: SpectralAnalysisUiState): SpectralStatPill = when (state) {
        SpectralAnalysisUiState.Idle ->
            SpectralStatPill(labelResId = R.string.spectral_analysis_bandwidth, value = "--")

        SpectralAnalysisUiState.LockedPreview ->
            SpectralStatPill(
                labelResId = R.string.spectral_analysis_bandwidth,
                valueResId = spectralBandwidthResId(SpectralBandwidth.WIDE),
            )

        is SpectralAnalysisUiState.Live ->
            spectralBandwidthResId(state.bandwidth)?.let { resId ->
                SpectralStatPill(labelResId = R.string.spectral_analysis_bandwidth, valueResId = resId)
            } ?: SpectralStatPill(labelResId = R.string.spectral_analysis_bandwidth, value = "--")
    }

private fun peakBandValue(bands: List<SpectralBandUiState>): String = bands
        .maxByOrNull { it.normalizedAmplitude }
        ?.centerFrequencyHz
        ?.takeIf { it > 0f }
        ?.let(::formatSpectralFrequency)
        ?: "--"

private fun spectralStatusResId(state: SpectralAnalysisUiState): Int = when (state) {
        SpectralAnalysisUiState.LockedPreview -> R.string.spectral_status_preview
        SpectralAnalysisUiState.Idle -> R.string.spectral_status_idle
        is SpectralAnalysisUiState.Live -> R.string.spectral_status_live
    }

internal fun spectralBandwidthResId(bandwidth: SpectralBandwidth): Int? = when (bandwidth) {
        SpectralBandwidth.UNKNOWN -> null
        SpectralBandwidth.NARROW -> R.string.spectral_bandwidth_narrow
        SpectralBandwidth.MEDIUM -> R.string.spectral_bandwidth_medium
        SpectralBandwidth.WIDE -> R.string.spectral_bandwidth_wide
    }

private fun spectralBandsFor(state: SpectralAnalysisUiState): List<SpectralBandUiState> = when (state) {
        SpectralAnalysisUiState.Idle -> List(PREVIEW_SPECTRAL_BANDS.size) {
            SpectralBandUiState(normalizedAmplitude = 0f)
        }

        SpectralAnalysisUiState.LockedPreview -> PREVIEW_SPECTRAL_BANDS

        is SpectralAnalysisUiState.Live -> state.bands
    }

private val PREVIEW_SPECTRAL_FREQUENCIES_HZ =
    listOf(
        24f,
        33f,
        46f,
        65f,
        91f,
        127f,
        178f,
        249f,
        349f,
        489f,
        685f,
        960f,
        1_345f,
        1_884f,
        2_400f,
        3_699f,
        5_184f,
        7_264f,
        10_177f,
        14_258f,
        19_975f,
        20_000f,
        20_000f,
        20_000f,
    )

private val PREVIEW_SPECTRAL_AMPLITUDES =
    listOf(
        0.12f,
        0.16f,
        0.2f,
        0.24f,
        0.28f,
        0.34f,
        0.42f,
        0.5f,
        0.58f,
        0.66f,
        0.74f,
        0.82f,
        0.9f,
        0.94f,
        0.98f,
        0.82f,
        0.64f,
        0.48f,
        0.36f,
        0.28f,
        0.22f,
        0.16f,
        0.12f,
        0.08f,
    )

private val PREVIEW_SPECTRAL_BANDS =
    PREVIEW_SPECTRAL_AMPLITUDES.mapIndexed { index, amplitude ->
        SpectralBandUiState(
            normalizedAmplitude = amplitude,
            centerFrequencyHz = PREVIEW_SPECTRAL_FREQUENCIES_HZ[index],
        )
    }
