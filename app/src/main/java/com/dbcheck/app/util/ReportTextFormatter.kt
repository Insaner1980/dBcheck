package com.dbcheck.app.util

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

object ReportTextFormatter {
    fun oneDecimal(value: Float): String = "%.1f".format(Locale.getDefault(), value)

    fun oneDecimalOrUnavailable(value: Float?, suffix: String, unavailableLabel: String): String =
        value?.let { "${oneDecimal(it)}$suffix" } ?: unavailableLabel

    fun duration(durationMs: Long): String = DurationFormatter.formatClockDuration(durationMs)

    fun dateTime(
        timestampMs: Long,
        pattern: String,
        locale: Locale = Locale.getDefault(),
        utcOffsetSeconds: Int? = null,
    ): String {
        val offset = utcOffsetSeconds.toZoneOffset()
        val dateTime =
            DateTimeFormatter
                .ofPattern(pattern, locale)
                .withZone(offset)
                .format(Instant.ofEpochMilli(timestampMs))
        return "$dateTime ${utcOffsetSeconds.utcOffsetLabel(offset)}"
    }

    fun dateRange(
        startTimeMs: Long,
        endTimeMs: Long,
        pattern: String,
        locale: Locale = Locale.getDefault(),
        startUtcOffsetSeconds: Int? = null,
        endUtcOffsetSeconds: Int? = null,
    ): String = "${dateTime(startTimeMs, pattern, locale, startUtcOffsetSeconds)} - " +
            dateTime(endTimeMs, pattern, locale, endUtcOffsetSeconds)

    private fun Int?.toZoneOffset(): ZoneOffset = this?.let(ZoneOffset::ofTotalSeconds) ?: ZoneOffset.UTC

    private fun Int?.utcOffsetLabel(offset: ZoneOffset): String =
        if (this == null || offset == ZoneOffset.UTC) "UTC" else "UTC${offset.id}"
}
