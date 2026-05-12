package com.dbcheck.app.domain.noise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.log10
import kotlin.math.pow

class DecibelMathTest {
    @Test
    fun energyAverageDbUsesSoundEnergyAverage() {
        val expected = 10.0 * log10((10.0.pow(6.0) + 10.0.pow(7.0)) / 2.0)

        assertEquals(
            expected.toFloat(),
            DecibelMath.energyAverageDb(listOf(60f, 70f)) ?: 0f,
            0.001f,
        )
    }

    @Test
    fun energyAverageDbReturnsNullForEmptyInput() {
        assertNull(DecibelMath.energyAverageDb(emptyList()))
    }
}
