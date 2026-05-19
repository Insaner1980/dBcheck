package com.dbcheck.app.data.local.preferences.model

enum class MeterRefreshRate(val preferenceValue: String, val uiIntervalMs: Long) {
    HIGH("high", uiIntervalMs = 100L),
    STANDARD("standard", uiIntervalMs = 250L),
    LOW("low", uiIntervalMs = 1_000L),
    ;

    companion object {
        fun fromPreference(value: String?): MeterRefreshRate =
            entries.firstOrNull { it.preferenceValue == value } ?: STANDARD
    }
}
