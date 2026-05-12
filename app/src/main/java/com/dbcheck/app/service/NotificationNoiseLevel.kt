package com.dbcheck.app.service

import com.dbcheck.app.domain.noise.NoiseLevel

enum class NotificationNoiseLevel {
    SAFE,
    ELEVATED,
    DANGEROUS,
    ;

    companion object {
        fun fromDb(db: Float): NotificationNoiseLevel =
            when (NoiseLevel.fromDb(db)) {
                NoiseLevel.QUIET,
                NoiseLevel.NORMAL,
                -> SAFE
                NoiseLevel.ELEVATED -> ELEVATED
                NoiseLevel.DANGEROUS -> DANGEROUS
            }
    }
}
