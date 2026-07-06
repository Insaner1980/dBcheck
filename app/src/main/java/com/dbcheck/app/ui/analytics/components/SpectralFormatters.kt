package com.dbcheck.app.ui.analytics.components

import java.util.Locale

internal fun formatSpectralFrequency(frequencyHz: Float): String = when {
    frequencyHz <= 0f -> "--"
    frequencyHz >= 1000f -> String.format(Locale.US, "%.1f kHz", frequencyHz / 1000f)
    else -> "${frequencyHz.toInt()} Hz"
}
