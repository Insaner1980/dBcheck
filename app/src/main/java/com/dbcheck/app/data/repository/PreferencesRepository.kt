package com.dbcheck.app.data.repository

import com.dbcheck.app.data.local.preferences.UserPreferencesDataStore
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(
    private val dataStore: UserPreferencesDataStore,
) {
    val userPreferences: Flow<UserPreferences> = dataStore.userPreferences

    suspend fun updateThemeMode(mode: String) = dataStore.updateThemeMode(mode)
    suspend fun updateExposureAlerts(enabled: Boolean) = dataStore.updateExposureAlerts(enabled)
    suspend fun updatePeakWarnings(enabled: Boolean) = dataStore.updatePeakWarnings(enabled)
    suspend fun updateNotificationThreshold(threshold: Int) = dataStore.updateNotificationThreshold(threshold)
    suspend fun updateMicSensitivityOffset(offset: Float) = dataStore.updateMicSensitivityOffset(offset)
    suspend fun updateFrequencyWeighting(weighting: String) = dataStore.updateFrequencyWeighting(weighting)
    suspend fun updateProUser(isPro: Boolean) = dataStore.updateProUser(isPro)
}
