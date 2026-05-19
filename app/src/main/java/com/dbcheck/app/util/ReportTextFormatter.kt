package com.dbcheck.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportTextFormatter {
    fun oneDecimal(value: Float): String = "%.1f".format(Locale.getDefault(), value)

    fun oneDecimalOrUnavailable(value: Float?, suffix: String, unavailableLabel: String): String =
        value?.let { "${oneDecimal(it)}$suffix" } ?: unavailableLabel

    fun duration(durationMs: Long): String = DurationFormatter.formatClockDuration(durationMs)

    fun dateTime(timestampMs: Long, pattern: String, locale: Locale = Locale.getDefault()): String =
        SimpleDateFormat(pattern, locale).format(Date(timestampMs))

    fun dateRange(startTimeMs: Long, endTimeMs: Long, pattern: String, locale: Locale = Locale.getDefault()): String {
        val dateFormat = SimpleDateFormat(pattern, locale)
        return "${dateFormat.format(Date(startTimeMs))} - ${dateFormat.format(Date(endTimeMs))}"
    }
}
