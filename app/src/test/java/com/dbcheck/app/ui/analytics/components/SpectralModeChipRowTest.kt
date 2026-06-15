package com.dbcheck.app.ui.analytics.components

import com.dbcheck.app.ui.analytics.state.SpectralMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpectralModeChipRowTest {
    @Test
    fun spectralModeToggleStartsWithBarsMode() {
        val items = spectralModeChipItems(selectedMode = SpectralMode.BARS)

        assertEquals(
            listOf(SpectralMode.BARS, SpectralMode.SPECTROGRAM, SpectralMode.RTA),
            items.map { it.mode },
        )
        assertEquals(SpectralMode.BARS, items.single { it.isSelected }.mode)
        assertEquals(1, items.count { it.isSelected })
    }

    @Test
    fun spectralModeToggleCanSelectRtaMode() {
        val items = spectralModeChipItems(selectedMode = SpectralMode.RTA)

        assertEquals(SpectralMode.RTA, items.single { it.isSelected }.mode)
        assertTrue(items.filterNot { it.mode == SpectralMode.RTA }.none { it.isSelected })
    }
}
