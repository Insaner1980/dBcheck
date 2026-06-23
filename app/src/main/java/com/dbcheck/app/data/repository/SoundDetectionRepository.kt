package com.dbcheck.app.data.repository

import com.dbcheck.app.data.local.db.dao.SoundDetectionEventDao
import com.dbcheck.app.data.local.db.entity.SoundDetectionEventEntity
import com.dbcheck.app.domain.audio.SoundDetectionEvent
import com.dbcheck.app.domain.report.ReportSoundEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundDetectionRepository
    @Inject
    constructor(
        private val soundDetectionEventDao: SoundDetectionEventDao,
    ) {
        suspend fun recordEvent(event: SoundDetectionEvent) {
            soundDetectionEventDao.insertEvent(event.toEntity())
        }

        fun getReportSoundEventsForSession(sessionId: Long): Flow<List<ReportSoundEvent>> =
            soundDetectionEventDao.getEventsForSession(sessionId)
                .map { events -> events.map { it.toReportSoundEvent() } }

        suspend fun deleteAllEvents() {
            soundDetectionEventDao.deleteAllEvents()
        }
    }

private fun SoundDetectionEvent.toEntity(): SoundDetectionEventEntity = SoundDetectionEventEntity(
    sessionId = sessionId,
    timestamp = timestamp,
    label = label,
    confidence = confidence,
)

private fun SoundDetectionEventEntity.toReportSoundEvent(): ReportSoundEvent = ReportSoundEvent(
    timestamp = timestamp,
    label = label,
    confidence = confidence,
)
