package com.dbcheck.app.domain.noise

object SoundLevelDisplayScale {
    const val MIN_DB = 0f
    const val MAX_DB = 130f

    fun positionForDb(db: Float): Float {
        val normalizedDb = db.coerceIn(MIN_DB, MAX_DB)
        return (normalizedDb - MIN_DB) / DB_RANGE
    }

    private const val DB_RANGE = MAX_DB - MIN_DB
}
