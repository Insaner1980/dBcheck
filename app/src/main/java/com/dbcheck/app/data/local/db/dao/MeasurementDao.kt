package com.dbcheck.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dbcheck.app.data.local.db.entity.MeasurementEntity
import kotlinx.coroutines.flow.Flow

data class HourlyAverage(
    val hour: Int,
    val avgDb: Float,
    val maxDb: Float,
)

data class DailyAverage(
    val day: Long,
    val avgDb: Float,
    val maxDb: Float,
)

@Dao
interface MeasurementDao {
    @Insert
    suspend fun insertMeasurement(measurement: MeasurementEntity)

    @Insert
    suspend fun insertMeasurements(measurements: List<MeasurementEntity>)

    @Query("SELECT * FROM measurements WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMeasurementsForSession(sessionId: Long): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE timestamp >= :since ORDER BY timestamp ASC")
    fun getMeasurementsSince(since: Long): Flow<List<MeasurementEntity>>

    @Query(
        """
        SELECT
            CAST((timestamp / 3600000) % 24 AS INTEGER) as hour,
            AVG(dbWeighted) as avgDb,
            MAX(dbWeighted) as maxDb
        FROM measurements
        WHERE timestamp >= :since
        GROUP BY hour
        ORDER BY hour ASC
        """,
    )
    fun getHourlyAverages(since: Long): Flow<List<HourlyAverage>>

    @Query(
        """
        SELECT
            (timestamp / 86400000) * 86400000 as day,
            AVG(dbWeighted) as avgDb,
            MAX(dbWeighted) as maxDb
        FROM measurements
        WHERE timestamp >= :since
        GROUP BY day
        ORDER BY day ASC
        """,
    )
    fun getDailyAverages(since: Long): Flow<List<DailyAverage>>
}
