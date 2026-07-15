package com.dbcheck.app.test

import com.dbcheck.app.data.local.db.dao.EnvironmentMixCounts
import com.dbcheck.app.data.local.db.dao.MeasurementDao
import com.dbcheck.app.data.local.db.dao.WeightedMeasurementPoint
import com.dbcheck.app.data.local.db.entity.MeasurementEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal open class EmptyMeasurementDao : MeasurementDao {
    override suspend fun insertMeasurements(measurements: List<MeasurementEntity>) = Unit

    override fun getMeasurementsForSession(sessionId: Long): Flow<List<MeasurementEntity>> = flowOf(emptyList())

    override suspend fun getMeasurementsForSessionExportPage(
        sessionId: Long,
        afterTimestamp: Long,
        afterId: Long,
        limit: Int,
    ): List<MeasurementEntity> = emptyList()

    override fun getMeasurementsInRange(startTime: Long, endTime: Long): Flow<List<MeasurementEntity>> =
        flowOf(emptyList())

    override fun getWeightedMeasurementsInRange(startTime: Long, endTime: Long): Flow<List<WeightedMeasurementPoint>> =
        flowOf(emptyList())

    override fun getEnvironmentMixCountsInRange(
        startTime: Long,
        endTime: Long,
        quietMaxDb: Float,
        moderateMaxDb: Float,
        loudMaxDb: Float,
    ): Flow<EnvironmentMixCounts> = flowOf(EnvironmentMixCounts())
}
