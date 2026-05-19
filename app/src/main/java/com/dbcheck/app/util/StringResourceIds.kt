package com.dbcheck.app.util

import androidx.annotation.StringRes
import com.dbcheck.app.R
import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.data.local.preferences.model.ThemeMode
import com.dbcheck.app.data.local.preferences.model.WaveformStyle
import com.dbcheck.app.domain.audio.WeightingType
import com.dbcheck.app.domain.hearingtest.Ear
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
fun hearingTestRatingStringRes(rating: String): Int = when (rating) {
        "Excellent" -> R.string.hearing_rating_excellent
        "Good" -> R.string.hearing_rating_good
        "Fair" -> R.string.hearing_rating_fair
        else -> R.string.hearing_rating_poor
    }
