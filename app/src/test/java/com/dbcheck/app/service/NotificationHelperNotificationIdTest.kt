package com.dbcheck.app.service

import org.junit.Assert.assertNotEquals
import org.junit.Test

class NotificationHelperNotificationIdTest {
    @Test
    fun exposureAndPeakAlertsUseSeparateNotificationIds() {
        assertNotEquals(
            NotificationHelper.EXPOSURE_ALERT_NOTIFICATION_ID,
            NotificationHelper.PEAK_ALERT_NOTIFICATION_ID,
        )
        assertNotEquals(
            NotificationHelper.MEASUREMENT_NOTIFICATION_ID,
            NotificationHelper.EXPOSURE_ALERT_NOTIFICATION_ID,
        )
        assertNotEquals(
            NotificationHelper.MEASUREMENT_NOTIFICATION_ID,
            NotificationHelper.PEAK_ALERT_NOTIFICATION_ID,
        )
    }
}
