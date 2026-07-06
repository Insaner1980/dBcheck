package com.dbcheck.app.ui.sleep

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dbcheck.app.R
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.sleep.SleepRecordingConfig
import com.dbcheck.app.service.AudioSessionManager
import com.dbcheck.app.service.MeasurementForegroundService
import com.dbcheck.app.ui.common.currentRecordingDurationMs
import com.dbcheck.app.ui.common.hasRecordAudioPermission
import com.dbcheck.app.util.toUserFacingMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SleepSetupAvailability {
    Loading,
    Locked,
    Ready,
}

object SleepSetupDefaults {
    const val DEFAULT_DURATION_MINUTES = SleepRecordingConfig.DEFAULT_TARGET_DURATION_MINUTES
    val DURATION_OPTIONS_MINUTES = SleepRecordingConfig.TARGET_DURATION_OPTIONS_MINUTES
}

data class SleepSetupUiState(
    val availability: SleepSetupAvailability = SleepSetupAvailability.Loading,
    val durationOptionsMinutes: List<Int> = SleepSetupDefaults.DURATION_OPTIONS_MINUTES,
    val targetDurationMinutes: Int = SleepSetupDefaults.DEFAULT_DURATION_MINUTES,
    val keepAwakeEnabled: Boolean = false,
    val isRecording: Boolean = false,
    val sessionDurationMs: Long = 0L,
    val isMicPermissionGranted: Boolean = false,
    val showMicDeniedPrompt: Boolean = false,
    val notificationPermissionAlreadyRequested: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SleepSetupViewModel
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val preferencesRepository: PreferencesRepository,
        private val audioSessionManager: AudioSessionManager,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SleepSetupUiState())
        val uiState: StateFlow<SleepSetupUiState> = _uiState
        private var timerJob: Job? = null
        private var sessionStartTime = 0L

        init {
            collectPreferences()
            collectRecordingState()
        }

        private fun collectPreferences() {
            viewModelScope.launch {
                preferencesRepository.userPreferences.collect { preferences ->
                    _uiState.update { current ->
                        current.copy(
                            availability =
                                if (preferences.isProUser) {
                                    SleepSetupAvailability.Ready
                                } else {
                                    SleepSetupAvailability.Locked
                                },
                        )
                    }
                }
            }
        }

        private fun collectRecordingState() {
            viewModelScope.launch {
                audioSessionManager.isRecording.collect { recording ->
                    if (recording) {
                        startActiveRecordingTimer()
                    } else {
                        stopActiveRecordingTimer()
                    }
                }
            }
        }

        fun updateTargetDurationMinutes(durationMinutes: Int) {
            _uiState.update { current ->
                if (
                    current.availability == SleepSetupAvailability.Ready &&
                    durationMinutes in current.durationOptionsMinutes
                ) {
                    current.copy(targetDurationMinutes = durationMinutes)
                } else {
                    current
                }
            }
        }

        fun updateKeepAwakeEnabled(enabled: Boolean) {
            _uiState.update { current ->
                if (current.availability == SleepSetupAvailability.Ready) {
                    current.copy(keepAwakeEnabled = enabled)
                } else {
                    current
                }
            }
        }

        fun startSleepRecording() {
            val current = _uiState.value
            when {
                current.availability != SleepSetupAvailability.Ready -> return

                current.isRecording -> return

                !context.hasRecordAudioPermission() -> {
                    _uiState.update {
                        it.copy(
                            isMicPermissionGranted = false,
                            showMicDeniedPrompt = true,
                        )
                    }
                }

                else -> {
                    val serviceIntent =
                        MeasurementForegroundService.startSleepIntent(
                            context = context,
                            targetDurationMinutes = current.targetDurationMinutes,
                            keepAwakeEnabled = current.keepAwakeEnabled,
                        )
                    val serviceStarted =
                        runCatching {
                            context.startForegroundService(serviceIntent)
                        }.onFailure { error ->
                            _uiState.update {
                                it.copy(
                                    error =
                                        error.toUserFacingMessage(
                                            context.getString(R.string.sleep_setup_start_background_failed),
                                        ),
                                )
                            }
                        }.isSuccess

                    if (serviceStarted) {
                        _uiState.update { it.copy(error = null) }
                    }
                }
            }
        }

        fun stopSleepRecording() {
            val serviceStopped =
                runCatching {
                    context.startService(MeasurementForegroundService.stopIntent(context, emitCompleted = true))
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            error =
                                error.toUserFacingMessage(
                                    context.getString(R.string.sleep_setup_stop_background_failed),
                                ),
                        )
                    }
                }.isSuccess

            if (serviceStopped) {
                stopActiveRecordingTimer()
            }
        }

        private fun startActiveRecordingTimer() {
            timerJob?.cancel()
            sessionStartTime = audioSessionManager.activeSessionStartTimeMs.value ?: System.currentTimeMillis()
            _uiState.update {
                it.copy(
                    isRecording = true,
                    sessionDurationMs = currentRecordingDurationMs(sessionStartTime, it.sessionDurationMs),
                    error = null,
                )
            }

            timerJob =
                viewModelScope.launch {
                    while (true) {
                        delay(1000)
                        _uiState.update {
                            it.copy(
                                sessionDurationMs =
                                    currentRecordingDurationMs(
                                        sessionStartTime = sessionStartTime,
                                        fallbackDurationMs = it.sessionDurationMs,
                                    ),
                            )
                        }
                    }
                }
        }

        private fun stopActiveRecordingTimer() {
            val finalDurationMs = currentRecordingDurationMs(sessionStartTime, _uiState.value.sessionDurationMs)
            timerJob?.cancel()
            timerJob = null
            _uiState.update {
                it.copy(
                    isRecording = false,
                    sessionDurationMs = finalDurationMs,
                )
            }
        }

        fun onMicPermissionResult(granted: Boolean) = applyMicrophonePermissionResult(granted)

        fun onMicPermissionDenied() = showMicrophoneDeniedPrompt()

        fun onNotificationPermissionRequested() = markNotificationPermissionRequested()

        private fun applyMicrophonePermissionResult(granted: Boolean) {
            _uiState.update { it.withMicrophonePermissionResult(granted) }
        }

        private fun showMicrophoneDeniedPrompt() {
            _uiState.update { it.copy(showMicDeniedPrompt = true) }
        }

        private fun markNotificationPermissionRequested() {
            _uiState.update { it.copy(notificationPermissionAlreadyRequested = true) }
        }
    }

private fun SleepSetupUiState.withMicrophonePermissionResult(granted: Boolean): SleepSetupUiState = copy(
    isMicPermissionGranted = granted,
    showMicDeniedPrompt = !granted && showMicDeniedPrompt,
)
