package com.dbcheck.app.data.repository

import com.dbcheck.app.data.local.db.dao.SleepSessionDao
import com.dbcheck.app.data.local.db.entity.SleepSessionEntity
import com.dbcheck.app.domain.sleep.SleepRecordingConfig
import com.dbcheck.app.domain.sleep.SleepSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepSessionRepository
    @Inject
    constructor(private val sleepSessionDao: SleepSessionDao) {
        suspend fun createSleepSession(sessionId: Long, config: SleepRecordingConfig, createdAt: Long) {
            sleepSessionDao.upsertSleepSession(
                SleepSessionEntity(
                    sessionId = sessionId,
                    targetDurationMinutes = config.targetDurationMinutes,
                    keepAwakeEnabled = config.keepAwakeEnabled,
                    createdAt = createdAt,
                ),
            )
        }

        fun getSleepSession(sessionId: Long): Flow<SleepSession?> =
            sleepSessionDao.getSleepSession(sessionId).map { it?.toDomain() }

        fun getSleepSessionIds(): Flow<Set<Long>> = sleepSessionDao.getSleepSessionIds().map { it.toSet() }
    }

private fun SleepSessionEntity.toDomain(): SleepSession = SleepSession(
        sessionId = sessionId,
        targetDurationMinutes = targetDurationMinutes,
        keepAwakeEnabled = keepAwakeEnabled,
        createdAt = createdAt,
    )
