package com.dbcheck.app.domain.ambient

import org.junit.Assert.assertEquals
import org.junit.Test

class AmbientSoundPolicyTest {
    @Test
    fun presetsStayLocalAmbientSoundOptions() {
        assertEquals(
            listOf(
                AmbientSoundPreset.WHITE_NOISE,
                AmbientSoundPreset.PINK_NOISE,
                AmbientSoundPreset.BROWN_NOISE,
                AmbientSoundPreset.FAN,
            ),
            AmbientSoundPreset.entries,
        )
        assertEquals(AmbientSoundPreset.WHITE_NOISE, AmbientSoundPolicy.normalizePreset(null))
        assertEquals(AmbientSoundPreset.PINK_NOISE, AmbientSoundPolicy.normalizePreset("PINK_NOISE"))
        assertEquals(AmbientSoundPreset.WHITE_NOISE, AmbientSoundPolicy.normalizePreset("therapy"))
    }

    @Test
    fun volumeIsClampedToPlaybackRange() {
        assertEquals(0.35f, AmbientSoundPolicy.normalizeVolume(null), 0f)
        assertEquals(0.05f, AmbientSoundPolicy.normalizeVolume(-1f), 0f)
        assertEquals(0.35f, AmbientSoundPolicy.normalizeVolume(Float.NaN), 0f)
        assertEquals(0.5f, AmbientSoundPolicy.normalizeVolume(0.5f), 0f)
        assertEquals(1f, AmbientSoundPolicy.normalizeVolume(2f), 0f)
    }

    @Test
    fun timerOptionsAreExplicitAndInvalidValuesReturnDefault() {
        assertEquals(listOf(0, 15, 30, 60, 120), AmbientSoundPolicy.TIMER_OPTIONS_MINUTES)
        assertEquals(30, AmbientSoundPolicy.normalizeTimerMinutes(null))
        assertEquals(0, AmbientSoundPolicy.normalizeTimerMinutes(0))
        assertEquals(15, AmbientSoundPolicy.normalizeTimerMinutes(15))
        assertEquals(30, AmbientSoundPolicy.normalizeTimerMinutes(7))
    }
}
