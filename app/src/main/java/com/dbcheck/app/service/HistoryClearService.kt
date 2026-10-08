package com.dbcheck.app.service

import android.content.Context
import com.dbcheck.app.data.repository.PassiveMonitoringRepository
import com.dbcheck.app.data.repository.SessionRepository
import com.dbcheck.app.di.IoDispatcher
import com.dbcheck.app.widget.DbCheckWidgetReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ClearHistoryResult(val deletedSessionCount: Int)

class HistoryClearService
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val sessionRepository: SessionRepository,
        private val passiveMonitoringRepository: PassiveMonitoringRepository,
        private val wavRecordingFileStore: WavRecordingFileStore,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        suspend fun clearHistory(): ClearHistoryResult {
            val deletedSessionIds = sessionRepository.clearInactiveHistory()
            if (deletedSessionIds.isNotEmpty()) {
                runCatching {
                    DbCheckWidgetReceiver.updateAllWidgets(context)
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                }
            }
            passiveMonitoringRepository.clearAllSamples()
            withContext(ioDispatcher) {
                deletedSessionIds.forEach { sessionId ->
                    wavRecordingFileStore.deleteRecordingForSession(sessionId)
                }
            }
            return ClearHistoryResult(deletedSessionCount = deletedSessionIds.size)
        }
    }
