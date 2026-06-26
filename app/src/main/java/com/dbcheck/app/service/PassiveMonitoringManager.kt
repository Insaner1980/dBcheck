package com.dbcheck.app.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.dbcheck.app.data.local.preferences.model.ProAudioPreferencePolicy
import com.dbcheck.app.data.repository.PassiveMonitoringRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.di.DefaultDispatcher
import com.dbcheck.app.domain.audio.AudioEngine
import com.dbcheck.app.domain.audio.AudioRecordingFailure
import com.dbcheck.app.domain.audio.AudioRecordingResult
import com.dbcheck.app.domain.audio.DecibelReading
import com.dbcheck.app.domain.audio.WeightingType
import com.dbcheck.app.domain.passive.PassiveMonitoringAggregator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PassiveMonitoringManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val audioEngine: AudioEngine,
        private val preferencesRepository: PreferencesRepository,
        private val passiveMonitoringRepository: PassiveMonitoringRepository,
        @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + defaultDispatcher)
        private val lifecycleMutex = Mutex()
        private var recordingJob: Job? = null
        private var readingCollectionJob: Job? = null
        private var startInProgress = false
        private var monitoringStartTimeMs = 0L
        private var aggregator: PassiveMonitoringAggregator? = null

        private val _isMonitoring = MutableStateFlow(false)
        val isMonitoring: StateFlow<Boolean> = _isMonitoring

        private val _activeMonitoringStartTimeMs = MutableStateFlow<Long?>(null)
        val activeMonitoringStartTimeMs: StateFlow<Long?> = _activeMonitoringStartTimeMs

        private val _monitoringStats = MutableStateFlow(SessionStats())
        val monitoringStats: StateFlow<SessionStats> = _monitoringStats

        private val _recordingFailures = MutableSharedFlow<AudioRecordingFailure>(extraBufferCapacity = 1)
        val recordingFailures: SharedFlow<AudioRecordingFailure> = _recordingFailures.asSharedFlow()

        suspend fun startMonitoring(): Boolean = lifecycleMutex.withLock {
                when {
                    _isMonitoring.value || startInProgress -> true
                    !context.hasMicrophonePermission() -> false
                    else -> startMonitoringLocked()
                }
            }

        fun stopMonitoring() {
            scope.launch {
                lifecycleMutex.withLock {
                    stopMonitoringLocked(persistAggregate = true, stopAudio = true)
                }
            }
        }

        private suspend fun startMonitoringLocked(): Boolean {
            startInProgress = true
            monitoringStartTimeMs = System.currentTimeMillis()
            aggregator = PassiveMonitoringAggregator(startedAtMs = monitoringStartTimeMs)
            _monitoringStats.value = SessionStats()
            applyAudioPreferencesForPassiveMonitoring()

            val recordingLaunch =
                scope.launchAudioRecording(
                    audioEngine = audioEngine,
                    onRecordingStarted = { onMonitoringStarted() },
                    onRecordingFinished = ::handleRecordingResult,
                )
            recordingJob = recordingLaunch.job
            val started = recordingLaunch.started.await()
            startInProgress = false
            if (!started) {
                stopMonitoringLocked(persistAggregate = false, stopAudio = false)
            }
            return started
        }

        private suspend fun applyAudioPreferencesForPassiveMonitoring() {
            val prefs = preferencesRepository.userPreferences.first()
            val weightingType = WeightingType.fromPreference(ProAudioPreferencePolicy.weighting(prefs))

            audioEngine.setWeighting(weightingType)
            audioEngine.setCalibrationOffset(ProAudioPreferencePolicy.micOffset(prefs))
            audioEngine.setResponseTime(
                ProAudioPreferencePolicy.responseTime(
                    isProUser = prefs.isProUser,
                    responseTime = prefs.responseTime,
                ),
            )
            audioEngine.setPreferredAudioInputDeviceId(prefs.selectedAudioInputDeviceId.takeIf { prefs.isProUser })
            audioEngine.setSoundDetectionEnabled(false)
            audioEngine.setSpectralAnalysisEnabled(false)
        }

        private fun onMonitoringStarted() {
            readingCollectionJob?.cancel()
            readingCollectionJob =
                scope.launch {
                    audioEngine.decibelFlow.collect { reading ->
                        handleReading(reading)
                    }
                }
            _activeMonitoringStartTimeMs.value = monitoringStartTimeMs
            _isMonitoring.value = true
        }

        private fun handleReading(reading: DecibelReading) {
            if (reading.timestamp < monitoringStartTimeMs) return

            aggregator?.add(reading)
            _monitoringStats.value = _monitoringStats.value.withReading(reading)
        }

        private fun handleRecordingResult(result: AudioRecordingResult) {
            if (result is AudioRecordingResult.Failed) {
                _recordingFailures.tryEmit(result.failure)
                scope.launch {
                    lifecycleMutex.withLock {
                        stopMonitoringLocked(persistAggregate = false, stopAudio = false)
                    }
                }
            }
        }

        private suspend fun stopMonitoringLocked(persistAggregate: Boolean, stopAudio: Boolean) {
            val sample = aggregator?.toSample(endedAtMs = System.currentTimeMillis())
            _isMonitoring.value = false
            _activeMonitoringStartTimeMs.value = null
            startInProgress = false
            if (stopAudio) {
                audioEngine.stopRecording()
            }
            recordingJob?.cancel()
            readingCollectionJob?.cancel()
            recordingJob = null
            readingCollectionJob = null
            aggregator = null
            audioEngine.setSoundDetectionEnabled(false)
            audioEngine.setSpectralAnalysisEnabled(false)

            if (persistAggregate && sample != null) {
                passiveMonitoringRepository.recordSample(sample)
            }
        }
    }

private fun Context.hasMicrophonePermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
