package com.dbcheck.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dbcheck.app.data.local.db.entity.SleepNotableEventEntity
import com.dbcheck.app.data.local.db.entity.SleepSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSleepSession(session: SleepSessionEntity)

    @Insert
    suspend fun insertNotableEvent(event: SleepNotableEventEntity): Long

    @Query("SELECT * FROM sleep_sessions WHERE sessionId = :sessionId")
    fun getSleepSession(sessionId: Long): Flow<SleepSessionEntity?>

    @Query("SELECT sessionId FROM sleep_sessions")
    fun getSleepSessionIds(): Flow<List<Long>>

    @Query("SELECT * FROM sleep_sessions WHERE sessionId IN (:sessionIds)")
    suspend fun getSleepSessionsForCsvExportByIds(sessionIds: List<Long>): List<SleepSessionEntity>

    @Query(
        """
        SELECT * FROM sleep_notable_events
        WHERE sessionId = :sessionId
        ORDER BY timestamp ASC, id ASC
        """,
    )
    fun getNotableEventsForSession(sessionId: Long): Flow<List<SleepNotableEventEntity>>
}
