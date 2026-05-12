package com.dbcheck.app.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.dbcheck.app.data.local.db.entity.MeasurementEntity
import com.dbcheck.app.data.local.db.entity.SessionEntity
import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.data.local.preferences.model.ProAudioPreferencePolicy
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.model.toDomainModel
import com.dbcheck.app.data.repository.MeasurementRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.data.repository.SessionRepository
import com.dbcheck.app.di.DefaultDispatcher
import com.dbcheck.app.domain.audio.AudioEngine
import com.dbcheck.app.domain.audio.DecibelReading
import com.dbcheck.app.domain.audio.WeightingType
import com.dbcheck.app.domain.report.ReportMeasurement
import com.dbcheck.app.domain.report.SessionReportCalculator
import com.dbcheck.app.domain.report.SessionReportData
import com.dbcheck.app.domain.session.Session
import com.dbcheck.app.sync.HealthConnectManager
import com.dbcheck.app.widget.DbCheckWidgetReceiver
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
    val totalDb: Double = 0.0,
)

private data class RuntimeAudioPreferences(
    val frequencyWeighting: String,
    val micSensitivityOffset: Float,
    val isProUser: Boolean,
    val refreshRate: MeterRefreshRate,
    val exposureAlertsEnabled: Boolean,
    val peakWarningsEnabled: Boolean,
    val notificationThreshold: Int,
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
        private var currentSessionId: Long? = null
        private var sessionStartTime: Long = 0L
        private var currentFrequencyWeighting: String = WeightingType.DEFAULT.name
        private var currentRefreshRate: MeterRefreshRate = MeterRefreshRate.STANDARD
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

        fun startSession(): Boolean = if (_isRecording.value) {
                true
            } else if (!hasMicrophonePermission()) {
                false
            } else {
                prepareSessionRuntime()
                startPreferenceCollection()
                startRecordingCollection()
                true
            }

        private fun prepareSessionRuntime() {
            sessionStartTime = System.currentTimeMillis()
            lastMeasurementFlushTime = sessionStartTime
            noiseAlertEvaluator.reset(sessionStartTime)
            _sessionStats.value = SessionStats()
            _isRecording.value = true
        }

        private fun startPreferenceCollection() {
            preferencesJob =
                scope.launch {
                    preferencesRepository.userPreferences
                        .map { prefs ->
                            RuntimeAudioPreferences(
                                frequencyWeighting = ProAudioPreferencePolicy.weighting(prefs),
                                micSensitivityOffset = ProAudioPreferencePolicy.micOffset(prefs),
                                isProUser = prefs.isProUser,
                                refreshRate = prefs.refreshRate,
                                exposureAlertsEnabled = prefs.exposureAlertsEnabled,
                                peakWarningsEnabled = prefs.peakWarningsEnabled,
                                notificationThreshold = prefs.notificationThreshold,
                            )
                        }
                        .distinctUntilChanged()
                        .collect(::applyRuntimeAudioPreferences)
                }
        }

        private fun applyRuntimeAudioPreferences(prefs: RuntimeAudioPreferences) {
            val weightingType = WeightingType.fromPreference(prefs.frequencyWeighting)
            currentFrequencyWeighting = weightingType.name
            audioEngine.setWeighting(weightingType)
            audioEngine.setCalibrationOffset(prefs.micSensitivityOffset)
            audioEngine.setSpectralAnalysisEnabled(prefs.isProUser)
            currentRefreshRate = prefs.refreshRate
            currentAlertPreferences =
                UserPreferences(
                    exposureAlertsEnabled = prefs.exposureAlertsEnabled,
                    peakWarningsEnabled = prefs.peakWarningsEnabled,
                    notificationThreshold = prefs.notificationThreshold,
                )
        }

        private fun startRecordingCollection() {
            scope.launch {
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

                recordingJob =
                    scope.launch {
                        audioEngine.startRecording()
                    }

                measurementCollectionJob =
                    scope.launch {
                        audioEngine.decibelFlow.collect(::handleDecibelReading)
                    }
            }
        }

        private suspend fun handleDecibelReading(reading: DecibelReading) {
            val statsBeforeReading = _sessionStats.value
            val shouldPersist =
                currentSessionId != null &&
                    measurementSampler.shouldPersist(
                        reading = reading,
                        refreshRate = currentRefreshRate,
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

        fun stopSession() {
            _isRecording.value = false
            audioEngine.stopRecording()
            recordingJob?.cancel()
            preferencesJob?.cancel()
            measurementCollectionJob?.cancel()
            preferencesJob = null
            measurementCollectionJob = null

            scope.launch {
                currentSessionId?.let { sessionId ->
                    measurementSampler.latestUnpersistedOnStop()?.let { reading ->
                        addPendingMeasurement(sessionId, reading)
                        measurementSampler.markPersisted(reading)
                    }
                }
                flushMeasurements()

                currentSessionId?.let { sessionId ->
                    val stats = _sessionStats.value
                    val endTime = System.currentTimeMillis()
                    val completedSession =
                        SessionEntity(
                            id = sessionId,
                            startTime = sessionStartTime,
                            endTime = endTime,
                            minDb = stats.minDb.takeIf { it != Float.MAX_VALUE } ?: 0f,
                            avgDb = stats.avgDb,
                            maxDb = stats.maxDb,
                            peakDb = stats.peakDb,
                            isActive = false,
                            frequencyWeighting = currentFrequencyWeighting,
                        )
                    sessionRepository.completeSession(
                        id = sessionId,
                        endTime = endTime,
                        minDb = completedSession.minDb,
                        avgDb = completedSession.avgDb,
                        maxDb = completedSession.maxDb,
                        peakDb = completedSession.peakDb,
                        frequencyWeighting = currentFrequencyWeighting,
                    )
                    val completedDomainSession = completedSession.toDomainModel()
                    if (preferencesRepository.userPreferences.first().healthConnectEnabled) {
                        val report = buildSessionReport(completedDomainSession)
                        healthConnectManager.writeNoiseDose(completedDomainSession, report.laeqDb)
                    }
                    _completedSessionIds.emit(sessionId)
                }
                currentSessionId = null
                measurementSampler.reset()
                noiseAlertEvaluator.reset(0L)

                // Paivita kotinayton widget viimeisimmalla sessiodatalla.
                DbCheckWidgetReceiver.updateAllWidgets(context)
            }
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

        private fun addPendingMeasurement(
            sessionId: Long,
            reading: DecibelReading,
        ) {
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
                    when (decision) {
                        is NoiseAlertDecision.Exposure ->
                            notificationHelper.sendExposureAlert(
                                avgDb = decision.avgDb,
                                durationMinutes = decision.durationMinutes,
                            )

                        is NoiseAlertDecision.Peak ->
                            notificationHelper.sendPeakWarning(decision.peakDb)
                    }
                }
        }

        private fun updateStats(reading: DecibelReading) {
            val current = _sessionStats.value
            val newCount = current.sampleCount + 1
            val newTotal = current.totalDb + reading.weightedDb
            _sessionStats.value =
                current.copy(
                    minDb = minOf(current.minDb, reading.weightedDb),
                    maxDb = maxOf(current.maxDb, reading.weightedDb),
                    peakDb = maxOf(current.peakDb, reading.instantDb),
                    avgDb = (newTotal / newCount).toFloat(),
                    sampleCount = newCount,
                    totalDb = newTotal,
                )
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
