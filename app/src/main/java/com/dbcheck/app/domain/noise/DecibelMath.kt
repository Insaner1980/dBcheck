package com.dbcheck.app.domain.noise

import kotlin.math.log10
import kotlin.math.pow

object DecibelMath {
    fun energyAverageDb(values: Iterable<Float>): Float? {
        var count = 0
        var totalEnergy = 0.0

        values.forEach { db ->
            totalEnergy += energyFromDb(db)
            count += 1
        }

        return energyAverageDb(totalEnergy, count)
    }

    fun energyAverageDb(totalEnergy: Double, count: Int): Float? = if (count == 0) {
            null
        } else {
            (DB_POWER_DIVISOR * log10(totalEnergy / count)).toFloat()
        }

    fun energyFromDb(db: Float): Double = DB_POWER_DIVISOR.pow(db / DB_POWER_DIVISOR)

    private const val DB_POWER_DIVISOR = 10.0
}
