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

    @Test
    fun measurementNotificationIncludesStopActionThatCompletesSession() {
        val source = projectFile("src/main/java/com/dbcheck/app/service/NotificationHelper.kt").readText()

        assertTrue(source.contains(".addAction("))
        assertTrue(source.contains("R.drawable.ic_notification_stop"))
        assertTrue(source.contains("context.getString(R.string.notification_action_stop)"))
        assertTrue(source.contains("measurementStopPendingIntent()"))
        assertTrue(source.contains("PendingIntent.getService("))
        assertTrue(source.contains("MeasurementForegroundService.stopIntent(context, emitCompleted = true)"))
    }
}
