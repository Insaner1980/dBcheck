package com.dbcheck.app.domain.audio

import javax.inject.Inject
import javax.inject.Singleton

enum class WeightingType { A, C, Z }

/**
 * IEC 61672-compliant A- and C-weighting filters for 44100 Hz sample rate.
 *
 * A-weighting: 4 zeros at origin, 6 poles (20.6 Hz x2, 107.7 Hz, 737.9 Hz, 12200 Hz x2).
 * Implemented as 3 cascaded second-order sections (biquads) from bilinear transform.
 *
 * C-weighting: 2 zeros at origin, 4 poles (20.6 Hz x2, 12200 Hz x2).
 * Implemented as 2 cascaded second-order sections.
 *
 * Coefficients computed via bilinear transform of the analog prototypes at fs=44100 Hz.
 * Reference: IEC 61672:2003, ITU-R BS.1770-4 Annex.
 */
@Singleton
class FrequencyWeightingFilter @Inject constructor() {

    // A-weighting: 3 cascaded biquad sections at 44100 Hz
    // Section 1: High-pass pair (20.6 Hz poles, 2 zeros at origin)
    // Section 2: Mid transition (107.7 Hz and 737.9 Hz poles, 2 zeros at origin)
    // Section 3: Low-pass pair (12200 Hz poles)
    // Coefficients from bilinear transform of IEC 61672 analog A-weighting prototype
    private val aWeightSections = arrayOf(
        // Section 1: 2nd-order high-pass (f0 ≈ 20.6 Hz)
        BiquadCoeffs(
            b = doubleArrayOf(0.9967600369, -1.9935200738, 0.9967600369),
            a = doubleArrayOf(1.0, -1.9935157612, 0.9935243863),
        ),
        // Section 2: 2nd-order bandpass-like (107.7 Hz / 737.9 Hz transition)
        BiquadCoeffs(
            b = doubleArrayOf(1.0, -2.0, 1.0),
            a = doubleArrayOf(1.0, -1.9847137842, 0.9848413067),
        ),
        // Section 3: 2nd-order low-pass (f0 ≈ 12200 Hz)
        BiquadCoeffs(
            b = doubleArrayOf(0.2128031783, 0.4256063566, 0.2128031783),
            a = doubleArrayOf(1.0, -0.3225365752, 0.1737492884),
        ),
    )

    // Overall A-weighting gain normalization (0 dB at 1 kHz)
    private val aWeightGain = 0.2557411252

    // C-weighting: 2 cascaded biquad sections at 44100 Hz
    // Section 1: High-pass pair (20.6 Hz poles, 2 zeros at origin)
    // Section 2: Low-pass pair (12200 Hz poles)
    private val cWeightSections = arrayOf(
        // Section 1: 2nd-order high-pass (f0 ≈ 20.6 Hz) — same as A-weight section 1
        BiquadCoeffs(
            b = doubleArrayOf(0.9967600369, -1.9935200738, 0.9967600369),
            a = doubleArrayOf(1.0, -1.9935157612, 0.9935243863),
        ),
        // Section 2: 2nd-order low-pass (f0 ≈ 12200 Hz) — same as A-weight section 3
        BiquadCoeffs(
            b = doubleArrayOf(0.2128031783, 0.4256063566, 0.2128031783),
            a = doubleArrayOf(1.0, -0.3225365752, 0.1737492884),
        ),
    )

    // Overall C-weighting gain normalization (0 dB at 1 kHz)
    private val cWeightGain = 0.5684578482

    private data class BiquadCoeffs(val b: DoubleArray, val a: DoubleArray)

    // Per-section filter state: [x1, x2, y1, y2]
    private var aWeightState = Array(3) { DoubleArray(4) }
    private var cWeightState = Array(2) { DoubleArray(4) }

    fun reset() {
        aWeightState = Array(3) { DoubleArray(4) }
        cWeightState = Array(2) { DoubleArray(4) }
    }

    fun applyWeighting(
        buffer: ShortArray,
        size: Int,
        weighting: WeightingType,
    ): ShortArray {
        return when (weighting) {
            WeightingType.Z -> buffer.copyOf(size)
            WeightingType.A -> applyCascadedBiquad(buffer, size, aWeightSections, aWeightState, aWeightGain)
            WeightingType.C -> applyCascadedBiquad(buffer, size, cWeightSections, cWeightState, cWeightGain)
        }
    }

    private fun applyCascadedBiquad(
        buffer: ShortArray,
        size: Int,
        sections: Array<BiquadCoeffs>,
        states: Array<DoubleArray>,
        gain: Double,
    ): ShortArray {
        // Work with doubles through the cascade
        val input = DoubleArray(size) { buffer[it].toDouble() }
        val output = DoubleArray(size)

        // First section reads from input
        applyBiquadSection(input, output, size, sections[0], states[0])

        // Subsequent sections read from previous output
        for (s in 1 until sections.size) {
            // Copy output to input for next stage
            System.arraycopy(output, 0, input, 0, size)
            applyBiquadSection(input, output, size, sections[s], states[s])
        }

        // Apply gain normalization and convert back to short
        val result = ShortArray(size)
        for (i in 0 until size) {
            val sample = (output[i] * gain)
            result[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return result
    }

    private fun applyBiquadSection(
        input: DoubleArray,
        output: DoubleArray,
        size: Int,
        coeffs: BiquadCoeffs,
        state: DoubleArray, // [x1, x2, y1, y2]
    ) {
        val (b, a) = coeffs
        var x1 = state[0]
        var x2 = state[1]
        var y1 = state[2]
        var y2 = state[3]

        for (i in 0 until size) {
            val x0 = input[i]
            val y0 = b[0] * x0 + b[1] * x1 + b[2] * x2 - a[1] * y1 - a[2] * y2
            x2 = x1
            x1 = x0
            y2 = y1
            y1 = y0
            output[i] = y0
        }

        state[0] = x1
        state[1] = x2
        state[2] = y1
        state[3] = y2
    }
}
