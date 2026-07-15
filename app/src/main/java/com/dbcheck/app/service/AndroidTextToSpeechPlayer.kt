package com.dbcheck.app.service

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidTextToSpeechPlayer internal constructor(private val engineFactory: TextToSpeechEngineFactory) :
    TtsPromptPlayer {
    @Inject
    constructor(@ApplicationContext context: Context) : this(
        engineFactory = AndroidTextToSpeechEngineFactory(context.applicationContext),
    )

    private val lock = Any()
    private var engine: TextToSpeechEngine? = null
    private var state = InitializationState.Idle
    private var pendingPrompt: String? = null
    private var generation = 0L

    override fun speak(text: String): Boolean = synchronized(lock) {
        val prompt = text.trim()
        when {
            prompt.isEmpty() -> false

            state == InitializationState.Ready -> engine?.speak(prompt) == true

            state == InitializationState.Initializing -> {
                pendingPrompt = prompt
                true
            }

            else -> initializeAndQueue(prompt)
        }
    }

    override fun shutdown() = synchronized(lock) {
        generation += 1L
        pendingPrompt = null
        state = InitializationState.Idle
        engine?.shutdown()
        engine = null
    }

    private fun initializeAndQueue(prompt: String): Boolean {
        pendingPrompt = prompt
        state = InitializationState.Initializing
        val currentGeneration = ++generation
        var constructionFinished = false
        var synchronousInitResult: Boolean? = null
        val createdEngine =
            try {
                engineFactory.create { initialized ->
                    synchronized(lock) {
                        if (constructionFinished) {
                            handleInitialization(currentGeneration, initialized)
                        } else {
                            synchronousInitResult = initialized
                        }
                    }
                }
            } catch (_: RuntimeException) {
                pendingPrompt = null
                state = InitializationState.Idle
                return false
            }
        engine = createdEngine
        constructionFinished = true
        synchronousInitResult?.let { initialized ->
            handleInitialization(currentGeneration, initialized)
        }
        return state != InitializationState.Idle
    }

    private fun handleInitialization(initializationGeneration: Long, initialized: Boolean) {
        if (initializationGeneration != generation || state != InitializationState.Initializing) return
        if (!initialized) {
            pendingPrompt = null
            state = InitializationState.Idle
            engine?.shutdown()
            engine = null
            return
        }

        state = InitializationState.Ready
        pendingPrompt?.let { prompt ->
            pendingPrompt = null
            engine?.speak(prompt)
        }
    }

    private enum class InitializationState {
        Idle,
        Initializing,
        Ready,
    }
}

internal fun interface TextToSpeechEngineFactory {
    fun create(onInitialized: (Boolean) -> Unit): TextToSpeechEngine
}

internal interface TextToSpeechEngine {
    fun speak(text: String): Boolean

    fun shutdown()
}

private class AndroidTextToSpeechEngineFactory(private val context: Context) : TextToSpeechEngineFactory {
    override fun create(onInitialized: (Boolean) -> Unit): TextToSpeechEngine {
        val textToSpeech =
            TextToSpeech(context) { status ->
                onInitialized(status == TextToSpeech.SUCCESS)
            }
        return AndroidTextToSpeechEngine(textToSpeech)
    }
}

private class AndroidTextToSpeechEngine(private val textToSpeech: TextToSpeech) : TextToSpeechEngine {
    override fun speak(text: String): Boolean = textToSpeech.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            UTTERANCE_ID,
        ) == TextToSpeech.SUCCESS

    override fun shutdown() {
        textToSpeech.shutdown()
    }

    private companion object {
        const val UTTERANCE_ID = "dbcheck_tts_risk_prompt"
    }
}
