package com.dbcheck.app.domain.audio

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10
import kotlin.math.sqrt

@Singleton
class DecibelCalculator
    @Inject
    constructor() {
        companion object {
            private const val REFERENCE = 32768.0
            private const val SPL_OFFSET = 90.0
            private const val MIN_DB = 0f
            private const val MAX_DB = 130f
        }

        fun calculateDb(buffer: ShortArray, size: Int, calibrationOffset: Float = 0f): Float =
            dbFromAmplitude(rmsAmplitude(buffer, size), calibrationOffset)

        fun calculateDb(buffer: DoubleArray, size: Int, calibrationOffset: Float = 0f): Float =
            dbFromAmplitude(rmsAmplitude(buffer, size), calibrationOffset)

        fun calculatePeakDb(buffer: ShortArray, size: Int, calibrationOffset: Float = 0f): Float =
            dbFromAmplitude(peakAmplitude(buffer, size), calibrationOffset)

        fun calculatePeakDb(buffer: DoubleArray, size: Int, calibrationOffset: Float = 0f): Float =
            dbFromAmplitude(peakAmplitude(buffer, size), calibrationOffset)

        private fun dbFromAmplitude(amplitude: Double, calibrationOffset: Float): Float = if (amplitude < 1.0) {
                MIN_DB
            } else {
                val db = (20.0 * log10(amplitude / REFERENCE) + SPL_OFFSET + calibrationOffset).toFloat()
                db.coerceIn(MIN_DB, MAX_DB)
            }

        private fun rmsAmplitude(buffer: ShortArray, size: Int): Double {
            if (size == 0) return 0.0

            var sum = 0.0
            for (i in 0 until size) {
                val sample = buffer[i].toDouble()
                sum += sample * sample
            }
            return sqrt(sum / size)
        }

        private fun rmsAmplitude(buffer: DoubleArray, size: Int): Double {
            if (size == 0) return 0.0

            var sum = 0.0
            for (i in 0 until size) {
                val sample = buffer[i]
                sum += sample * sample
            }
            return sqrt(sum / size)
        }

        private fun peakAmplitude(buffer: ShortArray, size: Int): Double {
            var peak = 0
            for (i in 0 until size) {
                val abs = kotlin.math.abs(buffer[i].toInt())
                if (abs > peak) peak = abs
            }
            return peak.toDouble()
        }

        private fun peakAmplitude(buffer: DoubleArray, size: Int): Double {
            var peak = 0.0
            for (i in 0 until size) {
                val abs = kotlin.math.abs(buffer[i])
                if (abs > peak) peak = abs
            }
            return peak
        }

        fun findPeakAmplitude(buffer: ShortArray, size: Int): Float {
            var peak = 0
            for (i in 0 until size) {
                val abs = kotlin.math.abs(buffer[i].toInt())
                if (abs > peak) peak = abs
            }
            return peak.toFloat() / Short.MAX_VALUE
        }
    }
