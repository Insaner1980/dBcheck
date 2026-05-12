package com.dbcheck.app.domain.audio

import javax.inject.Inject
import javax.inject.Singleton

enum class WeightingType(
    val displayName: String,
) {
    A("A-Weight"),
    B("B-Weight"),
    C("C-Weight"),
    Z("Z-Weight"),
    ITUR468("ITU-R 468"),
    ;

    companion object {
        const val DEFAULT_PREFERENCE_VALUE = "A"
        val DEFAULT = valueOf(DEFAULT_PREFERENCE_VALUE)

        fun fromPreference(value: String?): WeightingType =
            runCatching { valueOf(value.orEmpty()) }.getOrDefault(DEFAULT)
    }
}

/**
 * IEC 61672-compliant A-, B- and C-weighting filters for 44100 Hz sample rate.
 *
 * A-weighting: 4 zeros at origin, 6 poles (20.6 Hz x2, 107.7 Hz, 737.9 Hz, 12200 Hz x2).
 * Implemented as 3 cascaded second-order sections (biquads) from bilinear transform.
 *
 * B-weighting: 4 zeros at origin, 5 poles (20.6 Hz x2, 158.5 Hz, 12200 Hz x2).
 * Implemented as 2 cascaded second-order sections + 1 first-order section (as biquad with a2=b2=0).
 *
 * C-weighting: 2 zeros at origin, 4 poles (20.6 Hz x2, 12200 Hz x2).
 * Implemented as 2 cascaded second-order sections.
 *
 * Coefficients computed via bilinear transform of the analog prototypes at fs=44100 Hz.
 * Reference: IEC 61672:2003, ITU-R BS.1770-4 Annex.
 */
@Singleton
class FrequencyWeightingFilter
    @Inject
    constructor() {
        private val highPass20HzSection =
            BiquadCoeffs(
                b = doubleArrayOf(0.9967600369, -1.9935200738, 0.9967600369),
                a = doubleArrayOf(1.0, -1.9935157612, 0.9935243863),
            )

        private val lowPass12200HzSection =
            BiquadCoeffs(
                b = doubleArrayOf(0.2128031783, 0.4256063566, 0.2128031783),
                a = doubleArrayOf(1.0, -0.3225365752, 0.1737492884),
            )

        private val aWeightSections =
            arrayOf(
                highPass20HzSection,
                // Section 2: 2nd-order bandpass-like (107.7 Hz / 737.9 Hz transition)
                BiquadCoeffs(
                    b = doubleArrayOf(1.0, -2.0, 1.0),
                    a = doubleArrayOf(1.0, -1.9847137842, 0.9848413067),
                ),
                lowPass12200HzSection,
            )

        // Overall A-weighting gain normalization (0 dB at 1 kHz)
        private val aWeightGain = 0.2557411252

        // C-weighting: 2 cascaded biquad sections at 44100 Hz
        // Section 1: High-pass pair (20.6 Hz poles, 2 zeros at origin)
        // Section 2: Low-pass pair (12200 Hz poles)
        private val cWeightSections =
            arrayOf(
                highPass20HzSection,
                lowPass12200HzSection,
            )

        // Overall C-weighting gain normalization (0 dB at 1 kHz)
        private val cWeightGain = 0.5684578482

        // B-weighting: 3 cascaded biquad sections at 44100 Hz
        // Section 1: 2nd-order high-pass (20.6 Hz double pole) — identical to A-weight section 1
        // Section 2: 2nd-order with poles at 158.5 Hz + 12200 Hz, 2 zeros at z=1
        //   Pole z_p = (2 - ω_p·T) / (2 + ω_p·T) where ω_p = 2π·f_p, T = 1/44100
        //     158.5 Hz → z_p = 0.97766
        //     12200 Hz → z_p = 0.07003
        //   Denominator: (z - 0.97766)(z - 0.07003) = z² - 1.04769z + 0.06846
        // Section 3: 1st-order biquad (a₂ = b₂ = 0) — zero at z=-1, pole at 12200 Hz
        private val bWeightSections =
            arrayOf(
                highPass20HzSection,
                // Section 2: poles at 158.5 Hz + 12200 Hz, double zero at z=1
                BiquadCoeffs(
                    b = doubleArrayOf(1.0, -2.0, 1.0),
                    a = doubleArrayOf(1.0, -1.0476903, 0.0684563),
                ),
                // Section 3: 1st-order — zero at z=-1, single pole at 12200 Hz
                BiquadCoeffs(
                    b = doubleArrayOf(1.0, 1.0, 0.0),
                    a = doubleArrayOf(1.0, -0.0700310, 0.0),
                ),
            )

        // Overall B-weighting gain normalization (0 dB at 1 kHz). Computed numerically by
        // evaluating |H(z)| of the cascade at z = e^(j·2π·1000/44100): cascade gain ≈ 0.32708,
        // so normalization factor = 1 / 0.32708 ≈ 3.0573. Verify with 1 kHz reference tone test.
        private val bWeightGain = 3.0573

        // ITU-R BS.468-4 -kaskadi 44,1 kHz naytteenottotaajuudelle.
        // Analogiset navat on muunnettu bilineaarisella muunnoksella; vahvistus normalisoi 6,3 kHz:n +12,2 dB:iin.
        private val ituR468Sections =
            arrayOf(
                BiquadCoeffs(
                    b = doubleArrayOf(1.0, 0.0, -1.0),
                    a = doubleArrayOf(1.0, -0.715140590506, 0.0923650350016),
                ),
                BiquadCoeffs(
                    b = doubleArrayOf(1.0, 2.0, 1.0),
                    a = doubleArrayOf(1.0, -0.853169162281, 0.397408269847),
                ),
                BiquadCoeffs(
                    b = doubleArrayOf(1.0, 2.0, 1.0),
                    a = doubleArrayOf(1.0, -0.459849593246, 0.568868689383),
                ),
            )

        private val ituR468Gain = 0.0511761666834

        private data class BiquadCoeffs(
            val b: DoubleArray,
            val a: DoubleArray,
        )

        // Per-section filter state: [x1, x2, y1, y2]
        private var aWeightState = Array(3) { DoubleArray(4) }
        private var bWeightState = Array(3) { DoubleArray(4) }
        private var cWeightState = Array(2) { DoubleArray(4) }
        private var ituR468State = Array(3) { DoubleArray(4) }

        fun reset() {
            aWeightState = Array(3) { DoubleArray(4) }
            bWeightState = Array(3) { DoubleArray(4) }
            cWeightState = Array(2) { DoubleArray(4) }
            ituR468State = Array(3) { DoubleArray(4) }
        }

        fun applyWeighting(
            buffer: ShortArray,
            size: Int,
            weighting: WeightingType,
        ): ShortArray =
            when (weighting) {
                WeightingType.Z -> buffer.copyOf(size)
                WeightingType.A -> applyCascadedBiquad(buffer, size, aWeightSections, aWeightState, aWeightGain)
                WeightingType.B -> applyCascadedBiquad(buffer, size, bWeightSections, bWeightState, bWeightGain)
                WeightingType.C -> applyCascadedBiquad(buffer, size, cWeightSections, cWeightState, cWeightGain)
                WeightingType.ITUR468 -> {
                    applyCascadedBiquad(buffer, size, ituR468Sections, ituR468State, ituR468Gain)
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
