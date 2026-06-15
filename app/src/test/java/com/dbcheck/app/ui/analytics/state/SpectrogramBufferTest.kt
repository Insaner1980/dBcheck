package com.dbcheck.app.ui.analytics.state

import com.dbcheck.app.domain.audio.SpectralBand
import com.dbcheck.app.domain.audio.SpectralBandwidth
import com.dbcheck.app.domain.audio.SpectralFrame
import org.junit.Assert.assertEquals
import org.junit.Test

class SpectrogramBufferTest {
    @Test
    fun appendsSpectralFrameAsWaterfallRow() {
        val buffer = SpectrogramBuffer(maxRows = 3)

        val state = buffer.update(isProUser = true, frame = frame(timestamp = 10L, amplitude = 0.25f))

        val data = state as SpectrogramUiState.Data
        assertEquals(10L, data.rows.single().timestampMs)
        assertEquals(24, data.rows.single().bands.size)
        assertEquals(0.25f, data.rows.single().bands.first().normalizedAmplitude, 0.001f)
    }

    @Test
    fun trimsOldRowsToConfiguredMaximum() {
        val buffer = SpectrogramBuffer(maxRows = 3)

        listOf(1L, 2L, 3L, 4L).forEach { timestamp ->
            buffer.update(isProUser = true, frame = frame(timestamp = timestamp))
        }

        val data = buffer.update(isProUser = true, frame = frame(timestamp = 5L)) as SpectrogramUiState.Data
        assertEquals(listOf(3L, 4L, 5L), data.rows.map { it.timestampMs })
    }

    @Test
    fun ignoresDuplicateFrameTimestampFromStateRebuilds() {
        val buffer = SpectrogramBuffer(maxRows = 3)
        val frame = frame(timestamp = 10L)

        buffer.update(isProUser = true, frame = frame)
        val data = buffer.update(isProUser = true, frame = frame) as SpectrogramUiState.Data

        assertEquals(listOf(10L), data.rows.map { it.timestampMs })
    }

    @Test
    fun lockedOrMissingFrameClearsRows() {
        val buffer = SpectrogramBuffer(maxRows = 3)
        buffer.update(isProUser = true, frame = frame(timestamp = 1L))

        assertEquals(SpectrogramUiState.LockedPreview, buffer.update(isProUser = false, frame = frame(timestamp = 2L)))
        assertEquals(SpectrogramUiState.Empty, buffer.update(isProUser = true, frame = null))
    }

    private fun frame(timestamp: Long, amplitude: Float = 0.5f): SpectralFrame =
        SpectralFrame(
            bands =
                List(24) { index ->
                    SpectralBand(
                        startFrequencyHz = 20f + index,
                        endFrequencyHz = 40f + index,
                        centerFrequencyHz = 30f + index,
                        normalizedAmplitude = amplitude,
                    )
                },
            dominantFrequencyHz = 1000f,
            bandwidth = SpectralBandwidth.NARROW,
            timestamp = timestamp,
        )
}
