package com.dbcheck.app

import android.app.Application
import android.content.Context
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid
import io.sentry.android.core.SentryAndroidOptions
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class SentryInitTest {
    @Before
    fun mockInitialization() = mockkStatic(SentryAndroid::class)

    @After
    fun restoreInitialization() = unmockkStatic(SentryAndroid::class)

    @Test
    fun missingDsnSkipsSdk() {
        SentryInit.init(RuntimeEnvironment.getApplication(), " ")
        verify(exactly = 0) {
            SentryAndroid.init(any<Context>(), any<Sentry.OptionsConfiguration<SentryAndroidOptions>>())
        }
    }

    @Test
    fun initializationFailureDoesNotPreventApplicationStartup() {
        every {
            SentryAndroid.init(any<Context>(), any<Sentry.OptionsConfiguration<SentryAndroidOptions>>())
        } throws IllegalArgumentException("synthetic invalid DSN")
        SentryInit.init(RuntimeEnvironment.getApplication(), "synthetic-invalid")
    }

    @Test
    fun configuredSdkDoesNotEnableSensitiveLoggingOrTracing() {
        val configuration = slot<Sentry.OptionsConfiguration<SentryAndroidOptions>>()
        every { SentryAndroid.init(any<Context>(), capture(configuration)) } returns Unit
        SentryInit.init(RuntimeEnvironment.getApplication(), "https://synthetic@example.invalid/1")
        val options = SentryAndroidOptions()
        configuration.captured.configure(options)
        assertFalse(options.isDebug)
        assertEquals(0.0, options.tracesSampleRate ?: -1.0, 0.0)
        assertEquals(0, options.maxBreadcrumbs)
        assertFalse(options.isAttachScreenshot)
        assertFalse(options.isAttachViewHierarchy)
    }
}
