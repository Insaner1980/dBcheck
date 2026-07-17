package com.dbcheck.app.domain.ambient

import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

class AmbientSoundGenerator(private val sampleRate: Int = DEFAULT_SAMPLE_RATE, seed: Long = System.nanoTime()) {
    // Deterministinen PRNG tuottaa vain paikallista audio-noisea, ei turva- tai tunnistearvoja.
    private val random = Random(seed)
    private var pinkB0 = 0.0
    private var pinkB1 = 0.0
    private var pinkB2 = 0.0
    private var pinkB3 = 0.0
    private var pinkB4 = 0.0
    private var pinkB5 = 0.0
    private var pinkB6 = 0.0
    private var brownState = 0.0
    private var fanNoiseState = 0.0
    private var fanPhase = 0.0

    fun generate(preset: AmbientSoundPreset, sampleCount: Int, volume: Float): ShortArray {
        if (sampleCount <= 0) return ShortArray(0)
        return generateInto(preset, ShortArray(sampleCount), volume)
    }

    fun generateInto(preset: AmbientSoundPreset, samples: ShortArray, volume: Float): ShortArray {
        val amplitude = Short.MAX_VALUE * BASE_AMPLITUDE * AmbientSoundPolicy.normalizeVolume(volume)
        samples.indices.forEach { index ->
            samples[index] =
                when (preset) {
                    AmbientSoundPreset.WHITE_NOISE -> nextWhiteNoise()
                    AmbientSoundPreset.PINK_NOISE -> nextPinkNoise()
                    AmbientSoundPreset.BROWN_NOISE -> nextBrownNoise()
                    AmbientSoundPreset.FAN -> nextFanNoise()
                }.toPcm16(amplitude)
        }
        return samples
    }

    private fun nextWhiteNoise(): Double = random.nextDouble() * 2.0 - 1.0

    private fun nextPinkNoise(): Double {
        val white = nextWhiteNoise()
        pinkB0 = 0.99886 * pinkB0 + white * 0.0555179
        pinkB1 = 0.99332 * pinkB1 + white * 0.0750759
        pinkB2 = 0.96900 * pinkB2 + white * 0.1538520
        pinkB3 = 0.86650 * pinkB3 + white * 0.3104856
        pinkB4 = 0.55000 * pinkB4 + white * 0.5329522
        pinkB5 = -0.7616 * pinkB5 - white * 0.0168980
        val pink = pinkB0 + pinkB1 + pinkB2 + pinkB3 + pinkB4 + pinkB5 + pinkB6 + white * 0.5362
        pinkB6 = white * 0.115926
        return (pink / PINK_GAIN).coerceIn(-1.0, 1.0)
    }

    private fun nextBrownNoise(): Double {
        brownState = (brownState + nextWhiteNoise() * BROWN_STEP).coerceIn(-1.0, 1.0)
        return brownState
    }

    private fun nextFanNoise(): Double {
        fanNoiseState = FAN_NOISE_DECAY * fanNoiseState + (1.0 - FAN_NOISE_DECAY) * nextWhiteNoise()
        fanPhase = (fanPhase + TWO_PI * FAN_HUM_HZ / sampleRate) % TWO_PI
        val hum = sin(fanPhase) * FAN_HUM_MIX + sin(fanPhase * 2.0) * FAN_SECOND_HARMONIC_MIX
        return (fanNoiseState * FAN_NOISE_MIX + hum).coerceIn(-1.0, 1.0)
    }

    private fun Double.toPcm16(amplitude: Double): Short = (coerceIn(-1.0, 1.0) * amplitude)
            .roundToInt()
            .coerceIn(Short.MIN_VALUE.toInt() + 1, Short.MAX_VALUE.toInt() - 1)
            .toShort()

    private companion object {
        const val DEFAULT_SAMPLE_RATE = 44_100
        const val BASE_AMPLITUDE = 0.28
        const val PINK_GAIN = 5.0
        const val BROWN_STEP = 0.035
        const val FAN_NOISE_DECAY = 0.96
        const val FAN_NOISE_MIX = 0.68
        const val FAN_HUM_MIX = 0.24
        const val FAN_SECOND_HARMONIC_MIX = 0.08
        const val FAN_HUM_HZ = 120.0
        const val TWO_PI = 2.0 * PI
    }
}
