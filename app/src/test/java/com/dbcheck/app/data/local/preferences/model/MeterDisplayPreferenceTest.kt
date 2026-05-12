package com.dbcheck.app.data.local.preferences.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MeterDisplayPreferenceTest {
    @Test
    fun waveformStyleFallsBackToLineForUnknownPreference() {
        assertEquals(WaveformStyle.LINE, WaveformStyle.fromPreference(null))
        assertEquals(WaveformStyle.LINE, WaveformStyle.fromPreference("default"))
        assertEquals(WaveformStyle.LINE, WaveformStyle.fromPreference("unexpected"))
    }

    @Test
    fun refreshRateFallsBackToStandardForUnknownPreference() {
        assertEquals(MeterRefreshRate.STANDARD, MeterRefreshRate.fromPreference(null))
        assertEquals(MeterRefreshRate.STANDARD, MeterRefreshRate.fromPreference("standard"))
        assertEquals(MeterRefreshRate.STANDARD, MeterRefreshRate.fromPreference("unexpected"))
    }

    @Test
    fun refreshRateExposesUiAndPersistenceIntervals() {
        assertEquals(100L, MeterRefreshRate.HIGH.uiIntervalMs)
        assertEquals(100L, MeterRefreshRate.HIGH.persistenceIntervalMs)
        assertEquals(250L, MeterRefreshRate.STANDARD.uiIntervalMs)
        assertEquals(1_000L, MeterRefreshRate.STANDARD.persistenceIntervalMs)
        assertEquals(1_000L, MeterRefreshRate.LOW.uiIntervalMs)
        assertEquals(5_000L, MeterRefreshRate.LOW.persistenceIntervalMs)
    }
}
