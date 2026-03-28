package com.dbcheck.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.dbcheck.app.data.local.db.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE isActive = 1 LIMIT 1")
    fun getActiveSession(): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun getSessionById(id: Long): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE isActive = 0 ORDER BY startTime DESC LIMIT :limit")
    fun getRecentSessions(limit: Int = 20): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE startTime >= :startTime AND startTime <= :endTime ORDER BY startTime DESC")
    fun getSessionsInRange(
        startTime: Long,
        endTime: Long,
    ): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE startTime >= :sevenDaysAgo ORDER BY startTime DESC")
    fun getSessionsLast7Days(sevenDaysAgo: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE isActive = 0 ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("DELETE FROM sessions WHERE startTime < :timestamp AND isActive = 0")
    suspend fun deleteSessionsOlderThan(timestamp: Long)
}
