package com.dbcheck.app.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dbcheck.app.BuildConfig
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesDataStore
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        private object Keys {
            val THEME_MODE = stringPreferencesKey("theme_mode")
            val EXPOSURE_ALERTS = booleanPreferencesKey("exposure_alerts")
            val PEAK_WARNINGS = booleanPreferencesKey("peak_warnings")
            val NOTIFICATION_THRESHOLD = intPreferencesKey("notification_threshold")
            val MIC_SENSITIVITY_OFFSET = floatPreferencesKey("mic_sensitivity_offset")
            val FREQUENCY_WEIGHTING = stringPreferencesKey("frequency_weighting")
            val WAVEFORM_STYLE = stringPreferencesKey("waveform_style")
            val REFRESH_RATE = stringPreferencesKey("refresh_rate")
            val IS_PRO_USER = booleanPreferencesKey("is_pro_user")
        }

        val userPreferences: Flow<UserPreferences> =
            context.dataStore.data.map { prefs ->
                UserPreferences(
                    themeMode = prefs[Keys.THEME_MODE] ?: "system",
                    exposureAlertsEnabled = prefs[Keys.EXPOSURE_ALERTS] ?: true,
                    peakWarningsEnabled = prefs[Keys.PEAK_WARNINGS] ?: false,
                    notificationThreshold = prefs[Keys.NOTIFICATION_THRESHOLD] ?: 85,
                    micSensitivityOffset = prefs[Keys.MIC_SENSITIVITY_OFFSET] ?: 0f,
                    frequencyWeighting = prefs[Keys.FREQUENCY_WEIGHTING] ?: "A",
                    waveformStyle = prefs[Keys.WAVEFORM_STYLE] ?: "default",
                    refreshRate = prefs[Keys.REFRESH_RATE] ?: "standard",
                    isProUser = prefs[Keys.IS_PRO_USER] ?: BuildConfig.DEBUG,
                )
            }

        suspend fun updateThemeMode(mode: String) {
            context.dataStore.edit { it[Keys.THEME_MODE] = mode }
        }

        suspend fun updateExposureAlerts(enabled: Boolean) {
            context.dataStore.edit { it[Keys.EXPOSURE_ALERTS] = enabled }
        }

        suspend fun updatePeakWarnings(enabled: Boolean) {
            context.dataStore.edit { it[Keys.PEAK_WARNINGS] = enabled }
        }

        suspend fun updateNotificationThreshold(threshold: Int) {
            context.dataStore.edit { it[Keys.NOTIFICATION_THRESHOLD] = threshold }
        }

        suspend fun updateMicSensitivityOffset(offset: Float) {
            context.dataStore.edit { it[Keys.MIC_SENSITIVITY_OFFSET] = offset }
        }

        suspend fun updateFrequencyWeighting(weighting: String) {
            context.dataStore.edit { it[Keys.FREQUENCY_WEIGHTING] = weighting }
        }

        suspend fun updateProUser(isPro: Boolean) {
            context.dataStore.edit { it[Keys.IS_PRO_USER] = isPro }
        }
    }
