package com.dbcheck.app.domain.noise

enum class NoiseLevel(
    val label: String,
    val minDb: Float,
    val maxDb: Float,
) {
    QUIET("Whisper", 0f, 40f),
    NORMAL("Normal Conversation", 40f, 70f),
    ELEVATED("Busy / Elevated", 70f, 85f),
    DANGEROUS("Dangerous", 85f, Float.MAX_VALUE),
    ;

    companion object {
        fun fromDb(db: Float): NoiseLevel =
            entries.firstOrNull { db < it.maxDb } ?: DANGEROUS
    }
}
