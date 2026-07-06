package com.dbcheck.app.ui.meter

import android.app.Activity
import android.view.Window
import android.view.WindowManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MeterKeepScreenOnControllerTest {
    @Test
    fun enablingAddsKeepScreenOnFlagAndReleaseClearsIt() {
        val window = activityWindow()
        val controller = MeterKeepScreenOnController(window)

        controller.update(enabled = true)
        assertTrue(window.hasKeepScreenOnFlag())

        controller.release()
        assertFalse(window.hasKeepScreenOnFlag())
    }

    @Test
    fun disablingClearsKeepScreenOnFlag() {
        val window = activityWindow()
        val controller = MeterKeepScreenOnController(window)

        controller.update(enabled = true)
        controller.update(enabled = false)

        assertFalse(window.hasKeepScreenOnFlag())
    }

    private fun activityWindow(): Window = Robolectric
        .buildActivity(Activity::class.java)
        .setup()
        .get()
        .window

    private fun Window.hasKeepScreenOnFlag(): Boolean =
        attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0
}
