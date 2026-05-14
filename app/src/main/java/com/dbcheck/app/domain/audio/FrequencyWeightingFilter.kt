package com.dbcheck.app.domain.audio

import javax.inject.Inject
import javax.inject.Singleton

enum class WeightingType(val displayName: String) {
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
 * Taajuuspainotukset 44100 Hz naytteenottotaajuudelle.
 *
 * Kertoimet on sovitettu 44.1 kHz:n digitaalisiksi SOS-sektioiksi A-, B-, C- ja
 * ITU-R 468 -referenssivasteita vasten. Painotettu signaali pidetaan DoubleArrayna,
 * jotta positiiviset vahvistukset, kuten ITU-R 468:n +12.2 dB 6.3 kHz:ssa, eivat
 * leikkaudu PCM16-alueelle ennen dB-laskentaa.
 */
@Singleton
class FrequencyWeightingFilter
    @Inject
    constructor() {
        private val aWeightSections =
            arrayOf(
                BiquadCoeffs(
                    b = doubleArrayOf(0.702209147827972, 0.0817274232960434, -0.0645051514019468),
                    a = doubleArrayOf(1.0, -0.388936198544692, 0.00643077042044593),
                ),
                BiquadCoeffs(
                    b = doubleArrayOf(0.99996474866534, -1.99992876773283, 0.999964752000476),
                    a = doubleArrayOf(1.0, -1.88426819780756, 0.885830576419804),
                ),
                BiquadCoeffs(
                    b = doubleArrayOf(0.999964748524881, -1.99992877014185, 0.999964754549928),
                    a = doubleArrayOf(1.0, -1.99548364408126, 0.995487129902554),
                ),
                BiquadCoeffs(
                    b = doubleArrayOf(0.899266928599033, 0.058512906223464, -0.0459705114272575),
                    a = doubleArrayOf(1.0, -0.0745903066698123, -0.0400210368578391),
                ),
            )

        private val bWeightSections =
            arrayOf(
                BiquadCoeffs(
                    b = doubleArrayOf(0.613972679342952, 0.0679395171836271, -0.10303024671223),
                    a = doubleArrayOf(1.0, -0.190302436578997, -0.0264662305509916),
                ),
                BiquadCoeffs(
                    b = doubleArrayOf(0.930568544810063, -0.928996647085138, -0.00163427989656101),
                    a = doubleArrayOf(1.0, -1.18432244154341, 0.201983083906854),
                ),
                BiquadCoeffs(
                    b = doubleArrayOf(0.999881504914685, -1.99976255099875, 0.999881501256589),
                    a = doubleArrayOf(1.0, -1.99446725944912, 0.994474157549316),
                ),
                BiquadCoeffs(
                    b = doubleArrayOf(0.939831666929469, 0.0590944102402415, 0.0114214973504476),
                    a = doubleArrayOf(1.0, -0.0728282125459503, -0.053933932316391),
                ),
            )

        private val cWeightSections =
            arrayOf(
                BiquadCoeffs(
                    b = doubleArrayOf(0.614706700585201, 0.0596080168965685, -0.0868152012187231),
                    a = doubleArrayOf(1.0, -0.349169241905691, 0.0150080022658632),
                ),
                BiquadCoeffs(
                    b = doubleArrayOf(0.999914798855784, -1.99982846719653, 0.999914797704129),
                    a = doubleArrayOf(1.0, -1.99517110258717, 0.995174922234887),
                ),
                BiquadCoeffs(
                    b = doubleArrayOf(0.93365920473612, 0.0469991486605188, -0.0303407236498339),
                    a = doubleArrayOf(1.0, -0.0484587180754484, -0.0616305515141321),
                ),
                BiquadCoeffs(
                    b = doubleArrayOf(0.933659220266788, 0.0469991290557102, -0.0303407156896636),
                    a = doubleArrayOf(1.0, -0.0484587577711634, -0.0616305351352054),
                ),
            )

        private val ituR468Sections =
            arrayOf(
                BiquadCoeffs(
                    b = doubleArrayOf(1.03875068733911, -1.03968863432746, 0.000933145775353805),
                    a = doubleArrayOf(1.0, -0.890373055926747, 0.240997489989129),
                ),
                BiquadCoeffs(
                    b = doubleArrayOf(0.914659466024246, 0.18453863078621, -0.0576000834318053),
                    a = doubleArrayOf(1.0, -0.768328700797248, 0.344099211393362),
                ),
                BiquadCoeffs(
                    b = doubleArrayOf(0.913826194015739, 0.186034489194357, -0.0712529190679821),
                    a = doubleArrayOf(1.0, -0.205994208467531, 0.426993132271179),
                ),
                BiquadCoeffs(
                    b = doubleArrayOf(0.889144757737452, 0.242040815882066, -0.220841128784832),
                    a = doubleArrayOf(1.0, -0.162133907883523, -0.26344517755366),
                ),
            )

        private data class BiquadCoeffs(val b: DoubleArray, val a: DoubleArray)

        // Per-section filter state: [x1, x2, y1, y2]
        private var aWeightState = aWeightSections.emptyState()
        private var bWeightState = bWeightSections.emptyState()
        private var cWeightState = cWeightSections.emptyState()
        private var ituR468State = ituR468Sections.emptyState()

        fun reset() {
            aWeightState = aWeightSections.emptyState()
            bWeightState = bWeightSections.emptyState()
            cWeightState = cWeightSections.emptyState()
            ituR468State = ituR468Sections.emptyState()
        }

        fun applyWeighting(buffer: ShortArray, size: Int, weighting: WeightingType): DoubleArray = when (weighting) {
                WeightingType.Z -> DoubleArray(size) { buffer[it].toDouble() }

                WeightingType.A -> applyCascadedBiquad(buffer, size, aWeightSections, aWeightState)

                WeightingType.B -> applyCascadedBiquad(buffer, size, bWeightSections, bWeightState)

                WeightingType.C -> applyCascadedBiquad(buffer, size, cWeightSections, cWeightState)

                WeightingType.ITUR468 -> {
                    applyCascadedBiquad(buffer, size, ituR468Sections, ituR468State)
                }
            }

        private fun applyCascadedBiquad(
            buffer: ShortArray,
            size: Int,
            sections: Array<BiquadCoeffs>,
            states: Array<DoubleArray>,
        ): DoubleArray {
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

            return output
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

        private fun Array<BiquadCoeffs>.emptyState(): Array<DoubleArray> = Array(size) { DoubleArray(4) }
    }
