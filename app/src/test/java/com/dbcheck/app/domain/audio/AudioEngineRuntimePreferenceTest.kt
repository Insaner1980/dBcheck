package com.dbcheck.app.domain.audio

import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

class AudioEngineRuntimePreferenceTest {
    @Test
    fun calibrationOffsetIsVolatileForRuntimePreferenceUpdates() {
        val field = AudioEngine::class.java.getDeclaredField("calibrationOffset")

        assertTrue(Modifier.isVolatile(field.modifiers))
    }
}
