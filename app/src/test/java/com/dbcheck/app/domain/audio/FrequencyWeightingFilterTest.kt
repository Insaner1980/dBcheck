package com.dbcheck.app.domain.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class FrequencyWeightingFilterTest {
    private val decibelCalculator = DecibelCalculator()

    @Test
    fun itur468WeightingMatchesReferenceResponse() {
        assertTrue(WeightingType.entries.contains(WeightingType.ITUR468))

        val oneKhzDelta = weightedDeltaDb(1000.0)
        val sixPointThreeKhzDelta = weightedDeltaDb(6300.0)

        assertEquals(0.0, oneKhzDelta, 0.8)
        assertEquals(12.2, sixPointThreeKhzDelta, 0.8)
    }

    private fun weightedDeltaDb(frequencyHz: Double): Double {
        val source = sineWave(frequencyHz)
        val filter = FrequencyWeightingFilter()
        val weighted = filter.applyWeighting(source, source.size, WeightingType.ITUR468)
        val transientFreeSource = source.copyOfRange(SAMPLE_RATE / 2, source.size)
        val transientFreeWeighted = weighted.copyOfRange(SAMPLE_RATE / 2, weighted.size)

        return decibelCalculator.calculateDb(
            transientFreeWeighted,
            transientFreeWeighted.size,
        ).toDouble() - decibelCalculator.calculateDb(
            transientFreeSource,
            transientFreeSource.size,
        ).toDouble()
    }

    private fun sineWave(frequencyHz: Double): ShortArray =
        ShortArray(SAMPLE_RATE * DURATION_SECONDS) { index ->
            (sin(2.0 * PI * frequencyHz * index / SAMPLE_RATE) * AMPLITUDE)
                .toInt()
                .toShort()
        }

    private companion object {
        const val SAMPLE_RATE = 44100
        const val DURATION_SECONDS = 2
        const val AMPLITUDE = 2000
    }
}
