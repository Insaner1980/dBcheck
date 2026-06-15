package com.dbcheck.app.util

import com.dbcheck.app.R
import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.data.local.preferences.model.ThemeMode
import com.dbcheck.app.data.local.preferences.model.WaveformStyle
import com.dbcheck.app.domain.audio.ResponseTime
import com.dbcheck.app.domain.audio.WeightingType
import com.dbcheck.app.domain.hearingtest.Ear
import com.dbcheck.app.domain.noise.NoiseLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class StringResourceIdsTest {
    @Test
    fun noiseLevelLabelStringResMapsEveryNoiseLevel() {
        assertEquals(R.string.noise_level_quiet, NoiseLevel.QUIET.labelStringRes())
        assertEquals(R.string.noise_level_normal, NoiseLevel.NORMAL.labelStringRes())
        assertEquals(R.string.noise_level_elevated, NoiseLevel.ELEVATED.labelStringRes())
        assertEquals(R.string.noise_level_dangerous, NoiseLevel.DANGEROUS.labelStringRes())
    }

    @Test
    fun weightingDisplayNameStringResMapsEveryWeightingType() {
        assertEquals(R.string.weighting_a, WeightingType.A.displayNameStringRes())
        assertEquals(R.string.weighting_b, WeightingType.B.displayNameStringRes())
        assertEquals(R.string.weighting_c, WeightingType.C.displayNameStringRes())
        assertEquals(R.string.weighting_z, WeightingType.Z.displayNameStringRes())
        assertEquals(R.string.weighting_itu_r_468, WeightingType.ITUR468.displayNameStringRes())
    }

    @Test
    fun responseTimeDisplayNameStringResMapsEveryResponseTime() {
        assertEquals(R.string.response_time_fast, ResponseTime.FAST.displayNameStringRes())
        assertEquals(R.string.response_time_slow, ResponseTime.SLOW.displayNameStringRes())
        assertEquals(R.string.response_time_impulse, ResponseTime.IMPULSE.displayNameStringRes())
    }

    @Test
    fun earStringResMappingsKeepDisplayAndLowercaseFormsSeparate() {
        assertEquals(R.string.hearing_left_ear_only, Ear.LEFT.labelStringRes())
        assertEquals(R.string.hearing_right_ear_only, Ear.RIGHT.labelStringRes())
        assertEquals(R.string.hearing_left_lower, Ear.LEFT.lowercaseNameStringRes())
        assertEquals(R.string.hearing_right_lower, Ear.RIGHT.lowercaseNameStringRes())
    }

    @Test
    fun preferenceDisplayNameStringResMapsEveryTypedPreferenceOption() {
        assertEquals(R.string.theme_system, ThemeMode.SYSTEM.displayNameStringRes())
        assertEquals(R.string.theme_dark, ThemeMode.DARK.displayNameStringRes())
        assertEquals(R.string.theme_light, ThemeMode.LIGHT.displayNameStringRes())
        assertEquals(R.string.waveform_line, WaveformStyle.LINE.displayNameStringRes())
        assertEquals(R.string.waveform_filled, WaveformStyle.FILLED.displayNameStringRes())
        assertEquals(R.string.waveform_bars, WaveformStyle.BARS.displayNameStringRes())
        assertEquals(R.string.meter_refresh_high, MeterRefreshRate.HIGH.displayNameStringRes())
        assertEquals(R.string.meter_refresh_standard, MeterRefreshRate.STANDARD.displayNameStringRes())
        assertEquals(R.string.meter_refresh_low_power, MeterRefreshRate.LOW.displayNameStringRes())
    }

    @Test
    fun hearingTestRatingStringResMapsKnownRatingsAndFallback() {
        assertEquals(R.string.hearing_rating_excellent, hearingTestRatingStringRes("Excellent"))
        assertEquals(R.string.hearing_rating_good, hearingTestRatingStringRes("Good"))
        assertEquals(R.string.hearing_rating_fair, hearingTestRatingStringRes("Fair"))
        assertEquals(R.string.hearing_rating_poor, hearingTestRatingStringRes("Poor"))
        assertEquals(R.string.hearing_rating_poor, hearingTestRatingStringRes("Unknown"))
    }
}
