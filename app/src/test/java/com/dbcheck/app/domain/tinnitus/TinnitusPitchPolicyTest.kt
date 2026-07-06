package com.dbcheck.app.domain.tinnitus

import com.dbcheck.app.domain.hearingtest.Ear
import org.junit.Assert.assertEquals
import org.junit.Test

class TinnitusPitchPolicyTest {
    @Test
    fun normalizesPitchFrequencyIntoExistingToneGeneratorRange() {
        assertEquals(TinnitusPitchPolicy.MIN_FREQUENCY_HZ, TinnitusPitchPolicy.normalizeFrequencyHz(80f), 0f)
        assertEquals(TinnitusPitchPolicy.MAX_FREQUENCY_HZ, TinnitusPitchPolicy.normalizeFrequencyHz(12_500f), 0f)
        assertEquals(4_000f, TinnitusPitchPolicy.normalizeFrequencyHz(4_012f), 0f)
    }

    @Test
    fun profileUpdatesSelectedEarOnly() {
        val profile =
            TinnitusPitchProfile(
                leftFrequencyHz = 1_000f,
                rightFrequencyHz = 4_000f,
                updatedAtMs = 1_000L,
            )

        val updated = profile.withFrequency(Ear.RIGHT, 8_250f, updatedAtMs = 2_000L)

        assertEquals(1_000f, updated.leftFrequencyHz ?: 0f, 0f)
        assertEquals(TinnitusPitchPolicy.MAX_FREQUENCY_HZ, updated.rightFrequencyHz ?: 0f, 0f)
        assertEquals(2_000L, updated.updatedAtMs)
    }

    @Test
    fun previewToneUsesFixedSafeAmplitude() {
        assertEquals(-36f, TinnitusPitchPolicy.PREVIEW_AMPLITUDE_DB, 0f)
    }
}
