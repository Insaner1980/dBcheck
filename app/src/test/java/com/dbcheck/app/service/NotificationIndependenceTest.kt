package com.dbcheck.app.service

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import com.dbcheck.app.domain.ambient.AmbientSoundPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class NotificationIndependenceTest {
    @Test
    fun simultaneousNotificationsAndStopIntentsRemainIndependent() {
        val context = RuntimeEnvironment.getApplication()
        val helper = NotificationHelper(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        helper.createChannels()
        helper.createChannels()
        val measurement = helper.buildRichMeasurementNotification(
            MeasurementNotificationReading(73f, 94f, "1:00", NotificationNoiseLevel.ELEVATED, hasSamples = true),
            MeasurementNotificationVisibility(false, false, false),
        )
        val ambient = helper.buildAmbientSoundNotification(AmbientSoundPreset.BROWN_NOISE, 0, null)
        helper.updateNotification(NotificationHelper.MEASUREMENT_NOTIFICATION_ID, measurement)
        helper.updateNotification(NotificationHelper.AMBIENT_SOUND_NOTIFICATION_ID, ambient)
        assertTrue(helper.sendPeakWarning(125f))
        assertEquals(3, manager.activeNotifications.size)
        val measurementStop = measurement.actions.single().actionIntent
        val ambientStop = ambient.actions.single().actionIntent
        assertNotEquals(measurementStop, ambientStop)
        assertEquals(
            MeasurementForegroundService::class.java.name,
            shadowOf(measurementStop).savedIntent.component?.className,
        )
        assertEquals(
            AmbientSoundPlaybackService::class.java.name,
            shadowOf(ambientStop).savedIntent.component?.className,
        )
        manager.cancel(NotificationHelper.AMBIENT_SOUND_NOTIFICATION_ID)
        assertEquals(
            setOf(NotificationHelper.MEASUREMENT_NOTIFICATION_ID, NotificationHelper.PEAK_ALERT_NOTIFICATION_ID),
            manager.activeNotifications.map { it.id }.toSet(),
        )
    }
}
