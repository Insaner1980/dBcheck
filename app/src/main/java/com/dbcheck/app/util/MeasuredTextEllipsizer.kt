package com.dbcheck.app.util

internal fun ellipsizeMeasuredText(text: String, maxWidth: Float, measureText: (String) -> Float): String {
    val normalized = text.replace(Regex("\\s+"), " ").trim()
    val marker = "..."
    return when {
        maxWidth <= 0f -> ""
        measureText(normalized) <= maxWidth -> normalized
        measureText(marker) > maxWidth -> ""
        else -> fitMeasuredText(normalized, marker, maxWidth, measureText)
    }
}

private fun fitMeasuredText(
    normalized: String,
    marker: String,
    maxWidth: Float,
    measureText: (String) -> Float,
): String = (normalized.length downTo 1)
        .asSequence()
        .map { endIndex -> normalized.take(endIndex).trimEnd() + marker }
        .firstOrNull { candidate -> measureText(candidate) <= maxWidth }
        ?: marker
