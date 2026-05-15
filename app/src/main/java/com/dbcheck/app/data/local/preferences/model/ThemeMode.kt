package com.dbcheck.app.data.local.preferences.model

enum class ThemeMode(val preferenceValue: String, val displayName: String) {
    SYSTEM("system", "System"),
    DARK("dark", "Dark"),
    LIGHT("light", "Light"),
    ;

    companion object {
        fun fromPreference(value: String?): ThemeMode = entries.firstOrNull { it.preferenceValue == value } ?: SYSTEM
    }
}
