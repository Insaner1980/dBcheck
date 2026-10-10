package com.dbcheck.app.service

import android.app.Application
import android.app.Notification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class NotificationMeasurementAvailabilityTest {
    @Test
    fun startupWithoutSamplesDoesNotPresentZeroDbInBasicNotification() = verifyWaiting(false)

    @Test
    fun startupWithoutSamplesDoesNotPresentQuietRichNotification() = verifyWaiting(true)

    @Test
    fun measuredZeroRemainsARealReading() {
        val notification = NotificationHelper(RuntimeEnvironment.getApplication()).buildRichMeasurementNotification(
            reading = MeasurementNotificationReading(0f, 0f, "0:00", NotificationNoiseLevel.SAFE, hasSamples = true),
            visibility = MeasurementNotificationVisibility(true, true, false),
        )
        assertEquals("0 dB · 0:00", notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertNotNull(notification.contentView)
        assertNotNull(notification.bigContentView)
    }

    private fun verifyWaiting(rich: Boolean) {
        val notification = NotificationHelper(RuntimeEnvironment.getApplication()).buildRichMeasurementNotification(
            reading = MeasurementNotificationReading(0f, 0f, "0:00", NotificationNoiseLevel.SAFE),
            visibility = MeasurementNotificationVisibility(rich, rich, false),
        )
        val text = notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
        assertFalse("Unmeasured zero shown: $text", text.contains("0 dB"))
        assertEquals("Waiting for a measurement", text)
        assertNull(notification.contentView)
        assertNull(notification.bigContentView)
    }
}
