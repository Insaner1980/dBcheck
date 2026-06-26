package com.dbcheck.app.ui.meter

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dbcheck.app.R
import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.data.local.preferences.model.ProAudioPreferencePolicy
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.audio.AudioEngine
import com.dbcheck.app.domain.audio.AudioRecordingFailure
import com.dbcheck.app.domain.audio.DecibelReading
import com.dbcheck.app.domain.audio.WeightingType
import com.dbcheck.app.domain.noise.NoiseLevel
import com.dbcheck.app.domain.noise.SoundReferenceCatalog
import com.dbcheck.app.domain.report.equivalentLevelLabelForWeighting
import com.dbcheck.app.service.AudioSessionManager
import com.dbcheck.app.service.LiveExposureState
import com.dbcheck.app.service.MeasurementForegroundService
import com.dbcheck.app.ui.common.currentRecordingDurationMs
import com.dbcheck.app.ui.common.hasRecordAudioPermission
import com.dbcheck.app.ui.meter.state.LiveChartBuffer
import com.dbcheck.app.ui.meter.state.MeasurementMode
import com.dbcheck.app.ui.meter.state.MeterUiState
import com.dbcheck.app.ui.meter.state.toDosimeterUiState
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

private const val WAVEFORM_SAMPLE_LIMIT = 50

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
        private var latestLiveExposureState = LiveExposureState()

        private val waveformBuffer = ArrayDeque<Float>(WAVEFORM_SAMPLE_LIMIT)
        private val liveChartBuffer = LiveChartBuffer()

        init {
            collectMeterPreferences()
            collectDecibelReadings()
            collectAudioInputInfo()
            collectSessionStats()
            collectLiveExposureState()
            collectCompletedSessions()
            collectHealthConnectSyncFailures()
            collectRecordingFailures()
            collectRecordingState()
        }

        private fun collectMeterPreferences() {
            viewModelScope.launch {
                preferencesRepository.userPreferences.collect { prefs ->
                    val canUseDosimeterCard = prefs.isProUser && prefs.dosimeterCardEnabled
                    val canUseSleepCard = prefs.isProUser && prefs.sleepCardEnabled
                    val effectiveWeighting = ProAudioPreferencePolicy.weighting(prefs)
                    val effectiveResponseTime =
                        ProAudioPreferencePolicy.responseTime(
                            isProUser = prefs.isProUser,
                            responseTime = prefs.responseTime,
                        )
                    _uiState.update {
                        it.copy(
                            waveformStyle = prefs.waveformStyle,
                            refreshRate = prefs.refreshRate,
                            isProUser = prefs.isProUser,
                            dosimeterCardEnabled = canUseDosimeterCard,
                            sleepCardEnabled = canUseSleepCard,
                            measurementMode =
                                if (canUseDosimeterCard) {
                                    it.measurementMode
                                } else {
                                    MeasurementMode.DB_METER
                                },
                            equivalentLevelLabel = equivalentLevelLabelForWeighting(effectiveWeighting),
                            dosimeter = latestLiveExposureState.toDosimeterUiState(isProUser = canUseDosimeterCard),
                            sessionInfo =
                                it.sessionInfo.copy(
                                    weighting = WeightingType.fromPreference(effectiveWeighting),
                                    responseTime = effectiveResponseTime,
                                    showProDetails = prefs.isProUser && prefs.technicalMetadataEnabled,
                                ),
                        )
                    }
                }
            }
        }

        private fun collectAudioInputInfo() {
            viewModelScope.launch {
                audioEngine.audioInputInfo.collect { inputInfo ->
                    _uiState.update {
                        it.copy(
                            sessionInfo =
                                it.sessionInfo.copy(
                                    sampleRateHz = inputInfo.sampleRateHz,
                                    inputDeviceName = inputInfo.inputDeviceName,
                                ),
                        )
                    }
                }
            }
        }

        private fun collectDecibelReadings() {
            viewModelScope.launch {
                audioEngine.decibelFlow.collect { reading ->
                    handleDecibelReading(reading)
                }
            }
        }

        private fun handleDecibelReading(reading: DecibelReading) {
            val noiseLevel = NoiseLevel.fromDb(reading.weightedDb)
            emitThresholdHaptics(reading = reading, noiseLevel = noiseLevel)
            previousNoiseLevel = noiseLevel

            if (!shouldUpdateMeterUi(reading.timestamp, lastUiUpdateTimestamp, _uiState.value.refreshRate)) return

            lastUiUpdateTimestamp = reading.timestamp
            appendWaveformPeak(waveformBuffer = waveformBuffer, peakAmplitude = reading.peakAmplitude)
            updateMeterReadingState(reading = reading, noiseLevel = noiseLevel)
        }

        private fun emitThresholdHaptics(reading: DecibelReading, noiseLevel: NoiseLevel) {
            if (crossedDangerousThreshold(previousNoiseLevel = previousNoiseLevel, noiseLevel = noiseLevel)) {
                hapticHelper.lightTick()
            }

            if (reading.weightedDb > previousMaxDb && previousMaxDb > 0f) {
                hapticHelper.mediumClick()
            }
        }

        private fun updateMeterReadingState(reading: DecibelReading, noiseLevel: NoiseLevel) {
            val nearestReferenceMarker = SoundReferenceCatalog.nearestReferenceMarker(reading.weightedDb)
            val soundReferenceCurrentPosition = SoundReferenceCatalog.markerPosition(reading.weightedDb)
            val liveChartPoints =
                if (_uiState.value.isRecording) {
                    liveChartBuffer.add(timestampMs = reading.timestamp, db = reading.weightedDb)
                } else {
                    _uiState.value.liveChartPoints
                }

            _uiState.update {
                it.copy(
                    currentDb = reading.weightedDb,
                    noiseLevel = noiseLevel,
                    waveformData = waveformBuffer.toList(),
                    liveChartPoints = liveChartPoints,
                    nearestSoundReferenceMarker = nearestReferenceMarker,
                    soundReferenceCurrentPosition = soundReferenceCurrentPosition,
                )
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
                            peakDb = stats.peakDb,
                            sampleCount = stats.sampleCount,
                            equivalentLevelDb = stats.avgDb.takeIf { stats.sampleCount > 0 },
                        )
                    }
                }
            }
        }

        private fun collectLiveExposureState() {
            viewModelScope.launch {
                audioSessionManager.liveExposureState.collect { exposureState ->
                    latestLiveExposureState = exposureState
                    _uiState.update {
                        it.copy(dosimeter = exposureState.toDosimeterUiState(isProUser = it.dosimeterCardEnabled))
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

        private fun collectRecordingFailures() {
            viewModelScope.launch {
                audioSessionManager.recordingFailures.collect { failure ->
                    _uiState.update { it.copy(error = failure.toMeterErrorMessage(context)) }
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
            _uiState.update { it.withMicrophonePermissionResult(granted) }
        }

        fun onMicPermissionDenied() {
            _uiState.update { it.copy(showMicDeniedPrompt = true) }
        }

        fun onNotificationPermissionRequested() {
            _uiState.update { it.copy(notificationPermissionAlreadyRequested = true) }
        }

        fun setMeasurementMode(mode: MeasurementMode) {
            _uiState.update {
                it.copy(
                    measurementMode =
                        if (mode == MeasurementMode.DOSIMETER && !it.dosimeterCardEnabled) {
                            MeasurementMode.DB_METER
                        } else {
                            mode
                        },
                )
            }
        }

        fun toggleRecording() {
            if (_uiState.value.isRecording) {
                pauseRecording()
            } else {
                startRecording()
            }
        }

        private fun startRecording() {
            if (!context.hasRecordAudioPermission()) {
                _uiState.update {
                    it.copy(
                        isRecording = false,
                        isMicPermissionGranted = false,
                        showMicDeniedPrompt = true,
                    )
                }
            } else {
                val serviceIntent = MeasurementForegroundService.startMeasurementIntent(context)
                val serviceStarted =
                    runCatching {
                        context.startForegroundService(serviceIntent)
                    }.onFailure { error ->
                        _uiState.update {
                            it.copy(
                                error =
                                    error.toUserFacingMessage(
                                        context.getString(R.string.meter_start_background_failed),
                                    ),
                            )
                        }
                    }.isSuccess

                if (serviceStarted) {
                    _uiState.update { it.copy(error = null) }
                }
            }
        }

        private fun startActiveRecordingTimer() {
            timerJob?.cancel()
            sessionStartTime = audioSessionManager.activeSessionStartTimeMs.value ?: System.currentTimeMillis()
            val durationMs =
                currentRecordingDurationMs(
                    sessionStartTime = sessionStartTime,
                    fallbackDurationMs = _uiState.value.sessionDurationMs,
                )
            _uiState.update {
                it.copy(
                    isRecording = true,
                    sessionDurationMs = durationMs,
                    sessionInfo = it.sessionInfo.copy(isRecording = true, durationMs = durationMs),
                    error = null,
                )
            }

            timerJob =
                viewModelScope.launch {
                    while (true) {
                        delay(1000)
                        _uiState.update {
                            val nextDurationMs =
                                currentRecordingDurationMs(
                                    sessionStartTime = sessionStartTime,
                                    fallbackDurationMs = it.sessionDurationMs,
                                )
                            it.copy(
                                sessionDurationMs = nextDurationMs,
                                sessionInfo = it.sessionInfo.copy(durationMs = nextDurationMs),
                            )
                        }
                    }
                }
        }

        private fun stopActiveRecordingTimer() {
            val finalDurationMs =
                currentRecordingDurationMs(
                    sessionStartTime = sessionStartTime,
                    fallbackDurationMs = _uiState.value.sessionDurationMs,
                )
            timerJob?.cancel()
            timerJob = null
            _uiState.update {
                it.copy(
                    isRecording = false,
                    sessionDurationMs = finalDurationMs,
                    sessionInfo = it.sessionInfo.copy(isRecording = false, durationMs = finalDurationMs),
                )
            }
        }

        private fun pauseRecording(emitCompleted: Boolean = true): Boolean {
            val serviceStopped =
                runCatching {
                    context.startService(MeasurementForegroundService.stopIntent(context, emitCompleted))
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            error =
                                error.toUserFacingMessage(
                                    context.getString(R.string.meter_stop_background_failed),
                                ),
                        )
                    }
                }.isSuccess

            if (serviceStopped) {
                stopActiveRecordingTimer()
            }
            return serviceStopped
        }

        fun resetMeasurement() {
            if (_uiState.value.isRecording && !pauseRecording(emitCompleted = false)) return

            audioSessionManager.resetStats()
            waveformBuffer.clear()
            liveChartBuffer.clear()
            lastUiUpdateTimestamp = null
            previousMaxDb = 0f
            previousNoiseLevel = null
            _uiState.update {
                MeterUiState(
                    isMicPermissionGranted = it.isMicPermissionGranted,
                    notificationPermissionAlreadyRequested = it.notificationPermissionAlreadyRequested,
                    waveformStyle = it.waveformStyle,
                    refreshRate = it.refreshRate,
                    equivalentLevelLabel = it.equivalentLevelLabel,
                    dosimeter = latestLiveExposureState.toDosimeterUiState(isProUser = it.dosimeterCardEnabled),
                    sessionInfo = it.sessionInfo.copy(isRecording = false, durationMs = 0L),
                    isProUser = it.isProUser,
                    dosimeterCardEnabled = it.dosimeterCardEnabled,
                    sleepCardEnabled = it.sleepCardEnabled,
                    measurementMode =
                        if (it.dosimeterCardEnabled) {
                            it.measurementMode
                        } else {
                            MeasurementMode.DB_METER
                        },
                )
            }
        }

        fun createShareIntent() {
            val current = _uiState.value
            if (!current.canShare) {
                _uiState.update { it.copy(error = context.getString(R.string.meter_share_error_not_ready)) }
                return
            }

            val durationMs =
                if (current.isRecording) {
                    currentRecordingDurationMs(
                        sessionStartTime = sessionStartTime,
                        fallbackDurationMs = current.sessionDurationMs,
                    )
                } else {
                    current.sessionDurationMs
                }

            viewModelScope.launch {
                runCatching {
                    shareResultsGenerator.shareSessionStats(
                        avgDb = current.avgDb,
                        peakDb = current.peakDb,
                        durationMs = durationMs,
                        equivalentLevelLabel = current.equivalentLevelLabel,
                    )
                }.onSuccess { intent ->
                    _uiState.update { it.copy(error = null) }
                    _shareIntents.emit(intent)
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            error =
                                error.toUserFacingMessage(
                                    context.getString(R.string.meter_share_error_failed),
                                ),
                        )
                    }
                }
            }
        }

        fun onShareUnavailable() {
            _uiState.update { it.copy(error = context.getString(R.string.meter_share_error_no_app)) }
        }

        fun onSessionDetailOpened() {
            _uiState.update { it.copy(completedSessionId = null) }
        }
}

private fun AudioRecordingFailure.toMeterErrorMessage(context: Context): String = when (this) {
        AudioRecordingFailure.PermissionDenied -> context.getString(R.string.meter_recording_error_microphone_required)

        AudioRecordingFailure.CreationFailed,
        AudioRecordingFailure.StartFailed,
        -> context.getString(R.string.meter_recording_error_start_failed)

        AudioRecordingFailure.PersistenceFailed -> context.getString(R.string.meter_recording_error_storage_failed)

        is AudioRecordingFailure.ReadFailed -> context.getString(R.string.meter_recording_error_read_failed)
    }

private fun crossedDangerousThreshold(previousNoiseLevel: NoiseLevel?, noiseLevel: NoiseLevel): Boolean =
    previousNoiseLevel != null &&
        previousNoiseLevel != NoiseLevel.DANGEROUS &&
        noiseLevel == NoiseLevel.DANGEROUS

private fun appendWaveformPeak(waveformBuffer: ArrayDeque<Float>, peakAmplitude: Float) {
    waveformBuffer.addLast(peakAmplitude)
    if (waveformBuffer.size > WAVEFORM_SAMPLE_LIMIT) {
        waveformBuffer.removeFirst()
    }
}

private fun shouldUpdateMeterUi(
    timestamp: Long,
    previousUpdateTimestamp: Long?,
    refreshRate: MeterRefreshRate,
): Boolean {
    val previousUpdate = previousUpdateTimestamp ?: return true
    return timestamp - previousUpdate >= refreshRate.uiIntervalMs
}

private fun MeterUiState.withMicrophonePermissionResult(granted: Boolean): MeterUiState = copy(
    isMicPermissionGranted = granted,
    showMicDeniedPrompt = !granted && showMicDeniedPrompt,
)
