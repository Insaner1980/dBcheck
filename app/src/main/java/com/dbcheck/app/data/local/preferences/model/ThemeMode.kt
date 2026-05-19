package com.dbcheck.app.data.local.preferences.model

enum class ThemeMode(val preferenceValue: String) {
    SYSTEM("system"),
    DARK("dark"),
    LIGHT("light"),
    ;

    companion object {
        fun fromPreference(value: String?): ThemeMode = entries.firstOrNull { it.preferenceValue == value } ?: SYSTEM
    }
}
