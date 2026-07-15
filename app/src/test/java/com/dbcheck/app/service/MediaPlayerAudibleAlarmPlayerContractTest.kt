package com.dbcheck.app.service

import com.dbcheck.app.projectFile
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPlayerAudibleAlarmPlayerContractTest {
    @Test
    fun mediaPlayerUsesBundledAlarmSoundAndAlarmAudioAttributes() {
        val source = projectFile("src/main/java/com/dbcheck/app/service/MediaPlayerAudibleAlarmPlayer.kt").readText()

        assertTrue(source.contains("R.raw.audible_alarm"))
        assertTrue(source.contains("MediaPlayer.create("))
        assertTrue(source.contains("AudioAttributes.USAGE_ALARM"))
        assertTrue(source.contains("AudioAttributes.CONTENT_TYPE_SONIFICATION"))
        assertTrue(projectFile("src/main/res/raw/audible_alarm.wav").isFile)
    }

    @Test
    fun alarmPlaybackReleasesPlayerAndAudioFocusAfterCompletion() {
        val source = projectFile("src/main/java/com/dbcheck/app/service/MediaPlayerAudibleAlarmPlayer.kt").readText()
        val completionHandler =
            source.substringAfter("private fun finishPlayback", missingDelimiterValue = "")
                .substringBefore("private fun releasePlaybackLocked")
        val releasePlayback =
            source.substringAfter("private fun releasePlaybackLocked", missingDelimiterValue = "")
                .substringBefore("private fun abandonAudioFocusLocked")
        val abandonAudioFocus =
            source.substringAfter("private fun abandonAudioFocusLocked", missingDelimiterValue = "")
                .substringBefore("companion object")

        assertTrue(source.contains("setOnCompletionListener(::finishPlayback)"))
        assertTrue(completionHandler.contains("releasePlaybackLocked()"))
        assertTrue(releasePlayback.contains("player.release()"))
        assertTrue(releasePlayback.contains("abandonAudioFocusLocked()"))
        assertTrue(abandonAudioFocus.contains("abandonAudioFocusRequest"))
    }
}
