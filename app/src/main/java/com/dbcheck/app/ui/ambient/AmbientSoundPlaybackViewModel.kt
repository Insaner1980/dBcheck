package com.dbcheck.app.ui.ambient

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dbcheck.app.R
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.ambient.AmbientSoundPolicy
import com.dbcheck.app.domain.ambient.AmbientSoundPreset
import com.dbcheck.app.service.AmbientSoundPlaybackController
import com.dbcheck.app.service.AmbientSoundPlaybackRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AmbientSoundPlaybackUiState(
    val preset: AmbientSoundPreset = AmbientSoundPolicy.DEFAULT_PRESET,
    val volume: Float = AmbientSoundPolicy.DEFAULT_VOLUME,
    val timerMinutes: Int = AmbientSoundPolicy.DEFAULT_TIMER_MINUTES,
    val isProUser: Boolean = false,
    val isPlaying: Boolean = false,
    val errorMessage: String? = null,
    val title: String = "",
    val description: String = "",
) {
    val isLocked: Boolean
        get() = !isProUser
}

@HiltViewModel
class AmbientSoundPlaybackViewModel
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val preferencesRepository: PreferencesRepository,
        private val playbackController: AmbientSoundPlaybackController,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(baseState())
        val uiState: StateFlow<AmbientSoundPlaybackUiState> = _uiState

        init {
            viewModelScope.launch {
                combine(
                    preferencesRepository.userPreferences,
                    playbackController.isPlaying,
                ) { prefs, isPlaying ->
                    val isProUser = prefs.isProUser
                    AmbientSoundPlaybackUiState(
                        preset =
                            if (isProUser) {
                                prefs.ambientSoundPreset
                            } else {
                                AmbientSoundPolicy.DEFAULT_PRESET
                            },
                        volume =
                            if (isProUser) {
                                prefs.ambientSoundVolume
                            } else {
                                AmbientSoundPolicy.DEFAULT_VOLUME
                            },
                        timerMinutes =
                            if (isProUser) {
                                prefs.ambientSoundTimerMinutes
                            } else {
                                AmbientSoundPolicy.DEFAULT_TIMER_MINUTES
                            },
                        isProUser = isProUser,
                        isPlaying = isProUser && isPlaying,
                        errorMessage = _uiState.value.errorMessage,
                        title = context.getString(R.string.ambient_sound_title),
                        description = context.getString(R.string.ambient_sound_description),
                    )
                }.collect { state -> _uiState.value = state }
            }
        }

        fun updatePreset(preset: AmbientSoundPreset) {
            if (!ensureProUser()) return
            viewModelScope.launch {
                preferencesRepository.updateAmbientSoundPreset(preset)
            }
        }

        fun updateVolume(volume: Float) {
            if (!ensureProUser()) return
            viewModelScope.launch {
                preferencesRepository.updateAmbientSoundVolume(AmbientSoundPolicy.normalizeVolume(volume))
            }
        }

        fun updateTimerMinutes(minutes: Int) {
            if (!ensureProUser()) return
            viewModelScope.launch {
                preferencesRepository.updateAmbientSoundTimerMinutes(AmbientSoundPolicy.normalizeTimerMinutes(minutes))
            }
        }

        fun play(notificationPermissionGranted: Boolean) {
            if (!ensureProUser()) return
            if (!notificationPermissionGranted) {
                _uiState.update {
                    it.copy(errorMessage = context.getString(R.string.ambient_sound_notification_required))
                }
                return
            }
            val state = _uiState.value
            playbackController.startPlayback(
                AmbientSoundPlaybackRequest(
                    preset = state.preset,
                    volume = state.volume,
                    timerMinutes = state.timerMinutes,
                ),
            )
            _uiState.update { it.copy(errorMessage = null) }
        }

        fun stop() {
            playbackController.stopPlayback()
            _uiState.update { it.copy(errorMessage = null) }
        }

        private fun ensureProUser(): Boolean {
            if (_uiState.value.isProUser) return true
            playbackController.stopPlayback()
            _uiState.update {
                it.copy(errorMessage = context.getString(R.string.ambient_sound_pro_required))
            }
            return false
        }

        private fun baseState(): AmbientSoundPlaybackUiState = AmbientSoundPlaybackUiState(
            title = context.getString(R.string.ambient_sound_title),
            description = context.getString(R.string.ambient_sound_description),
        )
    }
