package com.dbcheck.app.service

import androidx.core.app.NotificationCompat
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPrivacyPolicyTest {
    @Test
    fun measurementNotificationsDoNotExposeLiveReadingsOnPublicLockScreen() {
        assertEquals(
            NotificationCompat.VISIBILITY_PRIVATE,
            NotificationPrivacyPolicy.measurementLockscreenVisibility(),
        )
    }
}
