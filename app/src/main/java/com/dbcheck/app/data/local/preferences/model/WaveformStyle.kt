package com.dbcheck.app.data.local.preferences.model

enum class WaveformStyle(
    val preferenceValue: String,
    val displayName: String,
) {
    LINE("default", "Line"),
    FILLED("filled", "Filled"),
    BARS("bars", "Bars"),
    ;

    companion object {
        fun fromPreference(value: String?): WaveformStyle =
            entries.firstOrNull { it.preferenceValue == value } ?: LINE
    }
}
