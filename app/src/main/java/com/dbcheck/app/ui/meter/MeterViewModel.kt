package com.dbcheck.app.ui.meter

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dbcheck.app.data.model.NoiseLevel
import com.dbcheck.app.domain.audio.AudioEngine
import com.dbcheck.app.domain.audio.AudioSessionManager
import com.dbcheck.app.service.MeasurementForegroundService
import com.dbcheck.app.ui.meter.state.MeterUiState
import com.dbcheck.app.util.HapticFeedbackHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeterViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val audioEngine: AudioEngine,
        private val audioSessionManager: AudioSessionManager,
        private val hapticHelper: HapticFeedbackHelper,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(MeterUiState())
        val uiState: StateFlow<MeterUiState> = _uiState

        private var timerJob: Job? = null
        private var sessionStartTime = 0L
        private var previousNoiseLevel: NoiseLevel? = null
        private var previousMaxDb = 0f

        private val waveformBuffer = ArrayDeque<Float>(50)

        init {
            collectDecibelReadings()
            collectSessionStats()
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

                    // Update waveform buffer
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

        private fun collectSessionStats() {
            viewModelScope.launch {
                audioSessionManager.sessionStats.collect { stats ->
                    previousMaxDb = stats.maxDb
                    _uiState.update {
                        it.copy(
                            minDb = stats.minDb.takeIf { db -> db != Float.MAX_VALUE } ?: 0f,
                            avgDb = stats.avgDb,
                            maxDb = stats.maxDb,
                        )
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
            if (!_uiState.value.isMicPermissionGranted) return

            audioSessionManager.startSession()
            sessionStartTime = System.currentTimeMillis()
            _uiState.update { it.copy(isRecording = true, error = null) }

            // Start foreground service for background measurement
            val serviceIntent = Intent(context, MeasurementForegroundService::class.java)
            context.startForegroundService(serviceIntent)

            timerJob =
                viewModelScope.launch {
                    while (true) {
                        delay(1000)
                        _uiState.update {
                            it.copy(sessionDurationMs = System.currentTimeMillis() - sessionStartTime)
                        }
                    }
                }
        }

        private fun pauseRecording() {
            audioSessionManager.stopSession()
            timerJob?.cancel()
            _uiState.update { it.copy(isRecording = false) }

            // Stop foreground service
            context.stopService(Intent(context, MeasurementForegroundService::class.java))
        }

        fun resetMeasurement() {
            if (_uiState.value.isRecording) {
                pauseRecording()
            }
            audioSessionManager.resetStats()
            waveformBuffer.clear()
            previousMaxDb = 0f
            previousNoiseLevel = null
            _uiState.update {
                MeterUiState(isMicPermissionGranted = it.isMicPermissionGranted)
            }
        }

        override fun onCleared() {
            super.onCleared()
            if (_uiState.value.isRecording) {
                audioSessionManager.stopSession()
            }
        }
    }
