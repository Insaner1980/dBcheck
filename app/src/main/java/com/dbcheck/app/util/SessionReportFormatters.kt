package com.dbcheck.app.util

import com.dbcheck.app.domain.report.PeakEvent
import com.dbcheck.app.domain.report.SessionReportData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun SessionReportData.dateRangeLabel(pattern: String): String {
    val dateFormat = SimpleDateFormat(pattern, Locale.getDefault())
    return "${dateFormat.format(Date(startTime))} - ${dateFormat.format(Date(endTime))}"
}

internal fun SessionReportData.durationLabel(): String = DurationFormatter.formatClockDuration(durationMs)

internal fun PeakEvent.timeLabel(pattern: String = "HH:mm:ss"): String {
    val timeFormat = SimpleDateFormat(pattern, Locale.getDefault())
    return timeFormat.format(Date(peakTime))
}

internal fun Float.formatOne(): String = "%.1f".format(Locale.US, this)

internal fun Float?.formatOneOrUnavailable(suffix: String): String = this?.let { "${it.formatOne()}$suffix" } ?: "N/A"
