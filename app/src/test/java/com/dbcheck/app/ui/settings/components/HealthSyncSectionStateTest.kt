package com.dbcheck.app.ui.settings.components

import com.dbcheck.app.sync.HealthConnectPermissions
import com.dbcheck.app.ui.settings.state.HealthConnectAvailabilityUi
import com.dbcheck.app.ui.settings.state.HealthConnectUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthSyncSectionStateTest {
    @Test
    fun healthConnectToggleIsOffWhenStoredPreferenceIsEnabledButWritePermissionWasRevoked() {
        val state =
            HealthSyncSectionState(
                healthConnectEnabled = true,
                heartRateOverlayEnabled = false,
                isProUser = true,
                status =
                    HealthConnectUiState(
                        availability = HealthConnectAvailabilityUi.AVAILABLE,
                        noiseSyncGranted = false,
                        noiseSyncPermissions = HealthConnectPermissions.NOISE_SYNC,
                    ),
            )

        assertFalse(state.isNoiseSyncActive)
    }

    @Test
    fun heartRateOverlayToggleIsOffWhenStoredPreferenceIsEnabledButReadPermissionWasRevoked() {
        val state =
            HealthSyncSectionState(
                healthConnectEnabled = true,
                heartRateOverlayEnabled = true,
                isProUser = true,
                status =
                    HealthConnectUiState(
                        availability = HealthConnectAvailabilityUi.AVAILABLE,
                        heartRateReadGranted = false,
                        heartRateReadPermissions = HealthConnectPermissions.HEART_RATE_READ,
                    ),
            )

        assertFalse(state.isHeartRateOverlayActive)
    }

    @Test
    fun healthTogglesAreOnOnlyWhenStoredPreferenceAndLiveGrantAreBothAvailable() {
        val state =
            HealthSyncSectionState(
                healthConnectEnabled = true,
                heartRateOverlayEnabled = true,
                isProUser = true,
                status =
                    HealthConnectUiState(
                        availability = HealthConnectAvailabilityUi.AVAILABLE,
                        noiseSyncGranted = true,
                        heartRateReadGranted = true,
                        noiseSyncPermissions = HealthConnectPermissions.NOISE_SYNC,
                        heartRateReadPermissions = HealthConnectPermissions.HEART_RATE_READ,
                    ),
            )

        assertTrue(state.isNoiseSyncActive)
        assertTrue(state.isHeartRateOverlayActive)
    }
}
