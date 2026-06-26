package com.dbcheck.app.service

import com.dbcheck.app.domain.voice.TtsRiskPromptRiskEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class TtsRiskPromptControllerTest {
    @Test
    fun playsPromptWhenPolicyTriggers() {
        val player = FakeTtsPromptPlayer()
        val controller = TtsRiskPromptController(player = player)

        val result =
            controller.onRiskEvent(
                riskEvent = TtsRiskPromptRiskEvent.DosimeterDose,
                timestampMs = 10_000L,
                isEnabled = true,
                isProUser = true,
                hasHearingBaseline = true,
                soundDetectionAvailable = true,
                promptMessage = PROMPT_MESSAGE,
            )

        assertEquals(TtsRiskPromptPlaybackResult.Played, result)
        assertEquals(listOf(PROMPT_MESSAGE), player.spokenPrompts)
    }

    @Test
    fun missingHearingBaselineDoesNotSpeak() {
        val player = FakeTtsPromptPlayer()
        val controller = TtsRiskPromptController(player = player)

        val result =
            controller.onRiskEvent(
                riskEvent = TtsRiskPromptRiskEvent.DosimeterDose,
                timestampMs = 10_000L,
                isEnabled = true,
                isProUser = true,
                hasHearingBaseline = false,
                soundDetectionAvailable = true,
                promptMessage = PROMPT_MESSAGE,
            )

        assertEquals(TtsRiskPromptPlaybackResult.SkippedMissingHearingBaseline, result)
        assertEquals(emptyList<String>(), player.spokenPrompts)
    }

    private class FakeTtsPromptPlayer : TtsPromptPlayer {
        val spokenPrompts = mutableListOf<String>()

        override fun speak(text: String): Boolean {
            spokenPrompts += text
            return true
        }
    }

    private companion object {
        const val PROMPT_MESSAGE = "Prompt"
    }
}
