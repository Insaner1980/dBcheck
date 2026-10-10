package com.dbcheck.app.service

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class NotificationHelperDeliveryTest {
    @Test
    fun blockedAlertsAreNotDeliveredAndDoNotBlockThePlaybackChannel() {
        val context = RuntimeEnvironment.getApplication()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(NotificationHelper.ALERTS_CHANNEL_ID, "Alerts", NotificationManager.IMPORTANCE_NONE),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                NotificationHelper.AMBIENT_PLAYBACK_CHANNEL_ID,
                "Playback",
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        val helper = NotificationHelper(context)

        assertFalse(helper.sendPeakWarning(125f))
        assertFalse(helper.sendExposureAlert(90f, 30))
        assertTrue(helper.canPostPlaybackNotification())
    }

    @Test
    fun missingChannelCannotBeReportedAsDelivered() {
        val helper = NotificationHelper(RuntimeEnvironment.getApplication())

        assertFalse(helper.sendPeakWarning(125f))
        assertFalse(helper.canPostPlaybackNotification())
    }

    @Test
    fun notificationManagerFailureReturnsFalse() {
        val manager = mockk<NotificationManager> {
            every { getNotificationChannel(NotificationHelper.ALERTS_CHANNEL_ID) } returns
                NotificationChannel(NotificationHelper.ALERTS_CHANNEL_ID, "Alerts", NotificationManager.IMPORTANCE_HIGH)
            every { areNotificationsEnabled() } returns true
            every { notify(any<Int>(), any()) } throws SecurityException("notification rejected")
        }
        val context = object : ContextWrapper(RuntimeEnvironment.getApplication()) {
            override fun getSystemService(name: String): Any? =
                if (name == Context.NOTIFICATION_SERVICE) manager else super.getSystemService(name)
        }

        assertFalse(NotificationHelper(context).sendPeakWarning(125f))
    }
}
