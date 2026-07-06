package com.dbcheck.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dbcheck.app.data.local.db.entity.PassiveMonitoringSampleEntity
import kotlinx.coroutines.flow.Flow

data class PassiveMonitoringDailySummaryRow(
    val sampleCount: Int = 0,
    val readingCount: Int = 0,
    val minDb: Float? = null,
    val maxDb: Float? = null,
    val peakDb: Float? = null,
    val totalEnergy: Double? = null,
)

@Dao
interface PassiveMonitoringDao {
    @Insert
    suspend fun insertSample(sample: PassiveMonitoringSampleEntity): Long

    @Query(
        """
        SELECT
            COUNT(*) AS sampleCount,
            COALESCE(SUM(readingCount), 0) AS readingCount,
            MIN(minDb) AS minDb,
            MAX(maxDb) AS maxDb,
            MAX(peakDb) AS peakDb,
            SUM(totalEnergy) AS totalEnergy
        FROM passive_monitoring_samples
        WHERE startedAtMs >= :startTimeMs AND startedAtMs < :endTimeMs
        """,
    )
    fun observeDailySummary(startTimeMs: Long, endTimeMs: Long): Flow<PassiveMonitoringDailySummaryRow>

    @Query("DELETE FROM passive_monitoring_samples")
    suspend fun deleteAllSamples()
}
