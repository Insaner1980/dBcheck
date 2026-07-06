package com.dbcheck.app.ui.analytics.components

import org.junit.Assert.assertEquals
import org.junit.Test

class SpectralFormattersTest {
    @Test
    fun spectralFrequencyFormatterUsesHzForSubKilohertzValues() {
        assertEquals("125 Hz", formatSpectralFrequency(125f))
        assertEquals("999 Hz", formatSpectralFrequency(999.9f))
    }

    @Test
    fun spectralFrequencyFormatterUsesKilohertzForThousandHertzAndAbove() {
        assertEquals("1.0 kHz", formatSpectralFrequency(1_000f))
        assertEquals("2.4 kHz", formatSpectralFrequency(2_400f))
    }

    @Test
    fun spectralFrequencyFormatterReturnsPlaceholderForInvalidValues() {
        assertEquals("--", formatSpectralFrequency(0f))
        assertEquals("--", formatSpectralFrequency(-20f))
    }
}
