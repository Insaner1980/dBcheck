package com.dbcheck.app.service

import android.content.Context
import com.dbcheck.app.data.repository.PassiveMonitoringRepository
import com.dbcheck.app.data.repository.SessionRepository
import com.dbcheck.app.di.IoDispatcher
import com.dbcheck.app.sync.MeasurementDatabaseGate
import com.dbcheck.app.widget.DbCheckWidgetReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ClearHistoryResult(val deletedSessionCount: Int, val incomplete: Boolean = false)

class HistoryClearService
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val sessionRepository: SessionRepository,
        private val passiveMonitoringRepository: PassiveMonitoringRepository,
        private val wavRecordingFileStore: WavRecordingFileStore,
        private val measurementDatabaseGate: MeasurementDatabaseGate,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        suspend fun clearHistory(): ClearHistoryResult {
            val owner = Any()
            check(measurementDatabaseGate.tryAcquire(owner)) { "Database maintenance is already active" }
            return try {
                clearHistoryWithPermit()
            } finally {
                measurementDatabaseGate.release(owner)
            }
        }

        private suspend fun clearHistoryWithPermit(): ClearHistoryResult {
            val sessionIds = sessionRepository.getInactiveSessionIds()
            var incomplete = false
            withContext(ioDispatcher) {
                sessionIds.forEach { sessionId ->
                    val deleted = runCatching {
                        wavRecordingFileStore.recordingFileForSession(sessionId) == null ||
                            wavRecordingFileStore.deleteRecordingForSession(sessionId)
                    }.onFailure { error ->
                        if (error is CancellationException) throw error
                    }.getOrDefault(false)
                    incomplete = incomplete || !deleted
                }
            }
            if (incomplete) return ClearHistoryResult(deletedSessionCount = 0, incomplete = true)
            val deletedSessionIds = sessionRepository.clearInactiveHistory()
            val passiveCleanup = runCatching {
                passiveMonitoringRepository.clearAllSamples()
            }
            val passiveFailure = passiveCleanup.exceptionOrNull()
            if (passiveFailure is Exception && passiveFailure !is CancellationException) {
                incomplete = true
            } else {
                passiveCleanup.getOrThrow()
            }
            if (deletedSessionIds.isNotEmpty()) {
                runCatching {
                    DbCheckWidgetReceiver.updateAllWidgets(context)
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                }
            }
            return ClearHistoryResult(deletedSessionCount = deletedSessionIds.size, incomplete = incomplete)
        }
    }
