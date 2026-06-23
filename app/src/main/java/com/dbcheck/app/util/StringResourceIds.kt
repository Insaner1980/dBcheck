package com.dbcheck.app.util

import androidx.annotation.StringRes
import com.dbcheck.app.R
import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.data.local.preferences.model.ThemeMode
import com.dbcheck.app.data.local.preferences.model.WaveformStyle
import com.dbcheck.app.domain.audio.ResponseTime
import com.dbcheck.app.domain.audio.WeightingType
import com.dbcheck.app.domain.hearingtest.Ear
import com.dbcheck.app.domain.hearingtest.HearingRating
import com.dbcheck.app.domain.noise.DosimeterStandard
import com.dbcheck.app.domain.noise.NoiseLevel

@StringRes
fun NoiseLevel.labelStringRes(): Int = when (this) {
        NoiseLevel.QUIET -> R.string.noise_level_quiet
        NoiseLevel.NORMAL -> R.string.noise_level_normal
        NoiseLevel.ELEVATED -> R.string.noise_level_elevated
        NoiseLevel.DANGEROUS -> R.string.noise_level_dangerous
    }

@StringRes
fun WeightingType.displayNameStringRes(): Int = when (this) {
        WeightingType.A -> R.string.weighting_a
        WeightingType.B -> R.string.weighting_b
        WeightingType.C -> R.string.weighting_c
        WeightingType.Z -> R.string.weighting_z
        WeightingType.ITUR468 -> R.string.weighting_itu_r_468
    }

@StringRes
fun ResponseTime.displayNameStringRes(): Int = when (this) {
        ResponseTime.FAST -> R.string.response_time_fast
        ResponseTime.SLOW -> R.string.response_time_slow
        ResponseTime.IMPULSE -> R.string.response_time_impulse
    }

@StringRes
fun DosimeterStandard.displayNameStringRes(): Int = when (this) {
        DosimeterStandard.NIOSH_REL -> R.string.meter_dosimeter_standard_niosh_rel
        DosimeterStandard.OSHA_PEL -> R.string.meter_dosimeter_standard_osha_pel
    }

@StringRes
fun Ear.labelStringRes(): Int = when (this) {
        Ear.LEFT -> R.string.hearing_left_ear_only
        Ear.RIGHT -> R.string.hearing_right_ear_only
    }

@StringRes
fun Ear.lowercaseNameStringRes(): Int = when (this) {
        Ear.LEFT -> R.string.hearing_left_lower
        Ear.RIGHT -> R.string.hearing_right_lower
    }

@StringRes
fun ThemeMode.displayNameStringRes(): Int = when (this) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.DARK -> R.string.theme_dark
        ThemeMode.LIGHT -> R.string.theme_light
    }

@StringRes
fun WaveformStyle.displayNameStringRes(): Int = when (this) {
        WaveformStyle.LINE -> R.string.waveform_line
        WaveformStyle.FILLED -> R.string.waveform_filled
        WaveformStyle.BARS -> R.string.waveform_bars
    }

@StringRes
fun MeterRefreshRate.displayNameStringRes(): Int = when (this) {
        MeterRefreshRate.HIGH -> R.string.meter_refresh_high
        MeterRefreshRate.STANDARD -> R.string.meter_refresh_standard
        MeterRefreshRate.LOW -> R.string.meter_refresh_low_power
    }

@StringRes
fun hearingTestRatingStringRes(rating: String): Int = hearingTestRatingStringRes(HearingRating.fromCode(rating))

@StringRes
fun hearingTestRatingStringRes(rating: HearingRating): Int = when (rating) {
        HearingRating.EXCELLENT -> R.string.hearing_rating_excellent
        HearingRating.GOOD -> R.string.hearing_rating_good
        HearingRating.FAIR -> R.string.hearing_rating_fair
        HearingRating.POOR -> R.string.hearing_rating_poor
    }
