package com.dbcheck.app.domain.noise

import kotlin.math.log10
import kotlin.math.pow

object DecibelMath {
    fun energyAverageDb(values: Iterable<Float>): Float? {
        var count = 0
        var totalEnergy = 0.0

        values.forEach { db ->
            totalEnergy += DB_POWER_DIVISOR.pow(db / DB_POWER_DIVISOR)
            count += 1
        }

        return if (count == 0) {
            null
        } else {
            (DB_POWER_DIVISOR * log10(totalEnergy / count)).toFloat()
        }
    }

    private const val DB_POWER_DIVISOR = 10.0
}
