package com.dbcheck.app.service

import com.dbcheck.app.data.repository.SessionRepository
import javax.inject.Inject

data class ClearHistoryResult(val deletedSessionCount: Int)

class HistoryClearService
    @Inject
    constructor(
        private val sessionRepository: SessionRepository,
        private val wavRecordingFileStore: WavRecordingFileStore,
    ) {
        suspend fun clearHistory(): ClearHistoryResult {
            val deletedSessionIds = sessionRepository.clearInactiveHistory()
            deletedSessionIds.forEach { sessionId ->
                wavRecordingFileStore.deleteRecordingForSession(sessionId)
            }
            return ClearHistoryResult(deletedSessionCount = deletedSessionIds.size)
        }
    }
