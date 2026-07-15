package com.dbcheck.app.service

import com.dbcheck.app.projectFile
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidTextToSpeechPlayerContractTest {
    @Test
    fun textToSpeechPlayerUsesInitQueueFlushAndShutdown() {
        val source = projectFile("src/main/java/com/dbcheck/app/service/AndroidTextToSpeechPlayer.kt").readText()

        assertTrue(source.contains("TextToSpeech("))
        assertTrue(source.contains("onInitialized(status == TextToSpeech.SUCCESS)"))
        assertTrue(source.contains("TextToSpeech.QUEUE_FLUSH"))
        assertTrue(source.contains("shutdown()"))
    }

    @Test
    fun controllerResetReleasesTextToSpeechPlayer() {
        val source = projectFile("src/main/java/com/dbcheck/app/service/TtsRiskPromptController.kt").readText()
        val resetBlock = source.substringAfter("fun reset()").substringBefore("fun onRiskEvent")

        assertTrue(resetBlock.contains("player.shutdown()"))
    }

    @Test
    fun manifestDeclaresTtsServiceQueryForAndroidElevenVisibility() {
        val manifest = projectFile("src/main/AndroidManifest.xml").readText()

        assertTrue(manifest.contains("android.intent.action.TTS_SERVICE"))
    }
}
