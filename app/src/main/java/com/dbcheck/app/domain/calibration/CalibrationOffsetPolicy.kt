package com.dbcheck.app.domain.calibration

object CalibrationOffsetPolicy {
    const val MIN_OFFSET_DB = -10f
    const val MAX_OFFSET_DB = 10f
    const val DEFAULT_OFFSET_DB = 0f

    fun normalizeOffsetDb(offsetDb: Float?): Float = offsetDb
        ?.takeIf { it.isFinite() }
        ?.coerceIn(MIN_OFFSET_DB, MAX_OFFSET_DB)
        ?: DEFAULT_OFFSET_DB
}
