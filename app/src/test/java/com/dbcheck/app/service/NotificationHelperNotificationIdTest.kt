package com.dbcheck.app.service

import com.dbcheck.app.projectFile
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun measurementNotificationRespondsToTap() {
        val source = projectFile("src/main/java/com/dbcheck/app/service/NotificationHelper.kt").readText()
        val builderSource =
            source.substring(
                source.indexOf("private fun measurementNotificationBuilder"),
                source.indexOf("private fun measurementTapPendingIntent"),
            )

        assertTrue(builderSource.contains(".setContentIntent(measurementTapPendingIntent())"))
    }
}
