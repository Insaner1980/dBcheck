package com.dbcheck.app.domain.audio

import android.content.Context
import com.dbcheck.app.data.local.db.entity.MeasurementEntity
import com.dbcheck.app.data.local.db.entity.SessionEntity
import com.dbcheck.app.data.repository.MeasurementRepository
import com.dbcheck.app.data.repository.SessionRepository
import com.dbcheck.app.widget.DbCheckWidgetReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

@Singleton
class AudioSessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioEngine: AudioEngine,
    private val sessionRepository: SessionRepository,
    private val measurementRepository: MeasurementRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var recordingJob: Job? = null
    private var currentSessionId: Long? = null
    private var sessionStartTime: Long = 0L
    private val pendingMeasurements = java.util.Collections.synchronizedList(mutableListOf<MeasurementEntity>())

    private val _sessionStats = MutableStateFlow(SessionStats())
    val sessionStats: StateFlow<SessionStats> = _sessionStats

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    fun startSession() {
        if (_isRecording.value) return

        _sessionStats.value = SessionStats()
        _isRecording.value = true

        scope.launch {
            sessionStartTime = System.currentTimeMillis()
            val session = SessionEntity(
                startTime = sessionStartTime,
                isActive = true,
            )
            currentSessionId = sessionRepository.createSession(session)

            recordingJob = scope.launch {
                audioEngine.startRecording()
            }

            // Collect readings and batch-write to DB
            scope.launch {
                audioEngine.decibelFlow.collect { reading ->
                    updateStats(reading)

                    currentSessionId?.let { sessionId ->
                        pendingMeasurements.add(
                            MeasurementEntity(
                                sessionId = sessionId,
                                timestamp = reading.timestamp,
                                dbValue = reading.instantDb,
                                dbWeighted = reading.weightedDb,
                            ),
                        )

                        // Batch write every ~1 second (roughly 10 readings at 100ms intervals)
                        if (pendingMeasurements.size >= 10) {
                            flushMeasurements()
                        }
                    }
                }
            }
        }
    }

    fun stopSession() {
        _isRecording.value = false
        audioEngine.stopRecording()
        recordingJob?.cancel()

        scope.launch {
            flushMeasurements()

            currentSessionId?.let { sessionId ->
                val stats = _sessionStats.value
                sessionRepository.updateSession(
                    SessionEntity(
                        id = sessionId,
                        startTime = sessionStartTime,
                        endTime = System.currentTimeMillis(),
                        minDb = stats.minDb.takeIf { it != Float.MAX_VALUE } ?: 0f,
                        avgDb = stats.avgDb,
                        maxDb = stats.maxDb,
                        peakDb = stats.peakDb,
                        isActive = false,
                    ),
                )
            }
            currentSessionId = null

            // Refresh home screen widget with latest session data
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
    }

    private fun updateStats(reading: DecibelReading) {
        val current = _sessionStats.value
        val newCount = current.sampleCount + 1
        val newTotal = current.totalDb + reading.weightedDb
        _sessionStats.value = current.copy(
            minDb = minOf(current.minDb, reading.weightedDb),
            maxDb = maxOf(current.maxDb, reading.weightedDb),
            peakDb = maxOf(current.peakDb, reading.instantDb),
            avgDb = (newTotal / newCount).toFloat(),
            sampleCount = newCount,
            totalDb = newTotal,
        )
    }
}
