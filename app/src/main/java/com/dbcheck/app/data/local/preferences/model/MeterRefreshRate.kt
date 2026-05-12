package com.dbcheck.app.data.local.preferences.model

enum class MeterRefreshRate(
    val preferenceValue: String,
    val displayName: String,
    val uiIntervalMs: Long,
    val persistenceIntervalMs: Long,
) {
    HIGH("high", "High", uiIntervalMs = 100L, persistenceIntervalMs = 100L),
    STANDARD("standard", "Standard", uiIntervalMs = 250L, persistenceIntervalMs = 1_000L),
    LOW("low", "Low power", uiIntervalMs = 1_000L, persistenceIntervalMs = 5_000L),
    ;

    companion object {
        fun fromPreference(value: String?): MeterRefreshRate =
            entries.firstOrNull { it.preferenceValue == value } ?: STANDARD
    }
}
