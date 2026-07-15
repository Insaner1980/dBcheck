package com.dbcheck.app.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import com.dbcheck.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaPlayerAudibleAlarmPlayer @Inject constructor(@ApplicationContext context: Context) : AudibleAlarmPlayer {
    private val appContext = context.applicationContext
    private val lock = Any()
    private val audioAttributes = alarmAudioAttributes()
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val audioFocusRequest =
        AudioFocusRequest
            .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener(::onAudioFocusChange)
            .build()
    private var activePlayer: MediaPlayer? = null
    private var hasAudioFocus = false

    override fun playAlarm(): Boolean = playAlarmSound()

    override fun previewAlarm(): Boolean = playAlarmSound()

    private fun playAlarmSound(): Boolean = synchronized(lock) {
        releasePlaybackLocked()
        if (audioManager.requestAudioFocus(audioFocusRequest) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return@synchronized false
        }
        hasAudioFocus = true

        val player =
            runCatching {
                MediaPlayer.create(
                    appContext,
                    R.raw.audible_alarm,
                    audioAttributes,
                    AudioManager.AUDIO_SESSION_ID_GENERATE,
                )
            }.getOrNull()
        if (player == null) {
            abandonAudioFocusLocked()
            return@synchronized false
        }

        activePlayer = player
        player.setOnCompletionListener(::finishPlayback)
        player.setOnErrorListener { failedPlayer, _, _ ->
            finishPlayback(failedPlayer)
            true
        }
        runCatching { player.start() }
            .fold(
                onSuccess = { true },
                onFailure = {
                    releasePlaybackLocked()
                    false
                },
            )
    }

    private fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
            -> synchronized(lock) {
                releasePlaybackLocked()
            }
        }
    }

    private fun finishPlayback(player: MediaPlayer) = synchronized(lock) {
        if (activePlayer === player) {
            releasePlaybackLocked()
        } else {
            runCatching { player.release() }
        }
    }

    private fun releasePlaybackLocked() {
        activePlayer?.let { player ->
            activePlayer = null
            player.setOnCompletionListener(null)
            player.setOnErrorListener(null)
            runCatching { player.stop() }
            runCatching { player.release() }
        }
        abandonAudioFocusLocked()
    }

    private fun abandonAudioFocusLocked() {
        if (hasAudioFocus) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
            hasAudioFocus = false
        }
    }

    companion object {
        fun alarmAudioAttributes(): AudioAttributes = AudioAttributes
                .Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
    }
}
