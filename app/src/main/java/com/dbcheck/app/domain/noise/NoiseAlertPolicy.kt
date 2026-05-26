package com.dbcheck.app.domain.noise

object NoiseAlertPolicy {
    const val EXPOSURE_DURATION_MINUTES = 30
    const val EXPOSURE_DURATION_MS = EXPOSURE_DURATION_MINUTES * 60_000L
    const val PEAK_WARNING_DB = 120f
}
