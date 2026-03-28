package com.dbcheck.app.data.repository

import com.dbcheck.app.data.local.db.dao.SessionDao
import com.dbcheck.app.data.local.db.entity.SessionEntity
import com.dbcheck.app.data.local.preferences.UserPreferencesDataStore
import com.dbcheck.app.data.model.Session
import com.dbcheck.app.data.model.toDomainModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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

        suspend fun updateSession(session: SessionEntity) = sessionDao.updateSession(session)

        fun getActiveSession(): Flow<Session?> = sessionDao.getActiveSession().map { it?.toDomainModel() }

        fun getRecentSessions(limit: Int = 20): Flow<List<Session>> =
            sessionDao.getRecentSessions(limit).map { list ->
                list.map { it.toDomainModel() }
            }

        suspend fun getSessions(): Flow<List<Session>> {
            val isPro = preferencesDataStore.userPreferences.first().isProUser
            return if (isPro) {
                sessionDao.getAllSessions()
            } else {
                val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
                sessionDao.getSessionsLast7Days(sevenDaysAgo)
            }.map { list -> list.map { it.toDomainModel() } }
        }

        fun getSessionsInRange(
            startTime: Long,
            endTime: Long,
        ): Flow<List<Session>> =
            sessionDao.getSessionsInRange(startTime, endTime).map { list ->
                list.map { it.toDomainModel() }
            }

        suspend fun cleanupOldSessions() {
            val isPro = preferencesDataStore.userPreferences.first().isProUser
            if (!isPro) {
                val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
                sessionDao.deleteSessionsOlderThan(sevenDaysAgo)
            }
        }
    }
