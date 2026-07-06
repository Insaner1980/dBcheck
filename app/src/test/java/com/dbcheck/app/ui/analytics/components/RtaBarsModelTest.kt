package com.dbcheck.app.ui.analytics.components

import com.dbcheck.app.R
import com.dbcheck.app.ui.analytics.state.RtaBandUiState
import com.dbcheck.app.ui.analytics.state.RtaUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RtaBarsModelTest {
    @Test
    fun liveRtaBarsUseUiStateBands() {
        val bands =
            listOf(
                RtaBandUiState(centerFrequencyHz = 31.62f, normalizedAmplitude = 0.25f),
                RtaBandUiState(centerFrequencyHz = 1_000f, normalizedAmplitude = 1f),
            )

        val bars = rtaBarsFor(RtaUiState.Data(bands))

        assertEquals(
            listOf(
                RtaBarCanvasItem(index = 0, centerFrequencyHz = 31.62f, normalizedAmplitude = 0.25f),
                RtaBarCanvasItem(index = 1, centerFrequencyHz = 1_000f, normalizedAmplitude = 1f),
            ),
            bars,
        )
    }

    @Test
    fun lockedRtaBarsUseStablePreviewValues() {
        val bars = rtaBarsFor(RtaUiState.LockedPreview)

        assertEquals(10, bars.size)
        assertTrue(bars.any { it.normalizedAmplitude > 0f })
        assertTrue(bars.all { it.normalizedAmplitude in 0f..1f })
    }

    @Test
    fun emptyRtaBarsReturnNoDrawableItems() {
        assertEquals(emptyList<RtaBarCanvasItem>(), rtaBarsFor(RtaUiState.Empty))
    }

    @Test
    fun rtaStatPillsUsePeakBandAndBandCount() {
        val pills =
            rtaStatPillsFor(
                RtaUiState.Data(
                    bands =
                        listOf(
                            RtaBandUiState(centerFrequencyHz = 125f, normalizedAmplitude = 0.2f),
                            RtaBandUiState(centerFrequencyHz = 1_000f, normalizedAmplitude = 0.9f),
                        ),
                ),
            )

        assertEquals(
            listOf(
                SpectralStatPill(labelResId = R.string.spectral_rta_peak, value = "1.0 kHz"),
                SpectralStatPill(labelResId = R.string.spectral_rta_bands, value = "2"),
            ),
            pills,
        )
    }
}
