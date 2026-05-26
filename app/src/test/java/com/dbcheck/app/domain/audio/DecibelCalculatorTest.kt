package com.dbcheck.app.domain.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.log10

class DecibelCalculatorTest {
    private val calculator = DecibelCalculator()

    @Test
    fun calculateDbUsesRmsAmplitudeAndCalibrationOffset() {
        val buffer = shortArrayOf(1_000, -1_000, 1_000, -1_000)
        val expected = dbForAmplitude(1_000.0) + CALIBRATION_OFFSET

        assertEquals(
            expected,
            calculator.calculateDb(buffer, buffer.size, CALIBRATION_OFFSET),
            0.001f,
        )
    }

    @Test
    fun calculateDbAcceptsWeightedSamplesOutsidePcm16Range() {
        val buffer = doubleArrayOf(40_000.0, -40_000.0, 40_000.0, -40_000.0)

        assertEquals(dbForAmplitude(40_000.0), calculator.calculateDb(buffer, buffer.size), 0.001f)
    }

    @Test
    fun calculateDbReturnsZeroForDigitalSilence() {
        assertEquals(0f, calculator.calculateDb(shortArrayOf(0, 0, 0, 0), 4), 0.001f)
        assertEquals(0f, calculator.calculateDb(shortArrayOf(1, 0, 0, 0), 4), 0.001f)
        assertEquals(0f, calculator.calculateDb(shortArrayOf(1, 1, 1, 1), 0), 0.001f)
    }

    @Test
    fun calculatePeakDbUsesPeakAmplitudeInsteadOfRms() {
        val buffer = shortArrayOf(1_000, 0, -1_000, 0)
        val peakDb = calculator.calculatePeakDb(buffer, buffer.size)
        val rmsDb = calculator.calculateDb(buffer, buffer.size)

        assertEquals(dbForAmplitude(1_000.0), peakDb, 0.001f)
        assertTrue(peakDb > rmsDb)
    }

    @Test
    fun calculatePeakDbAcceptsWeightedSamplesOutsidePcm16Range() {
        val buffer = doubleArrayOf(1_000.0, 0.0, -40_000.0, 0.0)

        assertEquals(dbForAmplitude(40_000.0), calculator.calculatePeakDb(buffer, buffer.size), 0.001f)
    }

    @Test
    fun calculatePeakDbClampsToMeterRangeAfterCalibration() {
        val buffer = shortArrayOf(Short.MAX_VALUE)

        assertEquals(130f, calculator.calculatePeakDb(buffer, buffer.size, calibrationOffset = 60f), 0.001f)
    }

    @Test
    fun findPeakAmplitudeReturnsNormalizedLargestAbsoluteSample() {
        val buffer = shortArrayOf(-123, 0, Short.MAX_VALUE, Short.MIN_VALUE)

        assertEquals(
            kotlin.math.abs(Short.MIN_VALUE.toInt()).toFloat() / Short.MAX_VALUE,
            calculator.findPeakAmplitude(buffer, buffer.size),
            0.001f,
        )
    }

    @Test
    fun findPeakAmplitudeReturnsZeroForEmptyRead() {
        assertEquals(0f, calculator.findPeakAmplitude(shortArrayOf(123), 0), 0.001f)
    }

    private fun dbForAmplitude(amplitude: Double): Float =
        (20.0 * log10(amplitude / PCM_REFERENCE) + SPL_OFFSET).toFloat()

    private companion object {
        const val PCM_REFERENCE = 32768.0
        const val SPL_OFFSET = 90.0
        const val CALIBRATION_OFFSET = 2.5f
    }
}
