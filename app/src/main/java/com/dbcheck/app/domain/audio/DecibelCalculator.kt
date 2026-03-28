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

        fun calculateDb(
            buffer: ShortArray,
            size: Int,
            calibrationOffset: Float = 0f,
        ): Float {
            if (size == 0) return MIN_DB

            var sum = 0.0
            for (i in 0 until size) {
                val sample = buffer[i].toDouble()
                sum += sample * sample
            }

            val rms = sqrt(sum / size)
            if (rms < 1.0) return MIN_DB

            val db = (20.0 * log10(rms / REFERENCE) + SPL_OFFSET + calibrationOffset).toFloat()
            return db.coerceIn(MIN_DB, MAX_DB)
        }

        fun findPeakAmplitude(
            buffer: ShortArray,
            size: Int,
        ): Float {
            var peak = 0
            for (i in 0 until size) {
                val abs = kotlin.math.abs(buffer[i].toInt())
                if (abs > peak) peak = abs
            }
            return peak.toFloat() / Short.MAX_VALUE
        }
    }
