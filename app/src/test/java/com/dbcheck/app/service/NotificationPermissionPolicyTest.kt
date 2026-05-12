package com.dbcheck.app.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionPolicyTest {
    @Test
    fun androidThirteenRequiresRuntimePermissionAndEnabledNotifications() {
        assertTrue(
            NotificationPermissionPolicy.canPostRegularNotification(
                sdkInt = 33,
                runtimePermissionGranted = true,
                notificationsEnabled = true,
            ),
        )

        assertFalse(
            NotificationPermissionPolicy.canPostRegularNotification(
                sdkInt = 33,
                runtimePermissionGranted = false,
                notificationsEnabled = true,
            ),
        )
    }

    @Test
    fun olderAndroidVersionsUseNotificationEnabledStateOnly() {
        assertTrue(
            NotificationPermissionPolicy.canPostRegularNotification(
                sdkInt = 32,
                runtimePermissionGranted = false,
                notificationsEnabled = true,
            ),
        )

        assertFalse(
            NotificationPermissionPolicy.canPostRegularNotification(
                sdkInt = 32,
                runtimePermissionGranted = true,
                notificationsEnabled = false,
            ),
        )
    }
}
