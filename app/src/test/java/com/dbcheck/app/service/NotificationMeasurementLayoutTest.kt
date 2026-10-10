package com.dbcheck.app.service

import android.app.Application
import android.app.Notification
import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.widget.FrameLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [28], qualifiers = "w320dp-mdpi")
class NotificationMeasurementLayoutTest {
    @Test
    @Config(sdk = [31])
    fun collapsedContentFitsAndroid12Limit() {
        val context = RuntimeEnvironment.getApplication()
        val notification = notification(context)
        val collapsed = notification.contentView
        if (collapsed != null) {
            val view = collapsed.apply(context, FrameLayout(context))
            view.measure(View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY), View.MeasureSpec.UNSPECIFIED)
            assertTrue("Collapsed height ${view.measuredHeight} exceeds 48dp", view.measuredHeight <= 48)
        }
        assertEquals("114 dB · 100:00:00", notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        val expanded = notification.bigContentView!!.apply(context, FrameLayout(context))
        expanded.measure(View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY), View.MeasureSpec.UNSPECIFIED)
        assertTrue("Normal expanded height ${expanded.measuredHeight} exceeds 252dp", expanded.measuredHeight <= 252)
    }

    @Test
    fun expandedContentFitsAt200PercentFont() {
        val base = RuntimeEnvironment.getApplication()
        val configuration = Configuration(base.resources.configuration).apply { fontScale = 2f }
        val context = base.createConfigurationContext(configuration)
        val notification = notification(context)
        val expanded = notification.bigContentView
        if (expanded != null) {
            val view = expanded.apply(context, FrameLayout(context))
            view.measure(View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY), View.MeasureSpec.UNSPECIFIED)
            assertTrue("Expanded height ${view.measuredHeight} exceeds 252dp", view.measuredHeight <= 252)
        } else {
            val text = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString()
            assertTrue(text, text.contains("114 dB"))
            assertTrue(text, text.contains("Peak 132 dB"))
            assertTrue(text, text.contains("100:00:00"))
        }
    }

    private fun notification(context: Context) = NotificationHelper(context).buildRichMeasurementNotification(
        reading =
            MeasurementNotificationReading(
                114f, 132f, "100:00:00", NotificationNoiseLevel.DANGEROUS, hasSamples = true,
            ),
        visibility = MeasurementNotificationVisibility(true, true, false),
    )
}
