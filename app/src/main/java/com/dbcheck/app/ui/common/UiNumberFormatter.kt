package com.dbcheck.app.ui.common

import java.util.Locale

/**
 * Formats user-visible measurement values with the UI's English-first decimal convention.
 *
 * Dates, times, user metadata, and exported content intentionally use their existing formatters.
 */
object UiNumberFormatter {
    fun integer(value: Float): String = String.format(Locale.US, "%.0f", value)

    fun integer(value: Double): String = String.format(Locale.US, "%.0f", value)

    fun integer(value: Int): String = value.toString()

    fun oneDecimal(value: Float): String = String.format(Locale.US, "%.1f", value)

    fun oneDecimal(value: Double): String = String.format(Locale.US, "%.1f", value)

    fun oneDecimalOrUnavailable(value: Float?, suffix: String, unavailableLabel: String): String =
        value?.let { "${oneDecimal(it)}$suffix" } ?: unavailableLabel

    fun signedOneDecimal(value: Float): String = String.format(Locale.US, "%+.1f", value)

    fun frequency(frequencyHz: Float): String = if (frequencyHz >= HERTZ_PER_KILOHERTZ) {
            "${oneDecimal(frequencyHz / HERTZ_PER_KILOHERTZ)} kHz"
        } else {
            "${integer(frequencyHz.toInt())} Hz"
        }

    fun percent(value: Float): String = "${integer(value)}%"

    fun percent(value: Int): String = "${integer(value)}%"

    fun fileSize(sizeBytes: Long): String = when {
            sizeBytes >= BYTES_PER_MEBIBYTE ->
                "${oneDecimal(sizeBytes.toDouble() / BYTES_PER_MEBIBYTE)} MB"

            sizeBytes >= BYTES_PER_KIBIBYTE ->
                "${oneDecimal(sizeBytes.toDouble() / BYTES_PER_KIBIBYTE)} KB"

            else -> "$sizeBytes B"
        }

    private const val HERTZ_PER_KILOHERTZ = 1_000f
    private const val BYTES_PER_KIBIBYTE = 1_024.0
    private const val BYTES_PER_MEBIBYTE = 1_024.0 * 1_024.0
}
