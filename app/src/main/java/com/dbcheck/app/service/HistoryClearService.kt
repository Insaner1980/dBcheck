package com.dbcheck.app.service

import com.dbcheck.app.data.repository.PassiveMonitoringRepository
import com.dbcheck.app.data.repository.SessionRepository
import com.dbcheck.app.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ClearHistoryResult(val deletedSessionCount: Int)

class HistoryClearService
    @Inject
    constructor(
        private val sessionRepository: SessionRepository,
        private val passiveMonitoringRepository: PassiveMonitoringRepository,
        private val wavRecordingFileStore: WavRecordingFileStore,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        suspend fun clearHistory(): ClearHistoryResult {
            val deletedSessionIds = sessionRepository.clearInactiveHistory()
            passiveMonitoringRepository.clearAllSamples()
            withContext(ioDispatcher) {
                deletedSessionIds.forEach { sessionId ->
                    wavRecordingFileStore.deleteRecordingForSession(sessionId)
                }
            }
            return ClearHistoryResult(deletedSessionCount = deletedSessionIds.size)
        }
    }
