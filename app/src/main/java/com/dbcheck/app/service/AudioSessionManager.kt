package com.dbcheck.app.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.dbcheck.app.data.local.db.entity.MeasurementEntity
import com.dbcheck.app.data.local.db.entity.SessionEntity
import com.dbcheck.app.data.local.preferences.model.ProAudioPreferencePolicy
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.model.toDomainModel
import com.dbcheck.app.data.repository.MeasurementRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.data.repository.SessionRepository
import com.dbcheck.app.di.DefaultDispatcher
import com.dbcheck.app.domain.audio.AudioEngine
import com.dbcheck.app.domain.audio.AudioRecordingFailure
import com.dbcheck.app.domain.audio.AudioRecordingResult
import com.dbcheck.app.domain.audio.DecibelReading
import com.dbcheck.app.domain.audio.WeightingType
import com.dbcheck.app.domain.noise.DecibelMath
import com.dbcheck.app.domain.report.ReportMeasurement
import com.dbcheck.app.domain.report.SessionReportCalculator
import com.dbcheck.app.domain.report.SessionReportData
import com.dbcheck.app.domain.session.Session
import com.dbcheck.app.sync.HealthConnectManager
import com.dbcheck.app.widget.DbCheckWidgetReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
    val isProUser: Boolean,
    val exposureAlertsEnabled: Boolean,
    val peakWarningsEnabled: Boolean,
    val notificationThreshold: Int,
)

private fun UserPreferences.toRuntimeAudioPreferences(): RuntimeAudioPreferences = RuntimeAudioPreferences(
        frequencyWeighting = ProAudioPreferencePolicy.weighting(this),
        micSensitivityOffset = ProAudioPreferencePolicy.micOffset(this),
        isProUser = isProUser,
        exposureAlertsEnabled = exposureAlertsEnabled,
        peakWarningsEnabled = peakWarningsEnabled,
        notificationThreshold = notificationThreshold,
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
    val pendingMeasurements: List<MeasurementEntity>,
)

@Singleton
class AudioSessionManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val audioEngine: AudioEngine,
        private val sessionRepository: SessionRepository,
        private val measurementRepository: MeasurementRepository,
        private val preferencesRepository: PreferencesRepository,
        private val healthConnectManager: HealthConnectManager,
        private val notificationHelper: NotificationHelper,
        @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + defaultDispatcher)
        private var recordingJob: Job? = null
        private var preferencesJob: Job? = null
        private var measurementCollectionJob: Job? = null
        private var startInProgress = false
        private var currentSessionId: Long? = null
        private var sessionStartTime: Long = 0L
        private var currentFrequencyWeighting: String = WeightingType.DEFAULT.name
        private var currentWeightingType: WeightingType? = null
        private var currentAlertPreferences = UserPreferences()
        private var lastMeasurementFlushTime = 0L
        private val pendingMeasurements =
            java.util.Collections.synchronizedList(mutableListOf<MeasurementEntity>())
        private val measurementSampler = MeasurementPersistenceSampler()
        private val noiseAlertEvaluator = NoiseAlertEvaluator()

        private val _sessionStats = MutableStateFlow(SessionStats())
        val sessionStats: StateFlow<SessionStats> = _sessionStats

        private val _isRecording = MutableStateFlow(false)
        val isRecording: StateFlow<Boolean> = _isRecording

        private val _completedSessionIds = MutableSharedFlow<Long>(extraBufferCapacity = 1)
        val completedSessionIds: SharedFlow<Long> = _completedSessionIds.asSharedFlow()

        private val _recordingFailures = MutableSharedFlow<AudioRecordingFailure>(extraBufferCapacity = 1)
        val recordingFailures: SharedFlow<AudioRecordingFailure> = _recordingFailures.asSharedFlow()

        suspend fun startSession(): Boolean = when {
            _isRecording.value || startInProgress -> true
            !hasMicrophonePermission() -> false
            else -> startAudioSession()
        }

        private suspend fun startAudioSession(): Boolean {
            startInProgress = true
            prepareSessionRuntime()
            applyRuntimeAudioPreferences(
                preferencesRepository.userPreferences
                    .first()
                    .toRuntimeAudioPreferences(),
            )
            startPreferenceCollection()
            val startResult = CompletableDeferred<Boolean>()
            recordingJob =
                scope.launch {
                    val result =
                        runCatching {
                            audioEngine.startRecording {
                                onAudioRecordingStarted()
                                startResult.complete(true)
                            }
                        }.getOrElse { error ->
                            if (!startResult.isCompleted) {
                                startResult.complete(false)
                            }
                            if (error is CancellationException) throw error
                            AudioRecordingResult.Failed(AudioRecordingFailure.StartFailed)
                        }
                    if (!startResult.isCompleted) {
                        startResult.complete(false)
                    }
                    handleAudioRecordingResult(result)
                }
            val started = startResult.await()
            startInProgress = false
            if (!started) {
                cleanupRecordingRuntime()
            }
            return started
        }

        private fun prepareSessionRuntime() {
            sessionStartTime = System.currentTimeMillis()
            lastMeasurementFlushTime = sessionStartTime
            noiseAlertEvaluator.reset(sessionStartTime)
            _sessionStats.value = SessionStats()
        }

        private fun startPreferenceCollection() {
            preferencesJob =
                scope.launch {
                    preferencesRepository.userPreferences
                        .map { it.toRuntimeAudioPreferences() }
                        .distinctUntilChanged()
                        .collect(::applyRuntimeAudioPreferences)
                }
        }

        private fun applyRuntimeAudioPreferences(prefs: RuntimeAudioPreferences) {
            val weightingType = WeightingType.fromPreference(prefs.frequencyWeighting)
            if (currentWeightingType != weightingType) {
                currentWeightingType = weightingType
                currentFrequencyWeighting = weightingType.name
                audioEngine.setWeighting(weightingType)
            }
            audioEngine.setCalibrationOffset(prefs.micSensitivityOffset)
            audioEngine.setSpectralAnalysisEnabled(prefs.isProUser)
            currentAlertPreferences =
                UserPreferences(
                    exposureAlertsEnabled = prefs.exposureAlertsEnabled,
                    peakWarningsEnabled = prefs.peakWarningsEnabled,
                    notificationThreshold = prefs.notificationThreshold,
                )
        }

        private suspend fun onAudioRecordingStarted() {
            measurementSampler.reset()
            synchronized(pendingMeasurements) {
                pendingMeasurements.clear()
            }
            val session =
                SessionEntity(
                    startTime = sessionStartTime,
                    isActive = true,
                )
            currentSessionId = sessionRepository.createSession(session)
            measurementCollectionJob =
                scope.launch {
                    audioEngine.decibelFlow.collect(::handleDecibelReading)
                }
            _isRecording.value = true
        }

        private fun handleAudioRecordingResult(result: AudioRecordingResult) {
            when (result) {
                AudioRecordingResult.Stopped -> Unit
                is AudioRecordingResult.Failed -> handleAudioRecordingFailure(result.failure)
            }
        }

        private fun handleAudioRecordingFailure(failure: AudioRecordingFailure) {
            _isRecording.value = false
            startInProgress = false
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
                    )

            updateStats(reading)
            dispatchNoiseAlerts(reading, _sessionStats.value)

            currentSessionId?.let { sessionId ->
                if (shouldPersist) {
                    addPendingMeasurement(sessionId, reading)
                    measurementSampler.markPersisted(reading)
                }
                flushMeasurementsIfNeeded(reading.timestamp)
            }
        }

        fun stopSession(emitCompleted: Boolean = true) {
            _isRecording.value = false
            startInProgress = false
            audioEngine.stopRecording()
            cleanupRecordingJobs(cancelRecordingJob = true)
            completeCurrentSession(emitCompleted)
        }

        private fun completeCurrentSession(emitCompleted: Boolean) {
            val snapshot = captureCompletionSnapshot() ?: return
            scope.launch {
                finishSession(snapshot, emitCompleted)
            }
        }

        private fun cleanupRecordingJobs(cancelRecordingJob: Boolean) {
            if (cancelRecordingJob) {
                recordingJob?.cancel()
                recordingJob = null
            }
            preferencesJob?.cancel()
            measurementCollectionJob?.cancel()
            preferencesJob = null
            measurementCollectionJob = null
        }

        private fun cleanupRecordingRuntime() {
            _isRecording.value = false
            cleanupRecordingJobs(cancelRecordingJob = true)
            currentSessionId = null
            measurementSampler.reset()
            noiseAlertEvaluator.reset(0L)
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
                        synchronized(pendingMeasurements) {
                            pendingMeasurements.clear()
                        }
                        return null
                    }
            currentSessionId = null
            measurementSampler.latestUnpersistedOnStop()?.let { reading ->
                addPendingMeasurement(sessionId, reading)
                measurementSampler.markPersisted(reading)
            }

            val stats = _sessionStats.value
            val endTime = System.currentTimeMillis()
            val pending =
                synchronized(pendingMeasurements) {
                    pendingMeasurements.toList().also {
                        pendingMeasurements.clear()
                    }
                }
            measurementSampler.reset()
            noiseAlertEvaluator.reset(0L)
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

        private suspend fun finishSession(snapshot: SessionCompletionSnapshot, emitCompleted: Boolean) {
            if (snapshot.pendingMeasurements.isNotEmpty()) {
                measurementRepository.insertMeasurements(snapshot.pendingMeasurements)
                lastMeasurementFlushTime = snapshot.endTime
            }
            val completedSession =
                SessionEntity(
                    id = snapshot.sessionId,
                    startTime = snapshot.startTime,
                    endTime = snapshot.endTime,
                    minDb = snapshot.minDb,
                    avgDb = snapshot.avgDb,
                    maxDb = snapshot.maxDb,
                    peakDb = snapshot.peakDb,
                    isActive = false,
                    frequencyWeighting = snapshot.frequencyWeighting,
                )
            sessionRepository.completeSession(
                id = snapshot.sessionId,
                endTime = snapshot.endTime,
                minDb = snapshot.minDb,
                avgDb = snapshot.avgDb,
                maxDb = snapshot.maxDb,
                peakDb = snapshot.peakDb,
                frequencyWeighting = snapshot.frequencyWeighting,
            )
            val completedDomainSession = completedSession.toDomainModel()
            if (emitCompleted && preferencesRepository.userPreferences.first().healthConnectEnabled) {
                val report = buildSessionReport(completedDomainSession)
                healthConnectManager.writeNoiseDose(completedDomainSession, report.laeqDb)
            }
            if (emitCompleted) {
                _completedSessionIds.emit(snapshot.sessionId)
            }

            // Paivita kotinayton widget viimeisimmalla sessiodatalla.
            DbCheckWidgetReceiver.updateAllWidgets(context)
        }

        suspend fun recoverInterruptedSession() {
            val activeSession = sessionRepository.getActiveSession().first() ?: return
            val measurements = measurementRepository.getMeasurementsForSession(activeSession.id).first()
            val recoveredEndTime = measurements.maxOfOrNull { it.timestamp } ?: activeSession.startTime
            val weightedMeasurements = measurements.map { it.dbWeighted }
            sessionRepository.completeSession(
                id = activeSession.id,
                endTime = recoveredEndTime,
                minDb = weightedMeasurements.minOrNull() ?: 0f,
                avgDb = DecibelMath.energyAverageDb(weightedMeasurements) ?: 0f,
                maxDb = weightedMeasurements.maxOrNull() ?: 0f,
                peakDb = activeSession.peakDb,
                frequencyWeighting = activeSession.frequencyWeighting,
            )
            DbCheckWidgetReceiver.updateAllWidgets(context)
        }

        fun resetStats() {
            _sessionStats.value = SessionStats()
        }

        private suspend fun flushMeasurements() {
            val toFlush: List<MeasurementEntity>
            synchronized(pendingMeasurements) {
                if (pendingMeasurements.isEmpty()) return
                toFlush = pendingMeasurements.toList()
                pendingMeasurements.clear()
            }
            measurementRepository.insertMeasurements(toFlush)
            lastMeasurementFlushTime = System.currentTimeMillis()
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

        private fun addPendingMeasurement(sessionId: Long, reading: DecibelReading) {
            pendingMeasurements.add(
                MeasurementEntity(
                    sessionId = sessionId,
                    timestamp = reading.timestamp,
                    dbValue = reading.instantDb,
                    dbWeighted = reading.weightedDb,
                ),
            )
        }

        private fun dispatchNoiseAlerts(reading: DecibelReading, stats: SessionStats) {
            noiseAlertEvaluator
                .evaluate(
                    reading = reading,
                    stats = stats,
                    preferences = currentAlertPreferences,
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
                    if (delivered) {
                        noiseAlertEvaluator.markDelivered(decision)
                    }
                }
        }

        private fun updateStats(reading: DecibelReading) {
            _sessionStats.value = _sessionStats.value.withReading(reading)
        }

        private suspend fun buildSessionReport(session: Session): SessionReportData {
            val measurements =
                measurementRepository
                    .getMeasurementsForSession(session.id)
                    .first()
                    .map { measurement ->
                        ReportMeasurement(
                            timestamp = measurement.timestamp,
                            dbWeighted = measurement.dbWeighted,
                        )
                    }
            return SessionReportCalculator.build(session, measurements)
        }

        private fun hasMicrophonePermission(): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
    }

private const val MAX_PENDING_MEASUREMENTS = 10
private const val FLUSH_INTERVAL_MS = 1_000L
