package com.dbcheck.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dbcheck.app.data.local.db.entity.SoundDetectionEventEntity

@Dao
interface SoundDetectionEventDao {
    @Insert
    suspend fun insertEvent(event: SoundDetectionEventEntity)

    @Query(
        """
        SELECT * FROM sound_detection_events
        WHERE sessionId = :sessionId
            AND (timestamp > :afterTimestamp OR (timestamp = :afterTimestamp AND id > :afterId))
        ORDER BY timestamp ASC, id ASC
        LIMIT :limit
        """,
    )
    suspend fun getEventsForSessionExportPage(
        sessionId: Long,
        afterTimestamp: Long,
        afterId: Long,
        limit: Int,
    ): List<SoundDetectionEventEntity>

    @Query("DELETE FROM sound_detection_events")
    suspend fun deleteAllEvents()
}
