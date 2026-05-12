package com.dbcheck.app.ui.meter

import org.junit.Assert.assertEquals
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
}
