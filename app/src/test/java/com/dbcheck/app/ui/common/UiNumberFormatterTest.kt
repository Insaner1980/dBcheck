package com.dbcheck.app.ui.common

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class UiNumberFormatterTest {
    private val originalLocale = Locale.getDefault()

    @After
    fun restoreDefaultLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun finnishDefaultLocaleStillUsesPointDecimalSeparator() {
        Locale.setDefault(Locale.forLanguageTag("fi-FI"))

        assertUiFormats()
    }

    @Test
    fun usDefaultLocaleUsesSameUiFormats() {
        Locale.setDefault(Locale.US)

        assertUiFormats()
    }

    private fun assertUiFormats() {
        assertEquals("83", UiNumberFormatter.integer(82.6f))
        assertEquals("82.6", UiNumberFormatter.oneDecimal(82.55f))
        assertEquals("+0.5", UiNumberFormatter.signedOneDecimal(0.5f))
        assertEquals("-0.5", UiNumberFormatter.signedOneDecimal(-0.5f))
        assertEquals("500 Hz", UiNumberFormatter.frequency(500f))
        assertEquals("999 Hz", UiNumberFormatter.frequency(999.9f))
        assertEquals("1.0 kHz", UiNumberFormatter.frequency(1_000f))
        assertEquals("83%", UiNumberFormatter.percent(82.6f))
        assertEquals("1.5 KB", UiNumberFormatter.fileSize(1_536L))
        assertEquals("1.5 MB", UiNumberFormatter.fileSize(1_572_864L))
        assertEquals("512 B", UiNumberFormatter.fileSize(512L))
    }
}
