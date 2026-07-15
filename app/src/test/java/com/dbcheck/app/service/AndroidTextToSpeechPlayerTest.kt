package com.dbcheck.app.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidTextToSpeechPlayerTest {
    @Test
    fun waitsForInitializationAndCreatesOnlyOneEngine() {
        val factory = FakeTextToSpeechEngineFactory()
        val player = AndroidTextToSpeechPlayer(factory)

        assertTrue(player.speak("First prompt"))
        assertTrue(player.speak("Latest prompt"))

        assertEquals(1, factory.engines.size)
        assertEquals(emptyList<String>(), factory.engines.single().spokenPrompts)

        factory.initializationCallbacks.single()(true)

        assertEquals(listOf("Latest prompt"), factory.engines.single().spokenPrompts)
    }

    @Test
    fun shutdownReleasesEngineAndNextUseInitializesNewEngine() {
        val factory = FakeTextToSpeechEngineFactory()
        val player = AndroidTextToSpeechPlayer(factory)

        player.speak("First session")
        factory.initializationCallbacks.single()(true)
        player.shutdown()

        assertEquals(1, factory.engines.single().shutdownCount)

        assertTrue(player.speak("Second session"))
        assertEquals(2, factory.engines.size)
        factory.initializationCallbacks.last()(true)

        assertEquals(listOf("Second session"), factory.engines.last().spokenPrompts)
    }

    @Test
    fun lateInitializationCallbackAfterShutdownIsIgnored() {
        val factory = FakeTextToSpeechEngineFactory()
        val player = AndroidTextToSpeechPlayer(factory)

        player.speak("Stale prompt")
        player.shutdown()
        factory.initializationCallbacks.single()(true)

        assertEquals(emptyList<String>(), factory.engines.single().spokenPrompts)
        assertEquals(1, factory.engines.single().shutdownCount)
    }

    @Test
    fun synchronousInitializationFailureDoesNotAcceptPrompt() {
        val factory = FakeTextToSpeechEngineFactory(synchronousInitializationResult = false)
        val player = AndroidTextToSpeechPlayer(factory)

        assertFalse(player.speak("Prompt"))
        assertEquals(1, factory.engines.single().shutdownCount)
    }

    private class FakeTextToSpeechEngineFactory(private val synchronousInitializationResult: Boolean? = null) :
        TextToSpeechEngineFactory {
        val engines = mutableListOf<FakeTextToSpeechEngine>()
        val initializationCallbacks = mutableListOf<(Boolean) -> Unit>()

        override fun create(onInitialized: (Boolean) -> Unit): TextToSpeechEngine {
            initializationCallbacks += onInitialized
            synchronousInitializationResult?.let(onInitialized)
            return FakeTextToSpeechEngine().also(engines::add)
        }
    }

    private class FakeTextToSpeechEngine : TextToSpeechEngine {
        val spokenPrompts = mutableListOf<String>()
        var shutdownCount = 0

        override fun speak(text: String): Boolean {
            spokenPrompts += text
            return true
        }

        override fun shutdown() {
            shutdownCount += 1
        }
    }
}
