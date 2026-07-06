package com.dbcheck.app.ui.common

import android.app.Activity
import android.app.Application
import android.content.ContextWrapper
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ContextActivityTest {
    @Test
    fun findActivityUnwrapsContextWrappers() {
        val activity = Robolectric
            .buildActivity(Activity::class.java)
            .setup()
            .get()
        val wrappedContext = ContextWrapper(ContextWrapper(activity))

        assertSame(activity, wrappedContext.findActivity())
    }
}
