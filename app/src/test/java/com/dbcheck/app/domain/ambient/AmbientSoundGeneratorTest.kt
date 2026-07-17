package com.dbcheck.app.domain.ambient

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AmbientSoundGeneratorTest {
    @Test
    fun everyPresetProducesFiniteNonSilentPcm16Data() {
        AmbientSoundPreset.entries.forEachIndexed { index, preset ->
            val samples =
                AmbientSoundGenerator(seed = 10_000L + index)
                    .generate(preset = preset, sampleCount = 4_096, volume = 0.35f)

            assertEquals(4_096, samples.size)
            assertTrue("$preset should not be silent", samples.any { it.toInt() != 0 })
            assertTrue("$preset should stay in PCM16 bounds", samples.all { it in Short.MIN_VALUE..Short.MAX_VALUE })
            assertTrue("$preset should vary over time", samples.distinct().size > 16)
            assertTrue("$preset should avoid clipped output", samples.maxOf { abs(it.toInt()) } < Short.MAX_VALUE)
        }
    }

    @Test
    fun generatorNormalizesRequestedVolumeBeforeRendering() {
        val samples =
            AmbientSoundGenerator(seed = 1L)
                .generate(
                    preset = AmbientSoundPreset.WHITE_NOISE,
                    sampleCount = 512,
                    volume = 5f,
                )

        assertTrue(samples.any { it.toInt() != 0 })
        assertTrue(samples.maxOf { abs(it.toInt()) } < Short.MAX_VALUE)
    }

    @Test
    fun generatorFillsCallerOwnedPlaybackBuffer() {
        val samples = ShortArray(512)

        val returned =
            AmbientSoundGenerator(seed = 2L)
                .generateInto(
                    preset = AmbientSoundPreset.PINK_NOISE,
                    samples = samples,
                    volume = 0.35f,
                )

        assertTrue(returned === samples)
        assertTrue(samples.any { it.toInt() != 0 })
    }
}
