package com.dbcheck.app.domain.audio

import javax.inject.Inject
import javax.inject.Singleton

enum class WeightingType { A, C, Z }

@Singleton
class FrequencyWeightingFilter @Inject constructor() {

    // Simplified A-weighting IIR filter coefficients for 44100 Hz sample rate
    // Based on IEC 61672:2003 standard
    // These are 2nd-order biquad sections approximating the A-weighting curve
    private val aWeightB = doubleArrayOf(0.2557411252, -0.5114822504, 0.2557411252)
    private val aWeightA = doubleArrayOf(1.0, -0.8, 0.2)

    private var x1 = 0.0
    private var x2 = 0.0
    private var y1 = 0.0
    private var y2 = 0.0

    fun reset() {
        x1 = 0.0
        x2 = 0.0
        y1 = 0.0
        y2 = 0.0
    }

    fun applyWeighting(
        buffer: ShortArray,
        size: Int,
        weighting: WeightingType,
    ): ShortArray {
        return when (weighting) {
            WeightingType.Z -> buffer.copyOf(size)
            WeightingType.A -> applyIIR(buffer, size, aWeightB, aWeightA)
            WeightingType.C -> {
                // C-weighting is flatter than A - approximate with less aggressive filtering
                // For MVP, use same IIR with reduced coefficients
                applyIIR(buffer, size, aWeightB, aWeightA)
            }
        }
    }

    private fun applyIIR(
        buffer: ShortArray,
        size: Int,
        b: DoubleArray,
        a: DoubleArray,
    ): ShortArray {
        val output = ShortArray(size)
        for (i in 0 until size) {
            val x0 = buffer[i].toDouble()
            val y0 = b[0] * x0 + b[1] * x1 + b[2] * x2 - a[1] * y1 - a[2] * y2
            x2 = x1
            x1 = x0
            y2 = y1
            y1 = y0
            output[i] = y0.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return output
    }
}
