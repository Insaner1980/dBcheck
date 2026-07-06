package com.dbcheck.app.service

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.dbcheck.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundPoolAudibleAlarmPlayer @Inject constructor(@ApplicationContext context: Context) : AudibleAlarmPlayer {
    private val appContext = context.applicationContext
    private val soundPool =
        SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(alarmAudioAttributes())
            .build()
    private var soundId = 0

    @Volatile
    private var soundLoaded = false

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (sampleId == soundId) {
                soundLoaded = status == SOUND_LOAD_SUCCESS
            }
        }
        soundId = soundPool.load(appContext, R.raw.audible_alarm, SOUND_PRIORITY)
    }

    override fun playAlarm(): Boolean = playLoadedAlarm()

    override fun previewAlarm(): Boolean = playLoadedAlarm()

    private fun playLoadedAlarm(): Boolean {
        if (!soundLoaded) return false
        val streamId =
            soundPool.play(
                soundId,
                FULL_VOLUME,
                FULL_VOLUME,
                STREAM_PRIORITY,
                NO_LOOP,
                NORMAL_RATE,
            )
        return streamId != PLAY_FAILED
    }

    companion object {
        private const val SOUND_LOAD_SUCCESS = 0
        private const val SOUND_PRIORITY = 1
        private const val STREAM_PRIORITY = 1
        private const val PLAY_FAILED = 0
        private const val NO_LOOP = 0
        private const val FULL_VOLUME = 1f
        private const val NORMAL_RATE = 1f

        fun alarmAudioAttributes(): AudioAttributes = AudioAttributes
                .Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
    }
}
