package com.dbcheck.app.util

import com.dbcheck.app.withDefaultLocale
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

class ReportTextFormatterTest {
    @Test
    fun oneDecimalUsesDefaultLocaleForUserVisibleReportNumbers() {
        withDefaultLocale(Locale.forLanguageTag("fi-FI")) {
            assertEquals("72,3", ReportTextFormatter.oneDecimal(72.34f))
            assertEquals("72,3 dB", ReportTextFormatter.oneDecimalOrUnavailable(72.34f, " dB", "N/A"))
        }
    }

    @Test
    fun unavailableNumberUsesProvidedLabel() {
        assertEquals("N/A", ReportTextFormatter.oneDecimalOrUnavailable(null, " dB", "N/A"))
    }

    @Test
    fun dateTimeAndDateRangeUseProvidedPatternAndLocale() {
        val start = 1_700_000_000_000L
        val end = start + 60_000L

        withDefaultTimeZone(TimeZone.getTimeZone("UTC")) {
            assertEquals("2023-11-14 22:13 UTC", ReportTextFormatter.dateTime(start, DATE_PATTERN, Locale.US))
            assertEquals(
                "2023-11-14 22:13 UTC - 2023-11-14 22:14 UTC",
                ReportTextFormatter.dateRange(start, end, DATE_PATTERN, Locale.US),
            )
        }
    }

    @Test
    fun dateTimeUsesPersistedOffsetInsteadOfCurrentDefaultTimeZone() {
        val timestamp = 1_700_000_000_000L

        withDefaultTimeZone(TimeZone.getTimeZone("America/Los_Angeles")) {
            assertEquals(
                "2023-11-15 00:13 UTC+02:00",
                ReportTextFormatter.dateTime(
                    timestampMs = timestamp,
                    pattern = DATE_PATTERN,
                    locale = Locale.US,
                    utcOffsetSeconds = 2 * 60 * 60,
                ),
            )
        }
    }

    @Test
    fun durationUsesClockDurationFormatter() {
        assertEquals("1:02:03", ReportTextFormatter.duration(3_723_000L))
    }

    private inline fun withDefaultTimeZone(timeZone: TimeZone, block: () -> Unit) {
        val original = TimeZone.getDefault()
        TimeZone.setDefault(timeZone)
        try {
            block()
        } finally {
            TimeZone.setDefault(original)
        }
    }

    private companion object {
        const val DATE_PATTERN = "yyyy-MM-dd HH:mm"
    }
}
