package com.dbcheck.app.ui.analytics.state

import com.dbcheck.app.domain.audio.SpectralFrame

internal class SpectrogramBuffer(private val maxRows: Int = DEFAULT_MAX_ROWS) {
    private var rows: List<SpectrogramRowUiState> = emptyList()

    init {
        require(maxRows > 0) { "maxRows must be positive" }
    }

    fun update(isProUser: Boolean, frame: SpectralFrame?): SpectrogramUiState =
        when {
            !isProUser -> {
                clear()
                SpectrogramUiState.LockedPreview
            }

            frame == null -> {
                clear()
                SpectrogramUiState.Empty
            }

            else -> {
                append(frame)
                SpectrogramUiState.Data(rows = rows)
            }
        }

    private fun append(frame: SpectralFrame) {
        if (rows.lastOrNull()?.timestampMs == frame.timestamp) return

        rows = (rows + frame.toSpectrogramRow()).takeLast(maxRows)
    }

    private fun clear() {
        rows = emptyList()
    }

    private fun SpectralFrame.toSpectrogramRow(): SpectrogramRowUiState =
        SpectrogramRowUiState(
            timestampMs = timestamp,
            bands =
                bands.map { band ->
                    SpectralBandUiState(
                        normalizedAmplitude = band.normalizedAmplitude,
                        centerFrequencyHz = band.centerFrequencyHz,
                    )
                },
        )

    companion object {
        const val DEFAULT_MAX_ROWS = 60
    }
}
