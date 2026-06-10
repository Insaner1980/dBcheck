package com.dbcheck.app.data.local.preferences.model

import com.dbcheck.app.domain.audio.ResponseTime
import com.dbcheck.app.domain.noise.DosimeterStandard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserPreferenceDefaultsTest {
    @Test
    fun userPreferencesUsesSharedDefaults() {
        val preferences = UserPreferences()

        assertEquals(UserPreferenceDefaults.THEME_MODE, preferences.themeMode)
        assertEquals(UserPreferenceDefaults.NOTIFICATION_THRESHOLD, preferences.notificationThreshold)
        assertEquals(UserPreferenceDefaults.FREQUENCY_WEIGHTING, preferences.frequencyWeighting)
        assertEquals(ResponseTime.FAST, UserPreferenceDefaults.responseTime)
        assertEquals(DosimeterStandard.NIOSH_REL, UserPreferenceDefaults.dosimeterStandard)
        assertTrue(preferences.exposureAlertsEnabled)
        assertFalse(preferences.peakWarningsEnabled)
    }

    @Test
    fun responseTimeNormalizationFallsBackToDefault() {
        assertEquals(ResponseTime.IMPULSE, UserPreferenceDefaults.normalizeResponseTime("impulse"))
        assertEquals(ResponseTime.FAST, UserPreferenceDefaults.normalizeResponseTime(null))
        assertEquals(ResponseTime.FAST, UserPreferenceDefaults.normalizeResponseTime("unknown"))
    }

    @Test
    fun dosimeterStandardNormalizationFallsBackToDefault() {
        assertEquals(DosimeterStandard.OSHA_PEL, UserPreferenceDefaults.normalizeDosimeterStandard("osha_pel"))
        assertEquals(DosimeterStandard.NIOSH_REL, UserPreferenceDefaults.normalizeDosimeterStandard(null))
        assertEquals(DosimeterStandard.NIOSH_REL, UserPreferenceDefaults.normalizeDosimeterStandard("unknown"))
    }
}
