package com.dbcheck.app.domain.audio

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

@OptIn(ExperimentalCoroutinesApi::class)
class AudioEngineRuntimePreferenceTest {
    @Test
    fun calibrationOffsetIsVolatileForRuntimePreferenceUpdates() {
        val field = AudioEngine::class.java.getDeclaredField("calibrationOffset")

        assertTrue(Modifier.isVolatile(field.modifiers))
    }

    @Test
    fun audioEngineKeepsAWeightingFilterSeparateFromSelectedAndPeakFilters() {
        val selectedWeightingFilter = FrequencyWeightingFilter()
        val engine =
            AudioEngine(
                context = mockk<Context>(),
                decibelCalculator = DecibelCalculator(),
                weightingFilter = selectedWeightingFilter,
                spectralAnalyzer = SpectralAnalyzer(FFTProcessor()),
                defaultDispatcher = UnconfinedTestDispatcher(),
            )
        val aWeightingFilter = AudioEngine::class.java
            .getDeclaredField("aWeightingFilter")
            .also { it.isAccessible = true }
            .get(engine)
        val cPeakWeightingFilter = AudioEngine::class.java
            .getDeclaredField("cPeakWeightingFilter")
            .also { it.isAccessible = true }
            .get(engine)

        assertNotSame(selectedWeightingFilter, aWeightingFilter)
        assertNotSame(cPeakWeightingFilter, aWeightingFilter)
    }
}
