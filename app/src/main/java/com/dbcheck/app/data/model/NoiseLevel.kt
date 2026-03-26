package com.dbcheck.app.data.model

enum class NoiseLevel(
    val label: String,
    val minDb: Float,
    val maxDb: Float,
) {
    QUIET("Whisper", 0f, 40f),
    NORMAL("Normal Conversation", 40f, 70f),
    ELEVATED("Busy / Elevated", 70f, 85f),
    DANGEROUS("Dangerous", 85f, Float.MAX_VALUE);

    companion object {
        fun fromDb(db: Float): NoiseLevel = when {
            db < 40f -> QUIET
            db < 70f -> NORMAL
            db < 85f -> ELEVATED
            else -> DANGEROUS
        }
    }
}
