package com.dbcheck.app.ui.analytics.components

import com.dbcheck.app.R
import com.dbcheck.app.domain.audio.SpectralBandwidth
import com.dbcheck.app.ui.analytics.state.SpectralAnalysisUiState
import com.dbcheck.app.ui.analytics.state.SpectralBandUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpectralStatPillsModelTest {
    @Test
    fun liveSpectralStatsUseLiveFrameData() {
        val pills =
            spectralStatPillsFor(
                SpectralAnalysisUiState.Live(
                    bands =
                        listOf(
                            SpectralBandUiState(centerFrequencyHz = 125f, normalizedAmplitude = 0.2f),
                            SpectralBandUiState(centerFrequencyHz = 2_400f, normalizedAmplitude = 0.9f),
                        ),
                    dominantFrequencyHz = 1_000f,
                    bandwidth = SpectralBandwidth.WIDE,
                ),
            )

        assertEquals(
            listOf(
                SpectralStatPill(labelResId = R.string.spectral_analysis_dominant, value = "1.0 kHz"),
                SpectralStatPill(
                    labelResId = R.string.spectral_analysis_bandwidth,
                    valueResId = R.string.spectral_bandwidth_wide,
                ),
                SpectralStatPill(labelResId = R.string.spectral_stat_peak_band, value = "2.4 kHz"),
                SpectralStatPill(
                    labelResId = R.string.spectral_stat_status,
                    valueResId = R.string.spectral_status_live,
                ),
            ),
            pills,
        )
    }

    @Test
    fun idleSpectralStatsUsePlaceholdersAndIdleStatus() {
        val pills = spectralStatPillsFor(SpectralAnalysisUiState.Idle)

        assertEquals(
            listOf(
                SpectralStatPill(labelResId = R.string.spectral_analysis_dominant, value = "--"),
                SpectralStatPill(labelResId = R.string.spectral_analysis_bandwidth, value = "--"),
                SpectralStatPill(labelResId = R.string.spectral_stat_peak_band, value = "--"),
                SpectralStatPill(
                    labelResId = R.string.spectral_stat_status,
                    valueResId = R.string.spectral_status_idle,
                ),
            ),
            pills,
        )
    }

    @Test
    fun lockedSpectralStatsDerivePreviewValuesFromPreviewBands() {
        val pills = spectralStatPillsFor(SpectralAnalysisUiState.LockedPreview)

        assertEquals(4, pills.size)
        assertEquals(R.string.spectral_stat_status, pills.last().labelResId)
        assertEquals(R.string.spectral_status_preview, pills.last().valueResId)
        assertTrue(pills.first().value?.endsWith("Hz") == true)
        assertTrue(pills[2].value?.endsWith("Hz") == true)
    }
}
