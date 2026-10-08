package com.dbcheck.app.domain.noise

import org.junit.Assert.assertEquals
import org.junit.Test

class NoiseLevelBoundaryTest {
    @Test
    fun classifiesValuesAtEveryLevelBoundary() {
        assertEquals(NoiseLevel.QUIET, NoiseLevel.fromDb(39.9f))
        assertEquals(NoiseLevel.NORMAL, NoiseLevel.fromDb(40.0f))
        assertEquals(NoiseLevel.NORMAL, NoiseLevel.fromDb(69.9f))
        assertEquals(NoiseLevel.ELEVATED, NoiseLevel.fromDb(70.0f))
        assertEquals(NoiseLevel.ELEVATED, NoiseLevel.fromDb(84.9f))
        assertEquals(NoiseLevel.DANGEROUS, NoiseLevel.fromDb(85.0f))
    }
}
