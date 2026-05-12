package com.dbcheck.app.data.local.preferences.model

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
        assertTrue(preferences.exposureAlertsEnabled)
        assertFalse(preferences.peakWarningsEnabled)
    }
}
