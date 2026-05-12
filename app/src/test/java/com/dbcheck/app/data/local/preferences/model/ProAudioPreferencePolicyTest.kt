package com.dbcheck.app.data.local.preferences.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ProAudioPreferencePolicyTest {
    @Test
    fun freeUserAudioPreferencesResolveToFreeDefaults() {
        val preferences =
            UserPreferences(
                isProUser = false,
                micSensitivityOffset = 7f,
                frequencyWeighting = "C",
            )

        assertEquals(UserPreferenceDefaults.MIC_SENSITIVITY_OFFSET, ProAudioPreferencePolicy.micOffset(preferences))
        assertEquals(UserPreferenceDefaults.FREQUENCY_WEIGHTING, ProAudioPreferencePolicy.weighting(preferences))
    }

    @Test
    fun proUserAudioPreferencesResolveToSavedValues() {
        val preferences =
            UserPreferences(
                isProUser = true,
                micSensitivityOffset = 7f,
                frequencyWeighting = "C",
            )

        assertEquals(7f, ProAudioPreferencePolicy.micOffset(preferences), 0f)
        assertEquals("C", ProAudioPreferencePolicy.weighting(preferences))
    }
}
