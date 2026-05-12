package com.dbcheck.app.data.repository

import com.dbcheck.app.data.local.db.dao.SessionDao
import com.dbcheck.app.data.local.db.entity.SessionEntity
import com.dbcheck.app.data.local.preferences.UserPreferencesDataStore
import com.dbcheck.app.data.model.toDomainModel
import com.dbcheck.app.domain.session.Session
import com.dbcheck.app.domain.session.SessionHistoryPolicy
import com.dbcheck.app.domain.session.SessionMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository
    @Inject
    constructor(
        private val sessionDao: SessionDao,
        private val preferencesDataStore: UserPreferencesDataStore,
    ) {
        suspend fun createSession(session: SessionEntity): Long = sessionDao.insertSession(session)

        suspend fun updateSessionMetadata(
            id: Long,
            name: String?,
            emoji: String?,
            tags: List<String>,
        ) = sessionDao.updateSessionMetadata(
            id = id,
            name = SessionMetadata.normalizeName(name),
            emoji = SessionMetadata.normalizeEmoji(emoji),
            tags = SessionMetadata.serializeTags(tags),
        )

        suspend fun completeSession(
            id: Long,
            endTime: Long,
            minDb: Float,
            avgDb: Float,
            maxDb: Float,
            peakDb: Float,
            frequencyWeighting: String,
        ) = sessionDao.completeSession(
            id = id,
            endTime = endTime,
            minDb = minDb,
            avgDb = avgDb,
            maxDb = maxDb,
            peakDb = peakDb,
            frequencyWeighting = frequencyWeighting,
        )

        fun getActiveSession(): Flow<Session?> = sessionDao.getActiveSession().map { it?.toDomainModel() }

        fun getSessionById(id: Long): Flow<Session?> = sessionDao.getSessionById(id).map { it?.toDomainModel() }

        fun getRecentSessions(limit: Int = 20): Flow<List<Session>> =
            sessionDao.getRecentSessions(limit).map { list ->
                list.map { it.toDomainModel() }
            }

        fun getAllCompletedSessions(): Flow<List<Session>> =
            sessionDao.getAllSessions().map { list ->
                list.map { it.toDomainModel() }
            }

        fun getSessions(): Flow<List<Session>> = flow {
            val isPro = preferencesDataStore.userPreferences.first().isProUser
            val sessions =
                if (isPro) {
                    sessionDao.getAllSessions()
                } else {
                    sessionDao.getSessionsLast7Days(SessionHistoryPolicy.freeHistoryStartMillis())
                }
            emitAll(sessions.map { list -> list.map { it.toDomainModel() } })
        }

        fun getSessionsInRange(
            startTime: Long,
            endTime: Long,
        ): Flow<List<Session>> =
            sessionDao.getSessionsInRange(startTime, endTime).map { list ->
                list.map { it.toDomainModel() }
            }

        fun getCompletedSessionCountInRange(
            startTime: Long,
            endTime: Long,
        ): Flow<Int> =
            sessionDao.getSessionsInRange(
                startTime = startTime,
                endTime = endTime,
            ).map { sessions -> sessions.count { !it.isActive } }

        suspend fun cleanupOldSessions() {
            val isPro = preferencesDataStore.userPreferences.first().isProUser
            if (!isPro) {
                sessionDao.deleteSessionsOlderThan(SessionHistoryPolicy.freeHistoryStartMillis())
            }
        }
    }
