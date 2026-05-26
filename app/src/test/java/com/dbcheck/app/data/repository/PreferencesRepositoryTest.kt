package com.dbcheck.app.data.repository

import com.dbcheck.app.data.local.preferences.UserPreferencesDataStore
import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.local.preferences.model.WaveformStyle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PreferencesRepositoryTest {
    private val dataStore = mockk<UserPreferencesDataStore>()

    @Test
    fun exposesUserPreferencesFlowFromDataStore() = runTest {
        val preferences = UserPreferences(isProUser = true)
        val repository = createRepository(preferences)

        assertEquals(preferences, repository.userPreferences.first())
    }

    @Test
    fun updateMethodsDelegateToDataStore() = runTest {
        stubUpdates()
        val repository = createRepository()

        repository.updateThemeMode("dark")
        repository.updateExposureAlerts(false)
        repository.updatePeakWarnings(false)
        repository.updateNotificationThreshold(90)
        repository.updateMicSensitivityOffset(2.5f)
        repository.updateFrequencyWeighting("C")
        repository.updateWaveformStyle(WaveformStyle.BARS)
        repository.updateRefreshRate(MeterRefreshRate.LOW)
        repository.updateLockscreenMeterEnabled(true)
        repository.updateHealthConnectEnabled(true)
        repository.updateHeartRateOverlayEnabled(true)
        repository.updateDebugForceFreeEnabled(true)
        repository.updateProUser(true)

        coVerify(exactly = 1) {
            dataStore.updateThemeMode("dark")
            dataStore.updateExposureAlerts(false)
            dataStore.updatePeakWarnings(false)
            dataStore.updateNotificationThreshold(90)
            dataStore.updateMicSensitivityOffset(2.5f)
            dataStore.updateFrequencyWeighting("C")
            dataStore.updateWaveformStyle(WaveformStyle.BARS)
            dataStore.updateRefreshRate(MeterRefreshRate.LOW)
            dataStore.updateLockscreenMeterEnabled(true)
            dataStore.updateHealthConnectEnabled(true)
            dataStore.updateHeartRateOverlayEnabled(true)
            dataStore.updateDebugForceFreeEnabled(true)
            dataStore.updateProUser(true)
        }
    }

    private fun createRepository(preferences: UserPreferences = UserPreferences()): PreferencesRepository {
        every { dataStore.userPreferences } returns flowOf(preferences)
        return PreferencesRepository(dataStore)
    }

    private fun stubUpdates() {
        coEvery { dataStore.updateThemeMode(any()) } returns Unit
        coEvery { dataStore.updateExposureAlerts(any()) } returns Unit
        coEvery { dataStore.updatePeakWarnings(any()) } returns Unit
        coEvery { dataStore.updateNotificationThreshold(any()) } returns Unit
        coEvery { dataStore.updateMicSensitivityOffset(any()) } returns Unit
        coEvery { dataStore.updateFrequencyWeighting(any()) } returns Unit
        coEvery { dataStore.updateWaveformStyle(any()) } returns Unit
        coEvery { dataStore.updateRefreshRate(any()) } returns Unit
        coEvery { dataStore.updateLockscreenMeterEnabled(any()) } returns Unit
        coEvery { dataStore.updateHealthConnectEnabled(any()) } returns Unit
        coEvery { dataStore.updateHeartRateOverlayEnabled(any()) } returns Unit
        coEvery { dataStore.updateDebugForceFreeEnabled(any()) } returns Unit
        coEvery { dataStore.updateProUser(any()) } returns Unit
    }
}
