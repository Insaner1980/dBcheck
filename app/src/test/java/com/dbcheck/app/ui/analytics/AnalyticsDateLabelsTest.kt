package com.dbcheck.app.ui.analytics

import android.app.Application
import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.clearForTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.time.Instant
import java.util.Locale
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28], shadows = [LocaleDatePatternShadow::class])
class AnalyticsDateLabelsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun dayLabelUsesFinnishAndEnglishDateOrder() {
        val originalLocale = Locale.getDefault()
        val originalZone = TimeZone.getDefault()
        val viewModel = AnalyticsViewModelTestFixture(kotlinx.coroutines.Dispatchers.Main).createViewModel()
        val format = AnalyticsViewModel::class.java.getDeclaredMethod("formatDayLabel", Long::class.javaPrimitiveType)
        format.isAccessible = true
        val timestamp = Instant.parse("2026-10-07T12:00:00Z").toEpochMilli()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            Locale.setDefault(Locale.forLanguageTag("fi-FI"))
            val finnish = format.invoke(viewModel, timestamp) as String
            assertTrue(finnish, finnish.startsWith("7"))
            Locale.setDefault(Locale.US)
            val english = format.invoke(viewModel, timestamp) as String
            assertTrue(english.startsWith("Oct"))
            assertTrue(english.endsWith("7"))
        } finally {
            viewModel.clearForTest()
            Locale.setDefault(originalLocale)
            TimeZone.setDefault(originalZone)
        }
    }
}

// Robolectric's default ICU shadow returns the skeleton unchanged for MMMd.
@Implements(className = "libcore.icu.ICU", isInAndroidSdk = false)
class LocaleDatePatternShadow private constructor() {
    companion object {
        @JvmStatic
        @Implementation
        fun getBestDateTimePattern(skeleton: String, locale: Locale): String =
            android.icu.text.DateTimePatternGenerator.getInstance(locale).getBestPattern(skeleton)
    }
}
