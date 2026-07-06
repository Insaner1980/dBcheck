package com.dbcheck.app.service

import android.content.Context
import androidx.core.content.ContextCompat
import com.dbcheck.app.domain.ambient.AmbientSoundPreset
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class AmbientSoundPlaybackRequest(val preset: AmbientSoundPreset, val volume: Float, val timerMinutes: Int)

@Singleton
class AmbientSoundPlaybackController
    @Inject
    constructor(
    @param:ApplicationContext private val context: Context,
) {
        private val _isPlaying = MutableStateFlow(false)
        val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

        fun startPlayback(request: AmbientSoundPlaybackRequest) {
            _isPlaying.value = true
            ContextCompat.startForegroundService(
                context,
                AmbientSoundPlaybackService.startIntent(
                    context = context,
                    preset = request.preset,
                    volume = request.volume,
                    timerMinutes = request.timerMinutes,
                    requestedByUser = true,
                ),
            )
        }

        fun stopPlayback() {
            _isPlaying.value = false
            context.startService(AmbientSoundPlaybackService.stopIntent(context))
        }

        internal fun markPlaying() {
            _isPlaying.value = true
        }

        internal fun markStopped() {
            _isPlaying.value = false
        }
    }
