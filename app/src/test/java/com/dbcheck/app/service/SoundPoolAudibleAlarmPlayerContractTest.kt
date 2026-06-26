package com.dbcheck.app.service

import com.dbcheck.app.projectFile
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundPoolAudibleAlarmPlayerContractTest {
    @Test
    fun soundPoolPlayerUsesBundledAlarmSoundAndAlarmAudioAttributes() {
        val source = projectFile("src/main/java/com/dbcheck/app/service/SoundPoolAudibleAlarmPlayer.kt").readText()

        assertTrue(source.contains("R.raw.audible_alarm"))
        assertTrue(source.contains("SoundPool.Builder()"))
        assertTrue(source.contains("AudioAttributes.USAGE_ALARM"))
        assertTrue(source.contains("AudioAttributes.CONTENT_TYPE_SONIFICATION"))
        assertTrue(projectFile("src/main/res/raw/audible_alarm.wav").isFile)
    }
}
