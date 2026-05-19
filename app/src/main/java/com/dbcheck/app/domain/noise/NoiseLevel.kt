package com.dbcheck.app.domain.noise

enum class NoiseLevel(val minDb: Float, val maxDb: Float) {
    QUIET(0f, 40f),
    NORMAL(40f, 70f),
    ELEVATED(70f, 85f),
    DANGEROUS(85f, Float.MAX_VALUE),
    ;

    companion object {
        fun fromDb(db: Float): NoiseLevel = entries.firstOrNull { db < it.maxDb } ?: DANGEROUS
    }
}
