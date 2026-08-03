package com.dbcheck.app.ui.analytics.components

import com.dbcheck.app.ui.common.UiNumberFormatter

internal fun formatSpectralFrequency(frequencyHz: Float): String = when {
    frequencyHz <= 0f -> "--"
    else -> UiNumberFormatter.frequency(frequencyHz)
}
