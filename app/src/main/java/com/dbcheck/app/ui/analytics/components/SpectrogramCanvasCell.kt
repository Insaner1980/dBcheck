package com.dbcheck.app.ui.analytics.components

import com.dbcheck.app.ui.analytics.state.SpectralBandUiState
import com.dbcheck.app.ui.analytics.state.SpectrogramRowUiState
import com.dbcheck.app.ui.analytics.state.SpectrogramUiState

internal data class SpectrogramCanvasCell(val rowIndex: Int, val columnIndex: Int, val normalizedAmplitude: Float)

internal fun spectrogramRowsFor(state: SpectrogramUiState): List<SpectrogramRowUiState> = when (state) {
        SpectrogramUiState.Empty -> emptyList()
        SpectrogramUiState.LockedPreview -> SPECTROGRAM_PREVIEW_ROWS
        is SpectrogramUiState.Data -> state.rows
    }

internal fun spectrogramCanvasCells(rows: List<SpectrogramRowUiState>): List<SpectrogramCanvasCell> =
    rows.flatMapIndexed { rowIndex, row ->
        row.bands.mapIndexed { columnIndex, band ->
            SpectrogramCanvasCell(
                rowIndex = rowIndex,
                columnIndex = columnIndex,
                normalizedAmplitude = band.normalizedAmplitude.coerceIn(0f, 1f),
            )
        }
    }

private const val PREVIEW_SPECTROGRAM_ROW_COUNT = 12
private const val PREVIEW_SPECTROGRAM_BAND_COUNT = 24

private val PREVIEW_SPECTROGRAM_AMPLITUDES =
    listOf(
        0.12f,
        0.22f,
        0.36f,
        0.58f,
        0.82f,
        0.68f,
        0.44f,
        0.28f,
    )

private val SPECTROGRAM_PREVIEW_ROWS: List<SpectrogramRowUiState> =
    List(PREVIEW_SPECTROGRAM_ROW_COUNT) { rowIndex ->
        SpectrogramRowUiState(
            timestampMs = rowIndex.toLong(),
            bands =
                List(PREVIEW_SPECTROGRAM_BAND_COUNT) { bandIndex ->
                    SpectralBandUiState(
                        normalizedAmplitude =
                            PREVIEW_SPECTROGRAM_AMPLITUDES[
                                (rowIndex + bandIndex) % PREVIEW_SPECTROGRAM_AMPLITUDES.size,
                            ],
                    )
                },
        )
    }
