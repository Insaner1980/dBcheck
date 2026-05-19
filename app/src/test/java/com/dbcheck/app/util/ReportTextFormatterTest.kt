package com.dbcheck.app.util

import com.dbcheck.app.withDefaultLocale
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class ReportTextFormatterTest {
    @Test
    fun oneDecimalUsesDefaultLocaleForUserVisibleReportNumbers() {
        withDefaultLocale(Locale.forLanguageTag("fi-FI")) {
            assertEquals("72,3", ReportTextFormatter.oneDecimal(72.34f))
            assertEquals("72,3 dB", ReportTextFormatter.oneDecimalOrUnavailable(72.34f, " dB", "N/A"))
        }
    }
}
