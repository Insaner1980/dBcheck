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
                requestNotification = false,
            ),
            MeterStartupPermissionPolicy.startupRequest(microphoneGranted = false),
        )
    }

    @Test
    fun startupRequestsNoPermissionsWhenMicrophoneIsAlreadyGranted() {
        assertEquals(
            MeterStartupPermissionRequest(
                requestMicrophone = false,
                requestNotification = false,
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
            ),
        )
        assertFalse(
            MeterNotificationPermissionPolicy.shouldRequestNotificationPermission(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                notificationPermissionGranted = true,
            ),
        )
        assertTrue(
            MeterNotificationPermissionPolicy.shouldRequestNotificationPermission(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                notificationPermissionGranted = false,
            ),
        )
    }
}
