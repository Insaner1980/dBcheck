package com.dbcheck.app.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dbcheck.app.BuildConfig
import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.data.local.preferences.model.UserPreferenceDefaults
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.local.preferences.model.WaveformStyle
import com.dbcheck.app.domain.entitlement.ProEntitlementPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

private object Keys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val EXPOSURE_ALERTS = booleanPreferencesKey("exposure_alerts")
    val PEAK_WARNINGS = booleanPreferencesKey("peak_warnings")
    val NOTIFICATION_THRESHOLD = intPreferencesKey("notification_threshold")
    val MIC_SENSITIVITY_OFFSET = floatPreferencesKey("mic_sensitivity_offset")
    val FREQUENCY_WEIGHTING = stringPreferencesKey("frequency_weighting")
    val WAVEFORM_STYLE = stringPreferencesKey("waveform_style")
    val REFRESH_RATE = stringPreferencesKey("refresh_rate")
    val LOCKSCREEN_METER = booleanPreferencesKey("lockscreen_meter")
    val HEALTH_CONNECT = booleanPreferencesKey("health_connect")
    val HEART_RATE_OVERLAY = booleanPreferencesKey("heart_rate_overlay")
    val DEBUG_FORCE_FREE = booleanPreferencesKey("debug_force_free")
    val IS_PRO_USER = booleanPreferencesKey("is_pro_user")
}

internal fun Flow<Preferences>.toUserPreferencesFlow(isDebugBuild: Boolean): Flow<UserPreferences> =
    catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }.map { prefs -> prefs.toUserPreferences(isDebugBuild) }

private fun Preferences.toUserPreferences(isDebugBuild: Boolean): UserPreferences {
    val debugForceFreeEnabled = this[Keys.DEBUG_FORCE_FREE] ?: UserPreferenceDefaults.DEBUG_FORCE_FREE_ENABLED
    val isPurchased = this[Keys.IS_PRO_USER] ?: UserPreferenceDefaults.IS_PRO_USER
    return UserPreferences(
        themeMode = UserPreferenceDefaults.normalizeThemeMode(this[Keys.THEME_MODE]),
        exposureAlertsEnabled =
            this[Keys.EXPOSURE_ALERTS] ?: UserPreferenceDefaults.EXPOSURE_ALERTS_ENABLED,
        peakWarningsEnabled =
            this[Keys.PEAK_WARNINGS] ?: UserPreferenceDefaults.PEAK_WARNINGS_ENABLED,
        notificationThreshold =
            UserPreferenceDefaults.normalizeNotificationThreshold(this[Keys.NOTIFICATION_THRESHOLD]),
        micSensitivityOffset =
            UserPreferenceDefaults.normalizeMicSensitivityOffset(this[Keys.MIC_SENSITIVITY_OFFSET]),
        frequencyWeighting =
            UserPreferenceDefaults.normalizeFrequencyWeighting(this[Keys.FREQUENCY_WEIGHTING]),
        waveformStyle = WaveformStyle.fromPreference(this[Keys.WAVEFORM_STYLE]),
        refreshRate = MeterRefreshRate.fromPreference(this[Keys.REFRESH_RATE]),
        lockscreenMeterEnabled =
            this[Keys.LOCKSCREEN_METER] ?: UserPreferenceDefaults.LOCKSCREEN_METER_ENABLED,
        healthConnectEnabled =
            this[Keys.HEALTH_CONNECT] ?: UserPreferenceDefaults.HEALTH_CONNECT_ENABLED,
        heartRateOverlayEnabled =
            this[Keys.HEART_RATE_OVERLAY] ?: UserPreferenceDefaults.HEART_RATE_OVERLAY_ENABLED,
        debugForceFreeEnabled = debugForceFreeEnabled,
        isProUser =
            ProEntitlementPolicy.isProUser(
                isPurchased = isPurchased,
                isDebugBuild = isDebugBuild,
                debugForceFreeEnabled = debugForceFreeEnabled,
            ),
    )
}

@Singleton
class UserPreferencesDataStore
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        val userPreferences: Flow<UserPreferences> =
            context.dataStore.data.toUserPreferencesFlow(isDebugBuild = BuildConfig.DEBUG)

        suspend fun updateThemeMode(mode: String) {
            context.dataStore.edit { it[Keys.THEME_MODE] = UserPreferenceDefaults.normalizeThemeMode(mode) }
        }

        suspend fun updateExposureAlerts(enabled: Boolean) {
            context.dataStore.edit { it[Keys.EXPOSURE_ALERTS] = enabled }
        }

        suspend fun updatePeakWarnings(enabled: Boolean) {
            context.dataStore.edit { it[Keys.PEAK_WARNINGS] = enabled }
        }

        suspend fun updateNotificationThreshold(threshold: Int) {
            context.dataStore.edit {
                it[Keys.NOTIFICATION_THRESHOLD] =
                    UserPreferenceDefaults.normalizeNotificationThreshold(threshold)
            }
        }

        suspend fun updateMicSensitivityOffset(offset: Float) {
            context.dataStore.edit {
                it[Keys.MIC_SENSITIVITY_OFFSET] =
                    UserPreferenceDefaults.normalizeMicSensitivityOffset(offset)
            }
        }

        suspend fun updateFrequencyWeighting(weighting: String) {
            context.dataStore.edit {
                it[Keys.FREQUENCY_WEIGHTING] =
                    UserPreferenceDefaults.normalizeFrequencyWeighting(weighting)
            }
        }

        suspend fun updateWaveformStyle(style: WaveformStyle) {
            context.dataStore.edit { it[Keys.WAVEFORM_STYLE] = style.preferenceValue }
        }

        suspend fun updateRefreshRate(rate: MeterRefreshRate) {
            context.dataStore.edit { it[Keys.REFRESH_RATE] = rate.preferenceValue }
        }

        suspend fun updateLockscreenMeterEnabled(enabled: Boolean) {
            context.dataStore.edit { it[Keys.LOCKSCREEN_METER] = enabled }
        }

        suspend fun updateHealthConnectEnabled(enabled: Boolean) {
            context.dataStore.edit { it[Keys.HEALTH_CONNECT] = enabled }
        }

        suspend fun updateHeartRateOverlayEnabled(enabled: Boolean) {
            context.dataStore.edit { it[Keys.HEART_RATE_OVERLAY] = enabled }
        }

        suspend fun updateDebugForceFreeEnabled(enabled: Boolean) {
            context.dataStore.edit { it[Keys.DEBUG_FORCE_FREE] = enabled }
        }

        suspend fun updateProUser(isPro: Boolean) {
            context.dataStore.edit { it[Keys.IS_PRO_USER] = isPro }
        }
    }
