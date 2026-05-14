package com.dbcheck.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dbcheck.app.data.local.db.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Query(
        """
        UPDATE sessions
        SET name = :name,
            emoji = :emoji,
            tags = :tags
        WHERE id = :id
        """,
    )
    suspend fun updateSessionMetadata(
        id: Long,
        name: String?,
        emoji: String?,
        tags: String?,
    )

    @Query(
        """
        UPDATE sessions
        SET endTime = :endTime,
            minDb = :minDb,
            avgDb = :avgDb,
            maxDb = :maxDb,
            peakDb = :peakDb,
            isActive = 0,
            activeSlot = NULL,
            frequencyWeighting = :frequencyWeighting
        WHERE id = :id
        """,
    )
    suspend fun completeSession(
        id: Long,
        endTime: Long,
        minDb: Float,
        avgDb: Float,
        maxDb: Float,
        peakDb: Float,
        frequencyWeighting: String,
    )

    @Query("SELECT * FROM sessions WHERE activeSlot = 1 ORDER BY startTime DESC, id DESC LIMIT 1")
    fun getActiveSession(): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun getSessionById(id: Long): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE isActive = 0 ORDER BY startTime DESC, id DESC LIMIT :limit")
    fun getRecentSessions(limit: Int = 20): Flow<List<SessionEntity>>

    @Query(
        """
        SELECT * FROM sessions
        WHERE startTime >= :startTime AND startTime <= :endTime
        ORDER BY startTime DESC, id DESC
        """,
    )
    fun getSessionsInRange(startTime: Long, endTime: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE startTime >= :sevenDaysAgo ORDER BY startTime DESC, id DESC")
    fun getSessionsLast7Days(sevenDaysAgo: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE isActive = 0 ORDER BY startTime DESC, id DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("DELETE FROM sessions WHERE startTime < :timestamp AND isActive = 0")
    suspend fun deleteSessionsOlderThan(timestamp: Long)
}
