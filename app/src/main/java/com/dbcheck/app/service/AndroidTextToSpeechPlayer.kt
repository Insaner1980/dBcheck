package com.dbcheck.app.service

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidTextToSpeechPlayer
    @Inject
    constructor(@ApplicationContext context: Context) :
    TtsPromptPlayer,
    TextToSpeech.OnInitListener {
        private val textToSpeech = TextToSpeech(context.applicationContext, this)

        @Volatile
        private var initialized = false

        @Volatile
        private var pendingPrompt: String? = null

        override fun onInit(status: Int) {
            initialized = status == TextToSpeech.SUCCESS
            if (!initialized) {
                pendingPrompt = null
                return
            }
            pendingPrompt?.let { prompt ->
                pendingPrompt = null
                speak(prompt)
            }
        }

        override fun speak(text: String): Boolean {
            val prompt = text.trim()
            return when {
                prompt.isEmpty() -> false

                !initialized -> {
                    pendingPrompt = prompt
                    true
                }

                else ->
                    textToSpeech.speak(
                        prompt,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        UTTERANCE_ID,
                    ) == TextToSpeech.SUCCESS
            }
        }

        fun shutdown() {
            pendingPrompt = null
            initialized = false
            textToSpeech.shutdown()
        }

        private companion object {
            const val UTTERANCE_ID = "dbcheck_tts_risk_prompt"
        }
    }
