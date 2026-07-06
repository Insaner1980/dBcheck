package com.dbcheck.app.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.dbcheck.app.R
import com.dbcheck.app.data.local.preferences.model.ProAudioPreferencePolicy
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.HearingTestRepository
import com.dbcheck.app.data.repository.MeasurementRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.data.repository.SessionMeasurementSummary
import com.dbcheck.app.data.repository.SessionRepository
import com.dbcheck.app.data.repository.SleepSessionRepository
import com.dbcheck.app.data.repository.SoundDetectionRepository
import com.dbcheck.app.di.DefaultDispatcher
import com.dbcheck.app.di.IoDispatcher
import com.dbcheck.app.domain.analytics.EnvironmentExposureMixCounts
import com.dbcheck.app.domain.analytics.withWeightedDb
import com.dbcheck.app.domain.audio.AudioInputInfo
import com.dbcheck.app.domain.audio.AudioRecordingFailure
import com.dbcheck.app.domain.audio.AudioRecordingResult
import com.dbcheck.app.domain.audio.DecibelReading
import com.dbcheck.app.domain.audio.ResponseTime
import com.dbcheck.app.domain.audio.SoundClassifier
import com.dbcheck.app.domain.audio.SoundDetectionError
import com.dbcheck.app.domain.audio.SoundDetectionEvent
import com.dbcheck.app.domain.audio.SoundDetectionState
import com.dbcheck.app.domain.audio.WeightingType
import com.dbcheck.app.domain.audio.withClassification
import com.dbcheck.app.domain.audio.withError
import com.dbcheck.app.domain.noise.DecibelMath
import com.dbcheck.app.domain.noise.DosimeterCalculator
import com.dbcheck.app.domain.noise.DosimeterStandard
import com.dbcheck.app.domain.noise.NoiseNotificationSchedule
import com.dbcheck.app.domain.report.SessionReportCalculator
import com.dbcheck.app.domain.report.SessionReportData
import com.dbcheck.app.domain.session.Session
import com.dbcheck.app.domain.session.SessionAudioInputDeviceMetadata
import com.dbcheck.app.domain.session.SessionMeasurement
import com.dbcheck.app.domain.sleep.SleepRecordingConfig
import com.dbcheck.app.domain.voice.TtsRiskPromptRiskEvent
import com.dbcheck.app.domain.voice.VoiceBaselineCalibrator
import com.dbcheck.app.domain.voice.VoiceBaselineCapture
import com.dbcheck.app.domain.voice.VoiceVolumeWarningEvaluation
import com.dbcheck.app.domain.voice.VoiceVolumeWarningEvaluator
import com.dbcheck.app.service.AudioEngine
import com.dbcheck.app.sync.HealthConnectManager
import com.dbcheck.app.sync.HealthConnectSyncResult
import com.dbcheck.app.util.HapticFeedbackHelper
import com.dbcheck.app.widget.DbCheckWidgetReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

data class SessionStats(
    val minDb: Float = Float.MAX_VALUE,
    val avgDb: Float = 0f,
    val maxDb: Float = 0f,
    val peakDb: Float = 0f,
    val sampleCount: Int = 0,
    val totalEnergy: Double = 0.0,
)

data class LiveExposureState(
    val standard: DosimeterStandard = DosimeterStandard.NIOSH_REL,
    val laeqDb: Float = 0f,
    val durationMs: Long = 0L,
    val twaDb: Float = 0f,
    val dosePercent: Float = 0f,
    val projectedDosePercent: Float = 0f,
    val remainingExposureMs: Long? = null,
    val sampleCount: Int = 0,
)

internal fun SessionStats.withReading(reading: DecibelReading): SessionStats {
    val newCount = sampleCount + 1
    val newTotalEnergy = totalEnergy + DecibelMath.energyFromDb(reading.weightedDb)
    return copy(
        minDb = minOf(minDb, reading.weightedDb),
        maxDb = maxOf(maxDb, reading.weightedDb),
        peakDb = maxOf(peakDb, reading.peakDb),
        avgDb = DecibelMath.energyAverageDb(newTotalEnergy, newCount) ?: 0f,
        sampleCount = newCount,
        totalEnergy = newTotalEnergy,
    )
}

private data class RuntimeAudioPreferences(
    val frequencyWeighting: String,
    val micSensitivityOffset: Float,
    val responseTime: ResponseTime,
    val dosimeterStandard: DosimeterStandard,
    val isProUser: Boolean,
    val exposureAlertsEnabled: Boolean,
    val peakWarningsEnabled: Boolean,
    val notificationThreshold: Int,
    val notificationSchedule: NoiseNotificationSchedule,
    val soundDetectionEnabled: Boolean,
    val soundDetectionPersistenceEnabled: Boolean,
    val wavRecordingEnabled: Boolean,
    val audibleAlarmEnabled: Boolean,
    val ttsRiskPromptEnabled: Boolean,
    val voiceBaselineLevelDb: Float?,
    val selectedAudioInputDeviceId: Int?,
)

private fun UserPreferences.toRuntimeAudioPreferences(): RuntimeAudioPreferences = RuntimeAudioPreferences(
        frequencyWeighting = ProAudioPreferencePolicy.weighting(this),
        micSensitivityOffset = ProAudioPreferencePolicy.micOffset(this),
        responseTime = ProAudioPreferencePolicy.responseTime(isProUser = isProUser, responseTime = responseTime),
        dosimeterStandard =
            ProAudioPreferencePolicy.dosimeterStandard(
                isProUser = isProUser,
                dosimeterStandard = dosimeterStandard,
            ),
        isProUser = isProUser,
        exposureAlertsEnabled = exposureAlertsEnabled,
        peakWarningsEnabled = peakWarningsEnabled,
        notificationThreshold = notificationThreshold,
        notificationSchedule = notificationSchedule,
        soundDetectionEnabled = isProUser && soundDetectionEnabled,
        soundDetectionPersistenceEnabled = isProUser && soundDetectionEnabled && soundDetectionPersistenceEnabled,
        wavRecordingEnabled = isProUser && wavRecordingDefaultEnabled,
        audibleAlarmEnabled = isProUser && audibleAlarmEnabled,
        ttsRiskPromptEnabled = isProUser && ttsRiskPromptEnabled,
        voiceBaselineLevelDb =
            voiceBaselineLevelDb
                ?.takeIf { isProUser && soundDetectionEnabled && voiceBaselineSampleCount > 0 },
        selectedAudioInputDeviceId = selectedAudioInputDeviceId.takeIf { isProUser },
    )

private data class SessionCompletionSnapshot(
    val sessionId: Long,
    val startTime: Long,
    val endTime: Long,
    val minDb: Float,
    val avgDb: Float,
    val maxDb: Float,
    val peakDb: Float,
    val frequencyWeighting: String,
    val pendingMeasurements: List<SessionMeasurement>,
)

private fun SessionCompletionSnapshot.toMeasurementSummary(): SessionMeasurementSummary = SessionMeasurementSummary(
    minDb = minDb,
    avgDb = avgDb,
    maxDb = maxDb,
    peakDb = peakDb,
    frequencyWeighting = frequencyWeighting,
)

private fun SessionStats.toMeasurementSummary(frequencyWeighting: String): SessionMeasurementSummary =
    SessionMeasurementSummary(
        minDb = minDb.takeIf { it != Float.MAX_VALUE } ?: 0f,
        avgDb = avgDb,
        maxDb = maxDb,
        peakDb = peakDb,
        frequencyWeighting = frequencyWeighting,
    )

@Suppress("LargeClass")
@Singleton
class AudioSessionManager
        @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val audioEngine: AudioEngine,
        private val soundClassifier: SoundClassifier,
        private val sessionRepository: SessionRepository,
        private val measurementRepository: MeasurementRepository,
        private val sleepSessionRepository: SleepSessionRepository,
        private val soundDetectionRepository: SoundDetectionRepository,
        private val hearingTestRepository: HearingTestRepository,
        private val wavRecordingFileStore: WavRecordingFileStore,
        private val sessionLocationCapturePort: SessionLocationCapturePort,
        private val preferencesRepository: PreferencesRepository,
        private val healthConnectManager: HealthConnectManager,
        private val notificationHelper: NotificationHelper,
        private val hapticFeedbackHelper: HapticFeedbackHelper,
        private val audibleAlarmPlaybackController: AudibleAlarmPlaybackController,
        private val ttsRiskPromptController: TtsRiskPromptController,
        @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + defaultDispatcher)
        private var recordingJob: Job? = null
        private var preferencesJob: Job? = null
        private var measurementCollectionJob: Job? = null
        private var soundDetectionCollectionJob: Job? = null
        private var hearingBaselineJob: Job? = null
        private var startInProgress = false
        private var currentSessionId: Long? = null
        private var sessionStartTime: Long = 0L
        private var currentFrequencyWeighting: String = WeightingType.DEFAULT.name
        private var currentResponseTime: ResponseTime = ResponseTime.FAST
        private var currentDosimeterStandard: DosimeterStandard = DosimeterStandard.NIOSH_REL
        private var currentWeightingType: WeightingType? = null
        private var currentAlertPreferences = UserPreferences()
        private var pendingSleepRecordingConfig: SleepRecordingConfig? = null

        @Volatile
        private var soundDetectionActive = false

        @Volatile
        private var soundDetectionPersistenceActive = false

        @Volatile
        private var wavRecordingPreferenceEnabled = false

        @Volatile
        private var wavRecordingActive = false

        @Volatile
        private var audibleAlarmEnabled = false

        @Volatile
        private var ttsRiskPromptEnabled = false

        @Volatile
        private var currentIsProUser = false

        @Volatile
        private var currentVoiceBaselineLevelDb: Float? = null

        @Volatile
        private var currentHasHearingBaseline = false
        private var lastPersistedSoundDetectionLabel: String? = null
        private var lastMeasurementFlushTime = 0L
        private var liveExposureTotalEnergy = 0.0
        private var interruptedSessionRecoveryComplete = false
        private var currentSessionLocationCaptured = false
        private val pendingMeasurements =
            java.util.Collections.synchronizedList(mutableListOf<SessionMeasurement>())
        private val measurementSampler = MeasurementPersistenceSampler()
        private val noiseAlertEvaluator = NoiseAlertEvaluator()
        private val voiceBaselineCalibrator = VoiceBaselineCalibrator()
        private val voiceVolumeWarningEvaluator = VoiceVolumeWarningEvaluator()
        private val sessionLifecycleMutex = Mutex()
        private val measurementFlushMutex = Mutex()
        private val resetStatsAfterCompletion = AtomicBoolean(false)

        private val _sessionStats = MutableStateFlow(SessionStats())
        val sessionStats: StateFlow<SessionStats> = _sessionStats

        private val _liveExposureState = MutableStateFlow(LiveExposureState())
        val liveExposureState: StateFlow<LiveExposureState> = _liveExposureState

        private val _liveEnvironmentMixCounts = MutableStateFlow(EnvironmentExposureMixCounts())
        val liveEnvironmentMixCounts: StateFlow<EnvironmentExposureMixCounts> = _liveEnvironmentMixCounts

        private val _soundDetectionState = MutableStateFlow(SoundDetectionState())
        val soundDetectionState: StateFlow<SoundDetectionState> = _soundDetectionState

        private val _isRecording = MutableStateFlow(false)
        val isRecording: StateFlow<Boolean> = _isRecording

        private val _activeSessionStartTimeMs = MutableStateFlow<Long?>(null)
        val activeSessionStartTimeMs: StateFlow<Long?> = _activeSessionStartTimeMs

        private val _completedSessionIds = MutableSharedFlow<Long>(extraBufferCapacity = 1)
        val completedSessionIds: SharedFlow<Long> = _completedSessionIds.asSharedFlow()

        private val _healthConnectSyncFailures = MutableSharedFlow<String>(extraBufferCapacity = 1)
        val healthConnectSyncFailures: SharedFlow<String> = _healthConnectSyncFailures.asSharedFlow()

        private val _recordingFailures = MutableSharedFlow<AudioRecordingFailure>(extraBufferCapacity = 1)
        val recordingFailures: SharedFlow<AudioRecordingFailure> = _recordingFailures.asSharedFlow()

        fun reportRecordingFailure(failure: AudioRecordingFailure) {
            scope.launch {
                handleAudioRecordingFailure(failure)
            }
        }

        fun previewAudibleAlarm(isProUser: Boolean) {
            audibleAlarmPlaybackController.preview(isProUser)
        }

        fun captureVoiceBaseline(isProUser: Boolean): VoiceBaselineCapture? {
            if (!isProUser || !_isRecording.value || !soundDetectionActive) return null
            return voiceBaselineCalibrator.capture(capturedAtMs = System.currentTimeMillis())
        }

        suspend fun startSession(): Boolean = startSessionInternal(sleepRecordingConfig = null)

        suspend fun startSleepSession(config: SleepRecordingConfig): Boolean = startSessionInternal(
            sleepRecordingConfig = config,
        )

        private suspend fun startSessionInternal(sleepRecordingConfig: SleepRecordingConfig?): Boolean =
            sessionLifecycleMutex.withLock {
            if (!_isRecording.value && !startInProgress && currentSessionId == null) {
                recoverInterruptedSessionIfNeededLocked()
            }
            when {
                _isRecording.value || startInProgress -> true
                currentSessionId != null -> false
                !context.hasMicrophonePermission() -> false
                else -> startAudioSession(sleepRecordingConfig)
            }
        }

        private suspend fun startAudioSession(sleepRecordingConfig: SleepRecordingConfig?): Boolean {
            startInProgress = true
            prepareSessionRuntime(sleepRecordingConfig)
            applyRuntimeAudioPreferences(
                preferencesRepository.userPreferences
                    .first()
                    .toRuntimeAudioPreferences(),
                updateMeasurementAudio = true,
            )
            currentHasHearingBaseline = hearingTestRepository.getLatestResult().first() != null
            startPreferenceCollection()
            startHearingBaselineCollection()
            val recordingLaunch =
                scope.launchAudioRecording(
                    audioEngine = audioEngine,
                    onRecordingStarted = ::onAudioRecordingStarted,
                    onRecordingFinished = ::handleAudioRecordingResult,
                )
            recordingJob = recordingLaunch.job
            val started = recordingLaunch.started.await()
            startInProgress = false
            if (!started) {
                cleanupRecordingRuntime()
            }
            return started
        }

        private fun prepareSessionRuntime(sleepRecordingConfig: SleepRecordingConfig?) {
            pendingSleepRecordingConfig = sleepRecordingConfig
            sessionStartTime = System.currentTimeMillis()
            lastMeasurementFlushTime = sessionStartTime
            currentSessionLocationCaptured = false
            noiseAlertEvaluator.reset(sessionStartTime)
            _sessionStats.value = SessionStats()
            resetLiveExposureState(currentDosimeterStandard)
            resetLiveEnvironmentMixCounts()
            resetSoundDetectionState()
            resetWavRecordingState()
            voiceBaselineCalibrator.reset()
            voiceVolumeWarningEvaluator.reset()
            audibleAlarmPlaybackController.reset()
            ttsRiskPromptController.reset()
            currentHasHearingBaseline = false
        }

        private fun startPreferenceCollection() {
            preferencesJob =
                scope.launch {
                    preferencesRepository.userPreferences
                        .map { it.toRuntimeAudioPreferences() }
                        .distinctUntilChanged()
                        .collect { prefs ->
                            applyRuntimeAudioPreferences(prefs, updateMeasurementAudio = false)
                        }
                }
        }

        private fun startHearingBaselineCollection() {
            hearingBaselineJob =
                scope.launch {
                    hearingTestRepository.getLatestResult().collect { result ->
                        currentHasHearingBaseline = result != null
                    }
                }
        }

        private suspend fun applyRuntimeAudioPreferences(
            prefs: RuntimeAudioPreferences,
            updateMeasurementAudio: Boolean,
        ) {
            if (updateMeasurementAudio) {
                val weightingType = WeightingType.fromPreference(prefs.frequencyWeighting)
                if (currentWeightingType != weightingType) {
                    currentWeightingType = weightingType
                    currentFrequencyWeighting = weightingType.name
                    audioEngine.setWeighting(weightingType)
                }
                audioEngine.setCalibrationOffset(prefs.micSensitivityOffset)
                audioEngine.setResponseTime(prefs.responseTime)
                audioEngine.setPreferredAudioInputDeviceId(prefs.selectedAudioInputDeviceId)
                currentResponseTime = prefs.responseTime
            }
            updateLiveExposureStandard(prefs.dosimeterStandard)
            audioEngine.setSpectralAnalysisEnabled(prefs.isProUser)
            applySoundDetectionEnabled(
                enabled = prefs.soundDetectionEnabled,
                persistenceEnabled = prefs.soundDetectionPersistenceEnabled,
            )
            applyWavRecordingEnabled(prefs.wavRecordingEnabled)
            audibleAlarmEnabled = prefs.audibleAlarmEnabled
            ttsRiskPromptEnabled = prefs.ttsRiskPromptEnabled
            if (!ttsRiskPromptEnabled) {
                ttsRiskPromptController.reset()
            }
            currentIsProUser = prefs.isProUser
            currentVoiceBaselineLevelDb = prefs.voiceBaselineLevelDb
            currentAlertPreferences =
                UserPreferences(
                    exposureAlertsEnabled = prefs.exposureAlertsEnabled,
                    peakWarningsEnabled = prefs.peakWarningsEnabled,
                    notificationThreshold = prefs.notificationThreshold,
                    notificationSchedule = prefs.notificationSchedule,
                )
        }

        private suspend fun onAudioRecordingStarted() {
            measurementSampler.reset()
            synchronized(pendingMeasurements) {
                pendingMeasurements.clear()
            }
            val sessionId =
                sessionRepository.createActiveSession(
                    startTime = sessionStartTime,
                    frequencyWeighting = currentFrequencyWeighting,
                    audioInputDevice = audioEngine.audioInputInfo.value.toSessionAudioInputDeviceMetadata(),
                )
            currentSessionId = sessionId
            createSleepSessionIfNeeded(sessionId)
            startWavRecordingIfNeeded()
            audibleAlarmPlaybackController.startMonitoring()
            captureAndPersistSessionLocationIfNeeded(sessionId)
            measurementCollectionJob =
                scope.launch {
                    audioEngine.decibelFlow.collect { reading ->
                        runCatching {
                            handleDecibelReading(reading)
                        }.onFailure { error ->
                            if (error is CancellationException) throw error
                            _isRecording.value = false
                            _activeSessionStartTimeMs.value = null
                            startInProgress = false
                            audioEngine.stopRecording()
                            abortWavRecording()
                            audibleAlarmPlaybackController.stopMonitoring()
                            ttsRiskPromptController.reset()
                            cleanupRecordingJobs(cancelRecordingJob = true)
                            _recordingFailures.tryEmit(AudioRecordingFailure.PersistenceFailed)
                            completeCurrentSession(emitCompleted = false)
                        }
                    }
                }
            soundDetectionCollectionJob =
                scope.launch {
                    audioEngine.soundDetectionWindows.collect { window ->
                        handleSoundDetectionWindow(window)
                    }
                }
            _activeSessionStartTimeMs.value = sessionStartTime
            _isRecording.value = true
        }

        private suspend fun handleAudioRecordingResult(result: AudioRecordingResult) {
            when (result) {
                AudioRecordingResult.Stopped -> Unit
                is AudioRecordingResult.Failed -> handleAudioRecordingFailure(result.failure)
            }
        }

        private suspend fun handleAudioRecordingFailure(failure: AudioRecordingFailure) {
            _isRecording.value = false
            _activeSessionStartTimeMs.value = null
            startInProgress = false
            resetSoundDetectionState()
            abortWavRecording()
            audibleAlarmPlaybackController.stopMonitoring()
            ttsRiskPromptController.reset()
            cleanupRecordingJobs(cancelRecordingJob = false)
            _recordingFailures.tryEmit(failure)
            completeCurrentSession(emitCompleted = false)
        }

        private suspend fun handleDecibelReading(reading: DecibelReading) {
            if (reading.timestamp < sessionStartTime) return

            val statsBeforeReading = _sessionStats.value
            val shouldPersist =
                currentSessionId != null &&
                    measurementSampler.shouldPersist(
                        reading = reading,
                        currentMaxDbBeforeReading = statsBeforeReading.maxDb,
                        currentPeakDbBeforeReading = statsBeforeReading.peakDb,
                    )

            updateStats(reading)
            voiceBaselineCalibrator.onReading(reading.weightedDb)
            dispatchVoiceVolumeWarning(reading)
            dispatchAudibleAlarm(reading)
            dispatchNoiseAlerts(reading, _sessionStats.value)

            currentSessionId?.let { _ ->
                if (shouldPersist) {
                    addPendingMeasurement(reading)
                    measurementSampler.markPersisted(reading)
                }
                flushMeasurementsIfNeeded(reading.timestamp)
            }
        }

        fun stopSession(emitCompleted: Boolean = true) {
            scope.launch {
                sessionLifecycleMutex.withLock {
                    stopSessionLocked(emitCompleted)
                }
            }
        }

        private suspend fun stopSessionLocked(emitCompleted: Boolean) {
            _isRecording.value = false
            _activeSessionStartTimeMs.value = null
            startInProgress = false
            audioEngine.stopRecording()
            stopWavRecording()
            audibleAlarmPlaybackController.stopMonitoring()
            ttsRiskPromptController.reset()
            resetSoundDetectionState()
            cleanupRecordingJobs(cancelRecordingJob = true)
            completeCurrentSession(emitCompleted)
        }

        private fun completeCurrentSession(emitCompleted: Boolean) {
            scope.launch {
                val snapshotResult =
                    runCatching {
                        measurementFlushMutex.withLock {
                            val snapshot = captureCompletionSnapshot() ?: return@withLock null
                            captureAndPersistSessionLocationIfNeeded(snapshot.sessionId)
                            sessionRepository.completeSessionWithMeasurements(
                                id = snapshot.sessionId,
                                endTime = snapshot.endTime,
                                measurements = snapshot.pendingMeasurements,
                                summary = snapshot.toMeasurementSummary(),
                            )
                            lastMeasurementFlushTime = snapshot.endTime
                            currentSessionId = null
                            currentSessionLocationCaptured = false
                            pendingSleepRecordingConfig = null
                            measurementSampler.reset()
                            noiseAlertEvaluator.reset(0L)
                            voiceBaselineCalibrator.reset()
                            voiceVolumeWarningEvaluator.reset()
                            audibleAlarmPlaybackController.stopMonitoring()
                            ttsRiskPromptController.reset()
                            synchronized(pendingMeasurements) {
                                pendingMeasurements.clear()
                            }
                            snapshot
                        }
                    }
                val snapshot =
                    snapshotResult.getOrElse { error ->
                        if (error is CancellationException) throw error
                        _recordingFailures.tryEmit(AudioRecordingFailure.PersistenceFailed)
                        return@launch
                    } ?: run {
                        if (resetStatsAfterCompletion.getAndSet(false)) {
                            _sessionStats.value = SessionStats()
                            resetLiveExposureState(currentDosimeterStandard)
                            resetLiveEnvironmentMixCounts()
                            resetSoundDetectionState()
                        }
                        return@launch
                    }
                publishCompletionSideEffects(snapshot, emitCompleted)
                if (resetStatsAfterCompletion.getAndSet(false)) {
                    _sessionStats.value = SessionStats()
                    resetLiveExposureState(currentDosimeterStandard)
                    resetLiveEnvironmentMixCounts()
                    resetSoundDetectionState()
                }
            }
        }

        private suspend fun publishCompletionSideEffects(snapshot: SessionCompletionSnapshot, emitCompleted: Boolean) {
            val completedDomainSession =
                Session(
                    id = snapshot.sessionId,
                    startTime = snapshot.startTime,
                    endTime = snapshot.endTime,
                    minDb = snapshot.minDb,
                    avgDb = snapshot.avgDb,
                    maxDb = snapshot.maxDb,
                    peakDb = snapshot.peakDb,
                    name = null,
                    emoji = null,
                    tags = emptyList(),
                    isActive = false,
                    frequencyWeighting = snapshot.frequencyWeighting,
                )
            if (
                emitCompleted &&
                preferencesRepository.userPreferences.first().healthConnectEnabled
            ) {
                val syncResult =
                    runCatching {
                        val report = buildSessionReport(completedDomainSession, measurementRepository)
                        healthConnectManager.writeNoiseDose(report)
                    }
                syncResult
                    .onSuccess { syncResult ->
                        when (syncResult) {
                            is HealthConnectSyncResult.Failed ->
                                _healthConnectSyncFailures.emit(syncResult.reason)

                            is HealthConnectSyncResult.Skipped ->
                                _healthConnectSyncFailures.emit(syncResult.reason)

                            HealthConnectSyncResult.Written,
                            -> Unit
                        }
                    }.onFailure { error ->
                        if (error is CancellationException) throw error
                        _healthConnectSyncFailures.emit(context.getString(R.string.health_connect_sync_failed))
                    }
            }
            if (emitCompleted) {
                _completedSessionIds.emit(snapshot.sessionId)
            }

            updateWidgetsIgnoringFailures(context)
        }

        private fun cleanupRecordingJobs(cancelRecordingJob: Boolean) {
            if (cancelRecordingJob) {
                recordingJob?.cancel()
                recordingJob = null
            }
            preferencesJob?.cancel()
            measurementCollectionJob?.cancel()
            soundDetectionCollectionJob?.cancel()
            hearingBaselineJob?.cancel()
            preferencesJob = null
            measurementCollectionJob = null
            soundDetectionCollectionJob = null
            hearingBaselineJob = null
        }

        private suspend fun cleanupRecordingRuntime() {
            _isRecording.value = false
            _activeSessionStartTimeMs.value = null
            cleanupRecordingJobs(cancelRecordingJob = true)
            currentSessionId = null
            currentSessionLocationCaptured = false
            pendingSleepRecordingConfig = null
            abortWavRecording()
            audibleAlarmPlaybackController.stopMonitoring()
            ttsRiskPromptController.reset()
            measurementSampler.reset()
            noiseAlertEvaluator.reset(0L)
            voiceBaselineCalibrator.reset()
            voiceVolumeWarningEvaluator.reset()
            resetLiveExposureState(currentDosimeterStandard)
            currentHasHearingBaseline = false
            resetSoundDetectionState()
            synchronized(pendingMeasurements) {
                pendingMeasurements.clear()
            }
        }

        private fun captureCompletionSnapshot(): SessionCompletionSnapshot? {
            val sessionId =
                currentSessionId
                    ?: run {
                        measurementSampler.reset()
                        noiseAlertEvaluator.reset(0L)
                        audibleAlarmPlaybackController.stopMonitoring()
                        ttsRiskPromptController.reset()
                        synchronized(pendingMeasurements) {
                            pendingMeasurements.clear()
                        }
                        return null
            }
            measurementSampler.latestUnpersistedOnStop()?.let { reading ->
                addPendingMeasurement(reading)
                measurementSampler.markPersisted(reading)
            }

            val stats = _sessionStats.value
            val endTime = System.currentTimeMillis()
            val pending =
                synchronized(pendingMeasurements) {
                    pendingMeasurements.toList()
                }
            return SessionCompletionSnapshot(
                sessionId = sessionId,
                startTime = sessionStartTime,
                endTime = endTime,
                minDb = stats.minDb.takeIf { it != Float.MAX_VALUE } ?: 0f,
                avgDb = stats.avgDb,
                maxDb = stats.maxDb,
                peakDb = stats.peakDb,
                frequencyWeighting = currentFrequencyWeighting,
                pendingMeasurements = pending,
            )
        }

        private suspend fun captureAndPersistSessionLocationIfNeeded(sessionId: Long) {
            if (currentSessionLocationCaptured) return
            val location =
                runCatching {
                    sessionLocationCapturePort.captureOneShotLocation()
                }.getOrElse { error ->
                    if (error is CancellationException) throw error
                    null
                } ?: return

            runCatching {
                sessionRepository.updateSessionLocation(id = sessionId, location = location)
            }.onSuccess {
                currentSessionLocationCaptured = true
            }.onFailure { error ->
                if (error is CancellationException) throw error
            }
        }

        suspend fun recoverInterruptedSession() = sessionLifecycleMutex.withLock {
            if (_isRecording.value || startInProgress || currentSessionId != null) {
                interruptedSessionRecoveryComplete = true
                return@withLock
            }
            recoverInterruptedSessionIfNeededLocked()
        }

        private suspend fun recoverInterruptedSessionIfNeededLocked() {
            if (interruptedSessionRecoveryComplete) return
            recoverActiveSessionLocked()
            interruptedSessionRecoveryComplete = true
        }

        private suspend fun recoverActiveSessionLocked() {
            val activeSession = sessionRepository.getActiveSession().first() ?: return
            val measurements = measurementRepository.getSessionMeasurements(activeSession.id).first()
            val recoveredEndTime = measurements.maxOfOrNull { it.timestamp } ?: activeSession.startTime
            val weightedMeasurements = measurements.map { it.dbWeighted }
            val hasFlushedRuntimeSummary =
                activeSession.avgDb > 0f ||
                    activeSession.maxDb > 0f ||
                    activeSession.peakDb > 0f ||
                    activeSession.minDb > 0f
            val recoveredMinDb =
                if (hasFlushedRuntimeSummary) {
                    activeSession.minDb
                } else {
                    weightedMeasurements.minOrNull() ?: 0f
                }
            val recoveredAvgDb =
                if (hasFlushedRuntimeSummary) {
                    activeSession.avgDb
                } else {
                    DecibelMath.energyAverageDb(weightedMeasurements) ?: 0f
                }
            val recoveredMaxDb =
                if (hasFlushedRuntimeSummary) {
                    activeSession.maxDb
                } else {
                    weightedMeasurements.maxOrNull() ?: 0f
                }
            sessionRepository.completeSessionWithMeasurements(
                id = activeSession.id,
                endTime = recoveredEndTime,
                measurements = emptyList(),
                summary =
                    SessionMeasurementSummary(
                        minDb = recoveredMinDb,
                        avgDb = recoveredAvgDb,
                        maxDb = recoveredMaxDb,
                        peakDb = activeSession.peakDb,
                        frequencyWeighting = activeSession.frequencyWeighting,
                    ),
            )
            updateWidgetsIgnoringFailures(context)
        }

        fun resetStats() {
            if (_isRecording.value || startInProgress || currentSessionId != null) {
                resetStatsAfterCompletion.set(true)
            } else {
                resetStatsAfterCompletion.set(false)
                _sessionStats.value = SessionStats()
                resetLiveExposureState(currentDosimeterStandard)
                resetLiveEnvironmentMixCounts()
                resetSoundDetectionState()
            }
        }

        private suspend fun flushMeasurements() {
            measurementFlushMutex.withLock {
                val sessionId = currentSessionId ?: return@withLock
                val stats = _sessionStats.value
                val frequencyWeighting = currentFrequencyWeighting
                val toFlush =
                    synchronized(pendingMeasurements) {
                        pendingMeasurements.toList()
                    }
                if (toFlush.isEmpty()) return@withLock

                withContext(NonCancellable) {
                    sessionRepository.recordActiveSessionMeasurements(
                        id = sessionId,
                        measurements = toFlush,
                        summary = stats.toMeasurementSummary(frequencyWeighting),
                    )
                    synchronized(pendingMeasurements) {
                        toFlush.forEach { pendingMeasurements.remove(it) }
                    }
                }
                lastMeasurementFlushTime = System.currentTimeMillis()
            }
        }

        private suspend fun flushMeasurementsIfNeeded(readingTimestamp: Long) {
            val shouldFlush =
                synchronized(pendingMeasurements) {
                    pendingMeasurements.size >= MAX_PENDING_MEASUREMENTS ||
                        (
                            pendingMeasurements.isNotEmpty() &&
                                readingTimestamp - lastMeasurementFlushTime >= FLUSH_INTERVAL_MS
                        )
                }
            if (shouldFlush) {
                flushMeasurements()
                lastMeasurementFlushTime = readingTimestamp
            }
        }

        private fun addPendingMeasurement(reading: DecibelReading) {
            pendingMeasurements.add(
                SessionMeasurement(
                    timestamp = reading.timestamp,
                    dbValue = reading.instantDb,
                    dbWeighted = reading.weightedDb,
                    peakDb = reading.peakDb,
                    aWeightedDb = reading.aWeightedDb,
                    responseTime = currentResponseTime.name,
                ),
            )
        }

        private fun dispatchNoiseAlerts(reading: DecibelReading, stats: SessionStats) {
            noiseAlertEvaluator
                .evaluate(
                    reading = reading,
                    stats = stats,
                    preferences = currentAlertPreferences,
                    liveExposure = _liveExposureState.value,
                ).forEach { decision ->
                    val delivered =
                        when (decision) {
                            is NoiseAlertDecision.Exposure ->
                                notificationHelper.sendExposureAlert(
                                    avgDb = decision.avgDb,
                                    durationMinutes = decision.durationMinutes,
                                )

                            is NoiseAlertDecision.Peak ->
                                notificationHelper.sendPeakWarning(decision.peakDb)
                        }
                    dispatchTtsRiskPrompt(decision = decision, timestampMs = reading.timestamp)
                    if (delivered) {
                        noiseAlertEvaluator.markDelivered(decision)
                    }
                }
        }

        private fun dispatchTtsRiskPrompt(decision: NoiseAlertDecision, timestampMs: Long) {
            val riskEvent = decision.toTtsRiskPromptRiskEvent()
            val soundDetectionAvailable = isTtsSoundDetectionAvailable()
            val promptMessage =
                if (shouldBuildTtsRiskPrompt(riskEvent, soundDetectionAvailable)) {
                    context.getString(R.string.tts_risk_prompt_high_noise)
                } else {
                    ""
                }
            ttsRiskPromptController.onRiskEvent(
                riskEvent = riskEvent,
                timestampMs = timestampMs,
                isEnabled = ttsRiskPromptEnabled,
                isProUser = currentIsProUser,
                hasHearingBaseline = currentHasHearingBaseline,
                soundDetectionAvailable = soundDetectionAvailable,
                promptMessage = promptMessage,
            )
        }

        private fun shouldBuildTtsRiskPrompt(
            riskEvent: TtsRiskPromptRiskEvent,
            soundDetectionAvailable: Boolean,
        ): Boolean = canUseTtsRiskPrompt &&
                soundDetectionAvailable &&
                riskEvent.isDosimeterTtsRiskEvent

        private val canUseTtsRiskPrompt: Boolean
            get() = ttsRiskPromptEnabled && currentIsProUser && currentHasHearingBaseline

        private fun NoiseAlertDecision.toTtsRiskPromptRiskEvent(): TtsRiskPromptRiskEvent = when (this) {
                is NoiseAlertDecision.Exposure ->
                    when (trigger) {
                        NoiseExposureAlertTrigger.AVERAGE_DURATION -> TtsRiskPromptRiskEvent.AverageDuration
                        NoiseExposureAlertTrigger.DOSE -> TtsRiskPromptRiskEvent.DosimeterDose
                        NoiseExposureAlertTrigger.PROJECTED_DOSE -> TtsRiskPromptRiskEvent.ProjectedDose
                    }

                is NoiseAlertDecision.Peak -> TtsRiskPromptRiskEvent.Peak
            }

        private val TtsRiskPromptRiskEvent.isDosimeterTtsRiskEvent: Boolean
            get() = this == TtsRiskPromptRiskEvent.DosimeterDose || this == TtsRiskPromptRiskEvent.ProjectedDose

        private fun isTtsSoundDetectionAvailable(): Boolean = soundDetectionActive &&
                _soundDetectionState.value.isEnabled &&
                _soundDetectionState.value.current != null &&
                _soundDetectionState.value.error == null

        private fun dispatchAudibleAlarm(reading: DecibelReading) {
            audibleAlarmPlaybackController.onReading(
                weightedDb = reading.weightedDb,
                timestampMs = reading.timestamp,
                isEnabled = audibleAlarmEnabled,
                isProUser = currentIsProUser,
            )
        }

        private fun dispatchVoiceVolumeWarning(reading: DecibelReading) {
            if (!currentIsProUser || !soundDetectionActive) {
                voiceVolumeWarningEvaluator.reset()
                return
            }
            when (
                val decision =
                    voiceVolumeWarningEvaluator.evaluate(
                        weightedDb = reading.weightedDb,
                        timestampMs = reading.timestamp,
                        baselineDb = currentVoiceBaselineLevelDb,
                    )
            ) {
                is VoiceVolumeWarningEvaluation.Trigger -> {
                    runCatching {
                        hapticFeedbackHelper.mediumClick()
                    }
                    notificationHelper.sendVoiceVolumeWarning(
                        currentDb = decision.currentDb,
                        baselineDb = decision.baselineDb,
                    )
                }

                VoiceVolumeWarningEvaluation.MissingBaseline,
                VoiceVolumeWarningEvaluation.NotSpeech,
                is VoiceVolumeWarningEvaluation.BelowThreshold,
                is VoiceVolumeWarningEvaluation.Waiting,
                is VoiceVolumeWarningEvaluation.CoolingDown,
                -> Unit
            }
        }

        private fun updateStats(reading: DecibelReading) {
            _sessionStats.value = _sessionStats.value.withReading(reading)
            updateLiveExposure(reading)
            _liveEnvironmentMixCounts.value = _liveEnvironmentMixCounts.value.withWeightedDb(reading.weightedDb)
        }

        private fun updateLiveExposure(reading: DecibelReading) {
            liveExposureTotalEnergy += DecibelMath.energyFromDb(reading.aWeightedDb)
            val sampleCount = _liveExposureState.value.sampleCount + 1
            val laeqDb = DecibelMath.energyAverageDb(liveExposureTotalEnergy, sampleCount) ?: 0f
            val durationMs = (reading.timestamp - sessionStartTime).coerceAtLeast(0L)
            val exposure =
                DosimeterCalculator.calculate(
                    standard = currentDosimeterStandard,
                    laeqDb = laeqDb,
                    durationMs = durationMs,
                )
            _liveExposureState.value =
                LiveExposureState(
                    standard = exposure.standard,
                    laeqDb = laeqDb,
                    durationMs = durationMs,
                    twaDb = exposure.twaDb,
                    dosePercent = exposure.dosePercent,
                    projectedDosePercent = exposure.projectedDosePercent,
                    remainingExposureMs = exposure.remainingExposureMs,
                    sampleCount = sampleCount,
                )
        }

        private fun updateLiveExposureStandard(standard: DosimeterStandard) {
            currentDosimeterStandard = standard
            val currentState = _liveExposureState.value
            if (currentState.sampleCount == 0) {
                _liveExposureState.value = LiveExposureState(standard = standard)
                return
            }
            val exposure =
                DosimeterCalculator.calculate(
                    standard = standard,
                    laeqDb = currentState.laeqDb,
                    durationMs = currentState.durationMs,
                )
            _liveExposureState.value =
                currentState.copy(
                    standard = exposure.standard,
                    twaDb = exposure.twaDb,
                    dosePercent = exposure.dosePercent,
                    projectedDosePercent = exposure.projectedDosePercent,
                    remainingExposureMs = exposure.remainingExposureMs,
                )
        }

        private fun resetLiveExposureState(standard: DosimeterStandard) {
            liveExposureTotalEnergy = 0.0
            _liveExposureState.value = LiveExposureState(standard = standard)
        }

        private fun resetLiveEnvironmentMixCounts() {
            _liveEnvironmentMixCounts.value = EnvironmentExposureMixCounts()
        }

        private fun applySoundDetectionEnabled(enabled: Boolean, persistenceEnabled: Boolean) {
            soundDetectionActive = enabled
            soundDetectionPersistenceActive = persistenceEnabled
            if (!persistenceEnabled) {
                lastPersistedSoundDetectionLabel = null
            }
            audioEngine.setSoundDetectionEnabled(enabled)
            if (enabled) {
                _soundDetectionState.value = _soundDetectionState.value.copy(isEnabled = true)
            } else {
                resetSoundDetectionState()
            }
            if (!enabled) {
                voiceVolumeWarningEvaluator.reset()
            }
        }

        private suspend fun handleSoundDetectionWindow(window: FloatArray) {
            if (soundDetectionActive) {
                val classificationResult =
                    runCatching {
                        soundClassifier.classify(window)
                    }.onFailure { error ->
                        if (error is CancellationException) throw error
                        handleSoundClassificationFailure()
                    }
                if (soundDetectionActive && classificationResult.isSuccess) {
                    val classification = classificationResult.getOrNull()
                    val timestamp = System.currentTimeMillis()
                    _soundDetectionState.value =
                        _soundDetectionState.value.withClassification(
                            classification = classification,
                            timestamp = timestamp,
                        )
                    voiceBaselineCalibrator.onClassification(classification)
                    voiceVolumeWarningEvaluator.onClassification(classification)
                    persistSoundDetectionIfNeeded(classification, timestamp)
                }
            }
        }

        private suspend fun createSleepSessionIfNeeded(sessionId: Long) {
            val config = pendingSleepRecordingConfig ?: return
            sleepSessionRepository.createSleepSession(
                sessionId = sessionId,
                config = config,
                createdAt = sessionStartTime,
            )
        }

        private fun handleSoundClassificationFailure() {
            if (soundDetectionActive) {
                voiceBaselineCalibrator.onClassification(null)
                voiceVolumeWarningEvaluator.onClassification(null)
                _soundDetectionState.value =
                    _soundDetectionState.value.withError(
                        SoundDetectionError.CLASSIFICATION_UNAVAILABLE,
                    )
            }
        }

        private suspend fun persistSoundDetectionIfNeeded(
            classification: com.dbcheck.app.domain.audio.SoundClassification?,
            timestamp: Long,
        ) {
            val sessionId = currentSessionId
            when {
                !soundDetectionPersistenceActive -> Unit

                sessionId == null -> Unit

                classification == null -> lastPersistedSoundDetectionLabel = null

                classification.label != lastPersistedSoundDetectionLabel -> {
                    runCatching {
                        soundDetectionRepository.recordEvent(
                            SoundDetectionEvent(
                                sessionId = sessionId,
                                timestamp = timestamp,
                                label = classification.label,
                                confidence = classification.confidence,
                            ),
                        )
                    }.onSuccess {
                        lastPersistedSoundDetectionLabel = classification.label
                    }.onFailure { error ->
                        if (error is CancellationException) throw error
                    }
                }
            }
        }

        private fun resetSoundDetectionState() {
            soundDetectionActive = false
            soundDetectionPersistenceActive = false
            lastPersistedSoundDetectionLabel = null
            voiceBaselineCalibrator.reset()
            voiceVolumeWarningEvaluator.reset()
            _soundDetectionState.value = SoundDetectionState()
        }

        private suspend fun applyWavRecordingEnabled(enabled: Boolean) {
            wavRecordingPreferenceEnabled = enabled
            if (enabled) {
                startWavRecordingIfNeeded()
            } else {
                stopWavRecording()
            }
        }

        private suspend fun startWavRecordingIfNeeded() {
            if (!wavRecordingPreferenceEnabled || wavRecordingActive) return
            val sessionId = currentSessionId ?: return
            val recordingFile =
                withContext(ioDispatcher) {
                    wavRecordingFileStore.createRecordingFile(sessionId, sessionStartTime)
                }
            audioEngine.startWavRecording(recordingFile)
            wavRecordingActive = true
        }

        private suspend fun stopWavRecording() {
            if (!wavRecordingActive) return
            audioEngine.stopWavRecording()
            wavRecordingActive = false
        }

        private suspend fun abortWavRecording() {
            if (!wavRecordingActive) return
            audioEngine.abortWavRecording()
            wavRecordingActive = false
        }

        private fun resetWavRecordingState() {
            wavRecordingPreferenceEnabled = false
            wavRecordingActive = false
        }
    }

private suspend fun updateWidgetsIgnoringFailures(context: Context) {
    runCatching {
        DbCheckWidgetReceiver.updateAllWidgets(context)
    }.onFailure { error ->
        if (error is CancellationException) throw error
    }
}

private fun AudioInputInfo.toSessionAudioInputDeviceMetadata(): SessionAudioInputDeviceMetadata? {
    if (selectedDeviceId == null && selectedDeviceName == null && routedDeviceName == null) return null
    return SessionAudioInputDeviceMetadata(
        selectedDeviceId = selectedDeviceId,
        selectedDeviceName = selectedDeviceName,
        routedDeviceName = routedDeviceName,
    )
}

private fun Context.hasMicrophonePermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

private suspend fun buildSessionReport(
    session: Session,
    measurementRepository: MeasurementRepository,
): SessionReportData {
    val measurements =
        measurementRepository
            .getReportMeasurementsForSession(session.id)
            .first()
    return SessionReportCalculator.build(session, measurements)
}

private const val MAX_PENDING_MEASUREMENTS = 10
private const val FLUSH_INTERVAL_MS = 1_000L
