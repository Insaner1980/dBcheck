package com.dbcheck.app.ui.meter

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.audio.AudioEngine
import com.dbcheck.app.domain.noise.NoiseLevel
import com.dbcheck.app.service.AudioSessionManager
import com.dbcheck.app.service.MeasurementForegroundService
import com.dbcheck.app.ui.meter.state.MeterUiState
import com.dbcheck.app.util.HapticFeedbackHelper
import com.dbcheck.app.util.ShareResultsGenerator
import com.dbcheck.app.util.toUserFacingMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeterViewModel
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val audioEngine: AudioEngine,
        private val audioSessionManager: AudioSessionManager,
        private val preferencesRepository: PreferencesRepository,
        private val hapticHelper: HapticFeedbackHelper,
        private val shareResultsGenerator: ShareResultsGenerator,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(MeterUiState())
        val uiState: StateFlow<MeterUiState> = _uiState
        private val _shareIntents = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
        val shareIntents: SharedFlow<Intent> = _shareIntents.asSharedFlow()

        private var timerJob: Job? = null
        private var sessionStartTime = 0L
        private var previousNoiseLevel: NoiseLevel? = null
        private var previousMaxDb = 0f
        private var lastUiUpdateTimestamp: Long? = null

        private val waveformBuffer = ArrayDeque<Float>(50)

        init {
            collectMeterPreferences()
            collectDecibelReadings()
            collectSessionStats()
            collectCompletedSessions()
            collectHealthConnectSyncFailures()
            collectRecordingState()
        }

        private fun collectMeterPreferences() {
            viewModelScope.launch {
                preferencesRepository.userPreferences.collect { prefs ->
                    _uiState.update {
                        it.copy(
                            waveformStyle = prefs.waveformStyle,
                            refreshRate = prefs.refreshRate,
                        )
                    }
                }
            }
        }

        private fun collectDecibelReadings() {
            viewModelScope.launch {
                audioEngine.decibelFlow.collect { reading ->
                    val noiseLevel = NoiseLevel.fromDb(reading.weightedDb)

                    // Haptic on 85dB threshold crossing
                    if (previousNoiseLevel != null &&
                        previousNoiseLevel != NoiseLevel.DANGEROUS &&
                        noiseLevel == NoiseLevel.DANGEROUS
                    ) {
                        hapticHelper.lightTick()
                    }

                    // Haptic on new peak
                    if (reading.weightedDb > previousMaxDb && previousMaxDb > 0f) {
                        hapticHelper.mediumClick()
                    }

                    previousNoiseLevel = noiseLevel

                    if (shouldUpdateMeterUi(reading.timestamp, _uiState.value.refreshRate)) {
                        lastUiUpdateTimestamp = reading.timestamp
                        waveformBuffer.addLast(reading.peakAmplitude)
                        if (waveformBuffer.size > 50) waveformBuffer.removeFirst()

                        _uiState.update {
                            it.copy(
                                currentDb = reading.weightedDb,
                                noiseLevel = noiseLevel,
                                waveformData = waveformBuffer.toList(),
                            )
                        }
                    }
                }
            }
        }

        private fun shouldUpdateMeterUi(
            timestamp: Long,
            refreshRate: MeterRefreshRate,
        ): Boolean {
            val previousUpdate = lastUiUpdateTimestamp ?: return true
            return timestamp - previousUpdate >= refreshRate.uiIntervalMs
        }

        private fun collectSessionStats() {
            viewModelScope.launch {
                audioSessionManager.sessionStats.collect { stats ->
                    previousMaxDb = stats.maxDb
                    _uiState.update {
                        it.copy(
                            minDb = stats.minDb.takeIf { db -> db != Float.MAX_VALUE } ?: 0f,
                            avgDb = stats.avgDb,
                            maxDb = stats.maxDb,
                            peakDb = stats.peakDb,
                            sampleCount = stats.sampleCount,
                        )
                    }
                }
            }
        }

        private fun collectCompletedSessions() {
            viewModelScope.launch {
                audioSessionManager.completedSessionIds.collect { sessionId ->
                    _uiState.update { it.copy(completedSessionId = sessionId) }
                }
            }
        }

        private fun collectHealthConnectSyncFailures() {
            viewModelScope.launch {
                audioSessionManager.healthConnectSyncFailures.collect { reason ->
                    _uiState.update { it.copy(error = reason) }
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

        fun onMicPermissionResult(granted: Boolean) {
            _uiState.update {
                it.copy(
                    isMicPermissionGranted = granted,
                    showMicDeniedPrompt = !granted && it.showMicDeniedPrompt,
                )
            }
        }

        fun onMicPermissionDenied() {
            _uiState.update { it.copy(showMicDeniedPrompt = true) }
        }

        fun toggleRecording() {
            if (_uiState.value.isRecording) {
                pauseRecording()
            } else {
                startRecording()
            }
        }

        private fun startRecording() {
            if (!hasMicrophonePermission()) {
                showMicrophonePermissionRequired()
            } else {
                val serviceIntent = Intent(context, MeasurementForegroundService::class.java)
                val serviceStarted =
                    runCatching {
                        context.startForegroundService(serviceIntent)
                    }.onFailure { error ->
                        _uiState.update {
                            it.copy(error = error.toUserFacingMessage("Unable to start background measurement"))
                        }
                    }.isSuccess

                if (serviceStarted) {
                    _uiState.update { it.copy(error = null) }
                }
            }
        }

        private fun startActiveRecordingTimer() {
            timerJob?.cancel()
            sessionStartTime = System.currentTimeMillis()
            _uiState.update { it.copy(isRecording = true, sessionDurationMs = 0L, error = null) }

            timerJob =
                viewModelScope.launch {
                    while (true) {
                        delay(1000)
                        _uiState.update {
                            it.copy(sessionDurationMs = currentSessionDurationMs())
                        }
                    }
                }
        }

        private fun stopActiveRecordingTimer() {
            val finalDurationMs = currentSessionDurationMs()
            timerJob?.cancel()
            timerJob = null
            _uiState.update { it.copy(isRecording = false, sessionDurationMs = finalDurationMs) }
        }

        private fun currentSessionDurationMs(): Long = if (sessionStartTime > 0L) {
            (System.currentTimeMillis() - sessionStartTime).coerceAtLeast(0L)
        } else {
            _uiState.value.sessionDurationMs
        }

        private fun hasMicrophonePermission(): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED

        private fun showMicrophonePermissionRequired() {
            _uiState.update {
                it.copy(
                    isRecording = false,
                    isMicPermissionGranted = false,
                    showMicDeniedPrompt = true,
                )
            }
        }

        private fun pauseRecording(emitCompleted: Boolean = true) {
            context.startService(MeasurementForegroundService.stopIntent(context, emitCompleted))
            stopActiveRecordingTimer()
        }

        fun resetMeasurement() {
            if (_uiState.value.isRecording) {
                pauseRecording(emitCompleted = false)
            }
            audioSessionManager.resetStats()
            waveformBuffer.clear()
            lastUiUpdateTimestamp = null
            previousMaxDb = 0f
            previousNoiseLevel = null
            _uiState.update {
                MeterUiState(
                    isMicPermissionGranted = it.isMicPermissionGranted,
                    waveformStyle = it.waveformStyle,
                    refreshRate = it.refreshRate,
                )
            }
        }

        fun createShareIntent() {
            val current = _uiState.value
            if (!current.canShare) {
                _uiState.update { it.copy(error = "Start measuring before sharing results") }
                return
            }

            val durationMs =
                if (current.isRecording) {
                    currentSessionDurationMs()
                } else {
                    current.sessionDurationMs
                }

            viewModelScope.launch {
                runCatching {
                    shareResultsGenerator.shareSessionStats(
                        avgDb = current.avgDb,
                        peakDb = current.peakDb,
                        durationMs = durationMs,
                    )
                }.onSuccess { intent ->
                    _uiState.update { it.copy(error = null) }
                    _shareIntents.emit(intent)
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(error = error.toUserFacingMessage("Unable to share meter results"))
                    }
                }
            }
        }

        fun onShareUnavailable() {
            _uiState.update { it.copy(error = "No app available to share results") }
        }

        fun onSessionDetailOpened() {
            _uiState.update { it.copy(completedSessionId = null) }
        }

        override fun onCleared() {
            super.onCleared()
            if (_uiState.value.isRecording) {
                context.stopService(Intent(context, MeasurementForegroundService::class.java))
            }
        }
    }
