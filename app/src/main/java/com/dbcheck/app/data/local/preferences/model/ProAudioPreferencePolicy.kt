package com.dbcheck.app.data.local.preferences.model

object ProAudioPreferencePolicy {
    fun canUseProAudioPreferences(isProUser: Boolean): Boolean = isProUser

    fun micOffset(preferences: UserPreferences): Float =
        if (canUseProAudioPreferences(preferences.isProUser)) {
            preferences.micSensitivityOffset
        } else {
            UserPreferenceDefaults.MIC_SENSITIVITY_OFFSET
        }

    fun weighting(preferences: UserPreferences): String =
        if (canUseProAudioPreferences(preferences.isProUser)) {
            preferences.frequencyWeighting
        } else {
            UserPreferenceDefaults.FREQUENCY_WEIGHTING
        }
}
