package com.dbcheck.app.data.local.preferences.model

import com.dbcheck.app.domain.audio.ResponseTime
import com.dbcheck.app.domain.noise.DosimeterStandard
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
    fun freeUserResponseTimeResolvesToFreeDefault() {
        assertEquals(
            UserPreferenceDefaults.responseTime,
            ProAudioPreferencePolicy.responseTime(isProUser = false, responseTime = ResponseTime.IMPULSE),
        )
    }

    @Test
    fun freeUserDosimeterStandardResolvesToFreeDefault() {
        assertEquals(
            UserPreferenceDefaults.dosimeterStandard,
            ProAudioPreferencePolicy.dosimeterStandard(
                isProUser = false,
                dosimeterStandard = DosimeterStandard.OSHA_PEL,
            ),
        )
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

    @Test
    fun proUserResponseTimeResolvesToSavedValue() {
        assertEquals(
            ResponseTime.SLOW,
            ProAudioPreferencePolicy.responseTime(isProUser = true, responseTime = ResponseTime.SLOW),
        )
    }

    @Test
    fun proUserDosimeterStandardResolvesToSavedValue() {
        assertEquals(
            DosimeterStandard.OSHA_PEL,
            ProAudioPreferencePolicy.dosimeterStandard(
                isProUser = true,
                dosimeterStandard = DosimeterStandard.OSHA_PEL,
            ),
        )
    }
}
