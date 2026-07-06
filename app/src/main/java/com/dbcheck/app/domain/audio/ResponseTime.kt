package com.dbcheck.app.domain.audio

enum class ResponseTime(val preferenceValue: String, val timeConstantMs: Long) {
    FAST("fast", timeConstantMs = 200L),
    SLOW("slow", timeConstantMs = 500L),
    IMPULSE("impulse", timeConstantMs = 50L),
    ;

    companion object {
        fun fromPreference(value: String?): ResponseTime = entries.firstOrNull { it.preferenceValue == value } ?: FAST
    }
}
