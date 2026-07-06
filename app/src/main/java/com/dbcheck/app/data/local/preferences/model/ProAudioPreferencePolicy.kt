package com.dbcheck.app.data.local.preferences.model

import com.dbcheck.app.domain.audio.ResponseTime
import com.dbcheck.app.domain.noise.DosimeterStandard

object ProAudioPreferencePolicy {
    fun canUseProAudioPreferences(isProUser: Boolean): Boolean = isProUser

    fun micOffset(preferences: UserPreferences): Float = if (canUseProAudioPreferences(preferences.isProUser)) {
            preferences.micSensitivityOffset
        } else {
            UserPreferenceDefaults.MIC_SENSITIVITY_OFFSET
        }

    fun weighting(preferences: UserPreferences): String = if (canUseProAudioPreferences(preferences.isProUser)) {
            preferences.frequencyWeighting
        } else {
            UserPreferenceDefaults.FREQUENCY_WEIGHTING
        }

    fun responseTime(isProUser: Boolean, responseTime: ResponseTime): ResponseTime =
        if (canUseProAudioPreferences(isProUser)) {
            responseTime
        } else {
            UserPreferenceDefaults.responseTime
        }

    fun dosimeterStandard(isProUser: Boolean, dosimeterStandard: DosimeterStandard): DosimeterStandard =
        if (canUseProAudioPreferences(isProUser)) {
            dosimeterStandard
        } else {
            UserPreferenceDefaults.dosimeterStandard
        }
}
