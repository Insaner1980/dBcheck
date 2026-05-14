package com.dbcheck.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dbcheck.app.data.local.db.entity.MeasurementEntity
import kotlinx.coroutines.flow.Flow

data class WeightedMeasurementPoint(val timestamp: Long, val dbWeighted: Float)

data class EnvironmentMixCounts(
    val quietCount: Long = 0,
    val moderateCount: Long = 0,
    val loudCount: Long = 0,
    val criticalCount: Long = 0,
    val totalCount: Long = 0,
)

@Dao
interface MeasurementDao {
    @Insert
    suspend fun insertMeasurement(measurement: MeasurementEntity)

    @Insert
    suspend fun insertMeasurements(measurements: List<MeasurementEntity>)

    @Query("SELECT * FROM measurements WHERE sessionId = :sessionId ORDER BY timestamp ASC, id ASC")
    fun getMeasurementsForSession(sessionId: Long): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE sessionId IN (:sessionIds) ORDER BY sessionId ASC, timestamp ASC, id ASC")
    suspend fun getMeasurementsForSessions(sessionIds: List<Long>): List<MeasurementEntity>

    @Query("SELECT * FROM measurements WHERE timestamp >= :since ORDER BY timestamp ASC, id ASC")
    fun getMeasurementsSince(since: Long): Flow<List<MeasurementEntity>>

    @Query(
        """
        SELECT timestamp, dbWeighted
        FROM measurements
        WHERE timestamp >= :startTime AND timestamp <= :endTime
        ORDER BY timestamp ASC, id ASC
        """,
    )
    fun getWeightedMeasurementsInRange(startTime: Long, endTime: Long): Flow<List<WeightedMeasurementPoint>>

    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN dbWeighted < :quietMaxDb THEN 1 ELSE 0 END), 0) as quietCount,
            COALESCE(
                SUM(CASE WHEN dbWeighted >= :quietMaxDb AND dbWeighted < :moderateMaxDb THEN 1 ELSE 0 END),
                0
            ) as moderateCount,
            COALESCE(
                SUM(CASE WHEN dbWeighted >= :moderateMaxDb AND dbWeighted < :loudMaxDb THEN 1 ELSE 0 END),
                0
            ) as loudCount,
            COALESCE(SUM(CASE WHEN dbWeighted >= :loudMaxDb THEN 1 ELSE 0 END), 0) as criticalCount,
            COUNT(*) as totalCount
        FROM measurements
        WHERE timestamp >= :since
        """,
    )
    fun getEnvironmentMixCounts(
        since: Long,
        quietMaxDb: Float,
        moderateMaxDb: Float,
        loudMaxDb: Float,
    ): Flow<EnvironmentMixCounts>
}
