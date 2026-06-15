package com.dbcheck.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
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

    @Query(
        """
        $SELECT_COMPLETED_HISTORY_SESSIONS
            AND startTime >= :historyStartTime
            AND (
                :nameOrTagPattern IS NULL
                OR name LIKE :nameOrTagPattern ESCAPE '\'
                OR tags LIKE :nameOrTagPattern ESCAPE '\'
            )
            AND (:startTimeFrom IS NULL OR startTime >= :startTimeFrom)
            AND (:startTimeTo IS NULL OR startTime <= :startTimeTo)
            AND (:minAvgDb IS NULL OR avgDb >= :minAvgDb)
            AND (:maxAvgDb IS NULL OR avgDb <= :maxAvgDb)
            AND (:frequencyWeighting IS NULL OR frequencyWeighting = :frequencyWeighting)
            AND (
                :hasLocation IS NULL
                OR (
                    :hasLocation = 1
                    AND locationLatitude IS NOT NULL
                    AND locationLongitude IS NOT NULL
                    AND locationCapturedAt IS NOT NULL
                )
                OR (
                    :hasLocation = 0
                    AND (
                        locationLatitude IS NULL
                        OR locationLongitude IS NULL
                        OR locationCapturedAt IS NULL
                    )
                )
            )
        ORDER BY startTime DESC, id DESC
        """,
    )
    fun searchSessions(
        historyStartTime: Long,
        nameOrTagPattern: String?,
        startTimeFrom: Long?,
        startTimeTo: Long?,
        minAvgDb: Float?,
        maxAvgDb: Float?,
        frequencyWeighting: String?,
        hasLocation: Int?,
    ): Flow<List<SessionEntity>>

    @Query("$SELECT_COMPLETED_HISTORY_SESSIONS ORDER BY startTime DESC, id DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("DELETE FROM sessions WHERE startTime < :timestamp AND isActive = 0")
    suspend fun deleteSessionsOlderThan(timestamp: Long)
}
