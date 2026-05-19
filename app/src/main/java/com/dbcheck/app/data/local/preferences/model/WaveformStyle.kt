package com.dbcheck.app.data.local.preferences.model

enum class WaveformStyle(val preferenceValue: String) {
    LINE("default"),
    FILLED("filled"),
    BARS("bars"),
    ;

    companion object {
        fun fromPreference(value: String?): WaveformStyle = entries.firstOrNull { it.preferenceValue == value } ?: LINE
    }
}
