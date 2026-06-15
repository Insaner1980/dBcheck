package com.dbcheck.app.ui.analytics.components

import com.dbcheck.app.ui.analytics.state.SpectralBandUiState
import com.dbcheck.app.ui.analytics.state.SpectrogramRowUiState
import com.dbcheck.app.ui.analytics.state.SpectrogramUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpectrogramCanvasModelTest {
    @Test
    fun liveSpectrogramUsesBufferedRows() {
        val rows = listOf(row(timestamp = 1L, amplitude = 0.25f), row(timestamp = 2L, amplitude = 0.75f))

        assertEquals(rows, spectrogramRowsFor(SpectrogramUiState.Data(rows)))
    }

    @Test
    fun lockedSpectrogramUsesStablePreviewRows() {
        val rows = spectrogramRowsFor(SpectrogramUiState.LockedPreview)

        assertEquals(12, rows.size)
        assertTrue(rows.all { it.bands.size == 24 })
        assertTrue(rows.flatMap { it.bands }.any { it.normalizedAmplitude > 0f })
    }

    @Test
    fun emptySpectrogramHasNoRows() {
        assertEquals(emptyList<SpectrogramRowUiState>(), spectrogramRowsFor(SpectrogramUiState.Empty))
    }

    @Test
    fun spectrogramCellsPreserveRowColumnAndAmplitude() {
        val rows = listOf(row(timestamp = 1L, amplitude = -0.5f), row(timestamp = 2L, amplitude = 1.5f))

        val cells = spectrogramCanvasCells(rows)

        assertEquals(48, cells.size)
        assertEquals(SpectrogramCanvasCell(rowIndex = 0, columnIndex = 0, normalizedAmplitude = 0f), cells.first())
        assertEquals(SpectrogramCanvasCell(rowIndex = 1, columnIndex = 23, normalizedAmplitude = 1f), cells.last())
    }

    private fun row(timestamp: Long, amplitude: Float): SpectrogramRowUiState =
        SpectrogramRowUiState(
            timestampMs = timestamp,
            bands = List(24) { SpectralBandUiState(normalizedAmplitude = amplitude) },
        )
}
