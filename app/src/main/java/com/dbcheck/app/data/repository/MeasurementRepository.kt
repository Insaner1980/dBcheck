package com.dbcheck.app.data.repository

import com.dbcheck.app.data.local.db.dao.DailyAverage
import com.dbcheck.app.data.local.db.dao.HourlyAverage
import com.dbcheck.app.data.local.db.dao.MeasurementDao
import com.dbcheck.app.data.local.db.entity.MeasurementEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeasurementRepository
    @Inject
    constructor(
        private val measurementDao: MeasurementDao,
    ) {
        suspend fun insertMeasurement(measurement: MeasurementEntity) = measurementDao.insertMeasurement(measurement)

        suspend fun insertMeasurements(measurements: List<MeasurementEntity>) = measurementDao.insertMeasurements(measurements)

        fun getMeasurementsForSession(sessionId: Long): Flow<List<MeasurementEntity>> = measurementDao.getMeasurementsForSession(sessionId)

        fun getLast24HoursMeasurements(): Flow<List<MeasurementEntity>> {
            val since = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
            return measurementDao.getMeasurementsSince(since)
        }

        fun getHourlyAveragesLast24H(): Flow<List<HourlyAverage>> {
            val since = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
            return measurementDao.getHourlyAverages(since)
        }

        fun getDailyAveragesLast7Days(): Flow<List<DailyAverage>> {
            val since = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
            return measurementDao.getDailyAverages(since)
        }
    }
