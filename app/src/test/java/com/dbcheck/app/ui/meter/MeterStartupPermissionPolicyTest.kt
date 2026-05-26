package com.dbcheck.app.ui.meter

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeterStartupPermissionPolicyTest {
    @Test
    fun startupRequestsOnlyMicrophoneWhenMicrophoneIsMissing() {
        assertEquals(
            MeterStartupPermissionRequest(
                requestMicrophone = true,
            ),
            MeterStartupPermissionPolicy.startupRequest(microphoneGranted = false),
        )
    }

    @Test
    fun startupRequestsNoPermissionsWhenMicrophoneIsAlreadyGranted() {
        assertEquals(
            MeterStartupPermissionRequest(
                requestMicrophone = false,
            ),
            MeterStartupPermissionPolicy.startupRequest(microphoneGranted = true),
        )
    }

    @Test
    fun notificationPermissionIsRequestedOnlyOnAndroidThirteenWhenMissing() {
        assertFalse(
            MeterNotificationPermissionPolicy.shouldRequestNotificationPermission(
                sdkInt = Build.VERSION_CODES.S,
                notificationPermissionGranted = false,
                notificationPermissionAlreadyRequested = false,
            ),
        )
        assertFalse(
            MeterNotificationPermissionPolicy.shouldRequestNotificationPermission(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                notificationPermissionGranted = true,
                notificationPermissionAlreadyRequested = false,
            ),
        )
        assertTrue(
            MeterNotificationPermissionPolicy.shouldRequestNotificationPermission(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                notificationPermissionGranted = false,
                notificationPermissionAlreadyRequested = false,
            ),
        )
    }

    @Test
    fun notificationPermissionIsNotRequestedAgainAfterInitialPrompt() {
        assertFalse(
            MeterNotificationPermissionPolicy.shouldRequestNotificationPermission(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                notificationPermissionGranted = false,
                notificationPermissionAlreadyRequested = true,
            ),
        )
    }
}
