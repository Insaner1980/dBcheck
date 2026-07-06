package com.dbcheck.app.ui.common

import android.app.Activity
import android.content.ContextWrapper
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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
