package com.dbcheck.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.dbcheck.app.data.local.db.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

private const val COMPLETED_HISTORY_SESSION_FILTER =
    "isActive = 0 AND endTime IS NOT NULL AND endTime > startTime " +
        "AND EXISTS (SELECT 1 FROM measurements WHERE measurements.sessionId = sessions.id)"
private const val SELECT_COMPLETED_HISTORY_SESSIONS =
    "SELECT * FROM sessions WHERE $COMPLETED_HISTORY_SESSION_FILTER"
private const val SELECT_COMPLETED_HISTORY_SESSIONS_IN_RANGE =
    "$SELECT_COMPLETED_HISTORY_SESSIONS AND startTime >= :startTime AND startTime <= :endTime"
private const val SELECT_COMPLETED_HISTORY_SESSIONS_IN_FREE_WINDOW =
    "$SELECT_COMPLETED_HISTORY_SESSIONS AND startTime >= :sevenDaysAgo"
private const val SEARCH_SESSIONS_SQL =
    """
        $SELECT_COMPLETED_HISTORY_SESSIONS
            AND startTime >= ?
            AND (
                ? IS NULL
                OR name LIKE ? ESCAPE '\'
                OR tags LIKE ? ESCAPE '\'
            )
            AND (? IS NULL OR startTime >= ?)
            AND (? IS NULL OR startTime <= ?)
            AND (? IS NULL OR avgDb >= ?)
            AND (? IS NULL OR avgDb <= ?)
            AND (? IS NULL OR frequencyWeighting = ?)
            AND (
                ? IS NULL
                OR (
                    ? = 1
                    AND locationLatitude IS NOT NULL
                    AND locationLongitude IS NOT NULL
                    AND locationCapturedAt IS NOT NULL
                )
                OR (
                    ? = 0
                    AND (
                        locationLatitude IS NULL
                        OR locationLongitude IS NULL
                        OR locationCapturedAt IS NULL
                    )
                )
            )
        ORDER BY startTime DESC, id DESC
        """

data class SessionCompletionUpdate(
    val id: Long,
    val endTime: Long,
    val endUtcOffsetSeconds: Int?,
    val minDb: Float,
    val avgDb: Float,
    val maxDb: Float,
    val peakDb: Float,
    val frequencyWeighting: String,
    val isActive: Boolean = false,
    val activeSlot: Int? = null,
)

data class SessionSearchQuery(
    val historyStartTime: Long,
    val nameOrTagPattern: String? = null,
    val timeRange: SessionSearchTimeRange = SessionSearchTimeRange(),
    val averageDbRange: SessionSearchAverageDbRange = SessionSearchAverageDbRange(),
    val frequencyWeighting: String? = null,
    val hasLocation: Int? = null,
)

data class SessionSearchTimeRange(val startTimeFrom: Long? = null, val startTimeTo: Long? = null)

data class SessionSearchAverageDbRange(val minAvgDb: Float? = null, val maxAvgDb: Float? = null)

@Suppress("TooManyFunctions")
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
    suspend fun updateSessionMetadata(id: Long, name: String?, emoji: String?, tags: String?)

    @Query(
        """
        UPDATE sessions
        SET locationLatitude = :latitude,
            locationLongitude = :longitude,
            locationAccuracyMeters = :accuracyMeters,
            locationCapturedAt = :capturedAt
        WHERE id = :id
        """,
    )
    suspend fun updateSessionLocation(
        id: Long,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float?,
        capturedAt: Long,
    )

    @Update(entity = SessionEntity::class)
    suspend fun completeSession(update: SessionCompletionUpdate)

    @Query(
        """
        UPDATE sessions
        SET minDb = :minDb,
            avgDb = :avgDb,
            maxDb = :maxDb,
            peakDb = :peakDb,
            frequencyWeighting = :frequencyWeighting
        WHERE id = :id AND isActive = 1
        """,
    )
    suspend fun updateSessionRuntimeSummary(
        id: Long,
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

    @Query("$SELECT_COMPLETED_HISTORY_SESSIONS ORDER BY startTime DESC, id DESC LIMIT :limit")
    fun getRecentSessions(limit: Int = 20): Flow<List<SessionEntity>>

    @Query(
        "$SELECT_COMPLETED_HISTORY_SESSIONS_IN_RANGE ORDER BY startTime DESC, id DESC",
    )
    fun getSessionsInRange(startTime: Long, endTime: Long): Flow<List<SessionEntity>>

    @Query("$SELECT_COMPLETED_HISTORY_SESSIONS_IN_FREE_WINDOW ORDER BY startTime DESC, id DESC")
    fun getSessionsLast7Days(sevenDaysAgo: Long): Flow<List<SessionEntity>>

    fun searchSessions(query: SessionSearchQuery): Flow<List<SessionEntity>> =
        searchSessionsRaw(query.toSupportSQLiteQuery())

    @RawQuery(observedEntities = [SessionEntity::class])
    fun searchSessionsRaw(query: SupportSQLiteQuery): Flow<List<SessionEntity>>

    @Query("$SELECT_COMPLETED_HISTORY_SESSIONS ORDER BY startTime DESC, id DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("$SELECT_COMPLETED_HISTORY_SESSIONS AND id IN (:sessionIds) ORDER BY startTime DESC, id DESC")
    fun getSessionsForCsvExportByIds(sessionIds: List<Long>): Flow<List<SessionEntity>>

    @Query("SELECT id FROM sessions WHERE isActive = 0 ORDER BY startTime DESC, id DESC")
    suspend fun getInactiveSessionIds(): List<Long>

    @Query("DELETE FROM sessions WHERE isActive = 0")
    suspend fun deleteInactiveSessions(): Int

    @Query("DELETE FROM sessions WHERE startTime < :timestamp AND isActive = 0")
    suspend fun deleteSessionsOlderThan(timestamp: Long)
}

private fun SessionSearchQuery.toSupportSQLiteQuery(): SupportSQLiteQuery = SimpleSQLiteQuery(
        SEARCH_SESSIONS_SQL,
        arrayOf<Any?>(
            historyStartTime,
            nameOrTagPattern,
            nameOrTagPattern,
            nameOrTagPattern,
            timeRange.startTimeFrom,
            timeRange.startTimeFrom,
            timeRange.startTimeTo,
            timeRange.startTimeTo,
            averageDbRange.minAvgDb,
            averageDbRange.minAvgDb,
            averageDbRange.maxAvgDb,
            averageDbRange.maxAvgDb,
            frequencyWeighting,
            frequencyWeighting,
            hasLocation,
            hasLocation,
            hasLocation,
        ),
    )
