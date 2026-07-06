package com.dbcheck.app.domain.ambient

enum class AmbientSoundPreset(val preferenceValue: String) {
    WHITE_NOISE("WHITE_NOISE"),
    PINK_NOISE("PINK_NOISE"),
    BROWN_NOISE("BROWN_NOISE"),
    FAN("FAN"),
    ;

    companion object {
        fun fromPreference(value: String?): AmbientSoundPreset =
            entries.firstOrNull { it.preferenceValue == value } ?: WHITE_NOISE
    }
}

object AmbientSoundPolicy {
    val DEFAULT_PRESET = AmbientSoundPreset.WHITE_NOISE
    const val MIN_VOLUME = 0.05f
    const val MAX_VOLUME = 1f
    const val DEFAULT_VOLUME = 0.35f
    const val DEFAULT_TIMER_MINUTES = 30
    val TIMER_OPTIONS_MINUTES = listOf(0, 15, 30, 60, 120)

    fun normalizePreset(value: String?): AmbientSoundPreset = AmbientSoundPreset.fromPreference(value)

    fun normalizeVolume(volume: Float?): Float = volume
            ?.takeIf { it.isFinite() }
            ?.coerceIn(MIN_VOLUME, MAX_VOLUME)
            ?: DEFAULT_VOLUME

    fun normalizeTimerMinutes(minutes: Int?): Int = minutes
            ?.takeIf { it in TIMER_OPTIONS_MINUTES }
            ?: DEFAULT_TIMER_MINUTES
}
