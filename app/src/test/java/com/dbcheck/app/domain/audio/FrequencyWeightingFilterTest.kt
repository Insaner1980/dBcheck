package com.dbcheck.app.domain.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class FrequencyWeightingFilterTest {
    private val decibelCalculator = DecibelCalculator()

    @Test
    fun aWeightingMatchesReferenceResponse() {
        assertReferenceResponse(WeightingType.A, A_WEIGHTING_REFERENCES)
    }

    @Test
    fun bWeightingMatchesReferenceResponse() {
        assertReferenceResponse(WeightingType.B, B_WEIGHTING_REFERENCES)
    }

    @Test
    fun cWeightingMatchesReferenceResponse() {
        assertReferenceResponse(WeightingType.C, C_WEIGHTING_REFERENCES)
    }

    @Test
    fun itur468WeightingMatchesReferenceResponse() {
        assertReferenceResponse(WeightingType.ITUR468, ITU_R_468_REFERENCES)
    }

    @Test
    fun zWeightingReturnsSourceSamplesAsDoubles() {
        val source = shortArrayOf(-2, 0, 3, Short.MAX_VALUE)

        assertArrayEquals(
            doubleArrayOf(-2.0, 0.0, 3.0, Short.MAX_VALUE.toDouble()),
            FrequencyWeightingFilter().applyWeighting(source, source.size, WeightingType.Z),
            0.0,
        )
    }

    @Test
    fun weightingStateCarriesAcrossChunks() {
        val source = sineWave(frequencyHz = 1000.0)
        val wholeBuffer = FrequencyWeightingFilter().applyWeighting(source, source.size, WeightingType.A)
        val chunked = filterInChunks(source, WeightingType.A)

        val wholeDelta = transientFreeDeltaDb(source.asDoubleArray(), wholeBuffer)
        val chunkedDelta = transientFreeDeltaDb(source.asDoubleArray(), chunked)

        assertEquals(wholeDelta, chunkedDelta, 0.01)
    }

    @Test
    fun resetClearsFilterHistory() {
        val source = sineWave(frequencyHz = 1000.0)
        val filter = FrequencyWeightingFilter()

        val first = filter.applyWeighting(source, source.size, WeightingType.A)
        filter.reset()
        val second = filter.applyWeighting(source, source.size, WeightingType.A)

        assertArrayEquals(first, second, 0.0)
    }

    @Test
    fun boostedItur468SignalDoesNotClipAtPcm16Range() {
        val source = sineWave(frequencyHz = 6300.0, amplitude = 10_000)
        val weighted = FrequencyWeightingFilter().applyWeighting(source, source.size, WeightingType.ITUR468)
        val delta = transientFreeDeltaDb(source.asDoubleArray(), weighted)

        assertEquals(12.2, delta, 0.3)
        assert(weighted.maxOf { kotlin.math.abs(it) } > Short.MAX_VALUE)
    }

    private fun assertReferenceResponse(weighting: WeightingType, references: List<ReferencePoint>) {
        references.forEach { reference ->
            assertEquals(
                "${weighting.name} weighting at ${reference.frequencyHz} Hz",
                reference.expectedDeltaDb,
                weightedDeltaDb(reference.frequencyHz, weighting),
                reference.toleranceDb,
            )
        }
    }

    private fun weightedDeltaDb(frequencyHz: Double, weighting: WeightingType): Double {
        val source = sineWave(frequencyHz)
        val weighted = filterInChunks(source, weighting)

        return transientFreeDeltaDb(source.asDoubleArray(), weighted)
    }

    private fun transientFreeDeltaDb(source: DoubleArray, weighted: DoubleArray): Double {
        val transientFreeSource = source.copyOfRange(TRANSIENT_SAMPLES, source.size)
        val transientFreeWeighted = weighted.copyOfRange(TRANSIENT_SAMPLES, weighted.size)

        return decibelCalculator.calculateDb(transientFreeWeighted, transientFreeWeighted.size).toDouble() -
            decibelCalculator.calculateDb(transientFreeSource, transientFreeSource.size).toDouble()
    }

    private fun filterInChunks(source: ShortArray, weighting: WeightingType): DoubleArray {
        val filter = FrequencyWeightingFilter()
        val weighted = DoubleArray(source.size)
        var offset = 0
        while (offset < source.size) {
            val chunkSize = minOf(CHUNK_SIZE, source.size - offset)
            val chunk = source.copyOfRange(offset, offset + chunkSize)
            val filtered = filter.applyWeighting(chunk, chunkSize, weighting)
            System.arraycopy(filtered, 0, weighted, offset, chunkSize)
            offset += chunkSize
        }
        return weighted
    }

    private fun sineWave(frequencyHz: Double, amplitude: Int = AMPLITUDE): ShortArray =
        ShortArray(SAMPLE_RATE * DURATION_SECONDS) { index ->
            (sin(2.0 * PI * frequencyHz * index / SAMPLE_RATE) * amplitude)
                .toInt()
                .toShort()
        }

    private fun ShortArray.asDoubleArray(): DoubleArray = DoubleArray(size) { index -> this[index].toDouble() }

    private data class ReferencePoint(
        val frequencyHz: Double,
        val expectedDeltaDb: Double,
        val toleranceDb: Double = 0.6,
    )

    private companion object {
        const val SAMPLE_RATE = 44100
        const val DURATION_SECONDS = 3
        const val TRANSIENT_SAMPLES = SAMPLE_RATE
        const val CHUNK_SIZE = AudioProcessingConfig.CHUNK_SIZE
        const val AMPLITUDE = 2000

        val A_WEIGHTING_REFERENCES =
            listOf(
                ReferencePoint(20.0, -50.4, 0.8),
                ReferencePoint(31.5, -39.5),
                ReferencePoint(63.0, -26.2),
                ReferencePoint(100.0, -19.1),
                ReferencePoint(125.0, -16.2),
                ReferencePoint(250.0, -8.7),
                ReferencePoint(500.0, -3.2),
                ReferencePoint(1000.0, 0.0),
                ReferencePoint(2000.0, 1.2),
                ReferencePoint(4000.0, 1.0),
                ReferencePoint(8000.0, -1.1),
                ReferencePoint(10000.0, -2.5),
                ReferencePoint(12500.0, -4.3),
                ReferencePoint(16000.0, -6.7),
                ReferencePoint(20000.0, -9.3, 0.8),
            )

        val B_WEIGHTING_REFERENCES =
            listOf(
                ReferencePoint(20.0, -24.2),
                ReferencePoint(31.5, -17.1),
                ReferencePoint(63.0, -9.4),
                ReferencePoint(100.0, -5.6),
                ReferencePoint(125.0, -4.2),
                ReferencePoint(250.0, -1.4),
                ReferencePoint(500.0, -0.3),
                ReferencePoint(1000.0, 0.0),
                ReferencePoint(2000.0, -0.1),
                ReferencePoint(4000.0, -0.7),
                ReferencePoint(8000.0, -2.9),
                ReferencePoint(10000.0, -4.3),
                ReferencePoint(12500.0, -6.1),
                ReferencePoint(16000.0, -8.5),
                ReferencePoint(20000.0, -11.2, 0.8),
            )

        val C_WEIGHTING_REFERENCES =
            listOf(
                ReferencePoint(20.0, -6.2),
                ReferencePoint(31.5, -3.0),
                ReferencePoint(63.0, -0.8),
                ReferencePoint(100.0, -0.3),
                ReferencePoint(125.0, -0.2),
                ReferencePoint(250.0, 0.0),
                ReferencePoint(500.0, 0.0),
                ReferencePoint(1000.0, 0.0),
                ReferencePoint(2000.0, -0.2),
                ReferencePoint(4000.0, -0.8),
                ReferencePoint(8000.0, -3.0),
                ReferencePoint(10000.0, -4.4),
                ReferencePoint(12500.0, -6.2),
                ReferencePoint(16000.0, -8.6),
                ReferencePoint(20000.0, -11.3, 0.8),
            )

        val ITU_R_468_REFERENCES =
            listOf(
                ReferencePoint(31.5, -29.9),
                ReferencePoint(63.0, -23.9),
                ReferencePoint(100.0, -19.8),
                ReferencePoint(200.0, -13.8),
                ReferencePoint(400.0, -7.8),
                ReferencePoint(800.0, -1.9),
                ReferencePoint(1000.0, 0.0),
                ReferencePoint(2000.0, 5.6),
                ReferencePoint(3150.0, 9.0),
                ReferencePoint(4000.0, 10.5),
                ReferencePoint(5000.0, 11.7),
                ReferencePoint(6300.0, 12.2),
                ReferencePoint(7100.0, 12.0),
                ReferencePoint(8000.0, 11.4),
                ReferencePoint(9000.0, 10.1),
                ReferencePoint(10000.0, 8.1),
                ReferencePoint(12500.0, 0.0),
                ReferencePoint(14000.0, -5.3),
                ReferencePoint(16000.0, -11.7),
                ReferencePoint(20000.0, -22.2, 0.8),
            )
    }
}
