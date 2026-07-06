package com.dbcheck.app.service

import androidx.core.app.NotificationCompat
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPrivacyPolicyTest {
    @Test
    fun measurementNotificationsStayPrivateByDefault() {
        assertEquals(
            NotificationCompat.VISIBILITY_PRIVATE,
            NotificationPrivacyPolicy.measurementLockscreenVisibility(
                isProUser = true,
                lockscreenMeterEnabled = true,
                showLockscreenMeterPublicly = false,
            ),
        )
    }

    @Test
    fun publicLockscreenVisibilityRequiresProLockscreenMeterAndExplicitOptIn() {
        assertEquals(
            NotificationCompat.VISIBILITY_PRIVATE,
            NotificationPrivacyPolicy.measurementLockscreenVisibility(
                isProUser = false,
                lockscreenMeterEnabled = true,
                showLockscreenMeterPublicly = true,
            ),
        )
        assertEquals(
            NotificationCompat.VISIBILITY_PRIVATE,
            NotificationPrivacyPolicy.measurementLockscreenVisibility(
                isProUser = true,
                lockscreenMeterEnabled = false,
                showLockscreenMeterPublicly = true,
            ),
        )
        assertEquals(
            NotificationCompat.VISIBILITY_PUBLIC,
            NotificationPrivacyPolicy.measurementLockscreenVisibility(
                isProUser = true,
                lockscreenMeterEnabled = true,
                showLockscreenMeterPublicly = true,
            ),
        )
    }
}
