package com.dbcheck.app.service

import com.dbcheck.app.projectFile
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientSoundPlaybackServiceContractTest {
    @Test
    fun mediaPlaybackRequestsPauseCallbackWhenAnotherAppMayDuck() {
        val source = projectFile("src/main/java/com/dbcheck/app/service/AmbientSoundPlaybackService.kt").readText()
        val focusRequest =
            source
                .substringAfter("private fun requestAudioFocus()")
                .substringBefore("private fun onAudioFocusChange")

        assertTrue(focusRequest.contains(".setWillPauseWhenDucked(true)"))
    }
}
