package com.dbcheck.app.ui.hearingtest.results

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudiogramChartStateTest {
    @Test
    fun audiogramCanRenderWhenOnlyRightEarDataExists() {
        assertTrue(
            hasAudiogramData(
                leftData = emptyList(),
                rightData = listOf(250f to -35f),
            ),
        )
    }

    @Test
    fun audiogramDoesNotRenderWhenBothEarsAreEmpty() {
        assertFalse(
            hasAudiogramData(
                leftData = emptyList(),
                rightData = emptyList(),
            ),
        )
    }

    @Test
    fun highFrequencyFormatterShowsUnavailableWhenNoFrequencyWasDetected() {
        assertEquals("N/A", formatHighFrequencyLimit(0f))
    }

    @Test
    fun highFrequencyFormatterShowsHighestDetectedFrequencyInKilohertz() {
        assertEquals("4.0 kHz", formatHighFrequencyLimit(4000f))
    }
}
