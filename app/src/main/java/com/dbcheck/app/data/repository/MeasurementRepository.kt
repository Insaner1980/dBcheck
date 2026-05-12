package com.dbcheck.app.data.repository

import com.dbcheck.app.data.local.db.dao.DailyAverage
import com.dbcheck.app.data.local.db.dao.EnvironmentMixCounts
import com.dbcheck.app.data.local.db.dao.HourlyAverage
import com.dbcheck.app.data.local.db.dao.MeasurementDao
import com.dbcheck.app.data.local.db.dao.WeightedMeasurementPoint
import com.dbcheck.app.data.local.db.entity.MeasurementEntity
import com.dbcheck.app.domain.analytics.DailyExposureAverage
import com.dbcheck.app.domain.analytics.EnvironmentExposureMixCounts
import com.dbcheck.app.domain.analytics.HourlyExposureAverage
import com.dbcheck.app.domain.analytics.WeightedExposureMeasurement
import com.dbcheck.app.domain.noise.NoiseLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeasurementRepository
    @Inject
    constructor(
        private val measurementDao: MeasurementDao,
    ) {
        suspend fun insertMeasurement(measurement: MeasurementEntity) = measurementDao.insertMeasurement(measurement)

        suspend fun insertMeasurements(measurements: List<MeasurementEntity>) =
            measurementDao.insertMeasurements(measurements)

        fun getMeasurementsForSession(sessionId: Long): Flow<List<MeasurementEntity>> =
            measurementDao.getMeasurementsForSession(sessionId)

        fun getLast24HoursMeasurements(): Flow<List<MeasurementEntity>> {
            val since = System.currentTimeMillis() - LAST_24_HOURS_MILLIS
            return measurementDao.getMeasurementsSince(since)
        }

        fun getHourlyAveragesLast24H(): Flow<List<HourlyExposureAverage>> {
            val since = System.currentTimeMillis() - LAST_24_HOURS_MILLIS
            return measurementDao.getHourlyAverages(since).map { averages ->
                averages.map { it.toDomainModel() }
            }
        }

        fun getWeightedMeasurementsInRange(
            startTime: Long,
            endTime: Long,
        ): Flow<List<WeightedExposureMeasurement>> =
            measurementDao.getWeightedMeasurementsInRange(
                startTime = startTime,
                endTime = endTime,
            ).map { measurements ->
                measurements.map { it.toDomainModel() }
            }

        fun getDailyAveragesLast7Days(): Flow<List<DailyExposureAverage>> {
            val since = System.currentTimeMillis() - LAST_7_DAYS_MILLIS
            return measurementDao.getDailyAverages(since).map { averages ->
                averages.map { it.toDomainModel() }
            }
        }

        fun getEnvironmentMixLast7Days(): Flow<EnvironmentExposureMixCounts> {
            val since = System.currentTimeMillis() - LAST_7_DAYS_MILLIS
            return measurementDao.getEnvironmentMixCounts(
                since = since,
                quietMaxDb = NoiseLevel.QUIET.maxDb,
                moderateMaxDb = NoiseLevel.NORMAL.maxDb,
                loudMaxDb = NoiseLevel.ELEVATED.maxDb,
            ).map { it.toDomainModel() }
        }

        private companion object {
            const val DAY_MILLIS = 24 * 60 * 60 * 1000L
            const val LAST_24_HOURS_MILLIS = DAY_MILLIS
            const val LAST_7_DAYS_MILLIS = 7 * DAY_MILLIS
        }
    }

private fun HourlyAverage.toDomainModel(): HourlyExposureAverage =
    HourlyExposureAverage(
        hour = hour,
        avgDb = avgDb,
        maxDb = maxDb,
    )

private fun DailyAverage.toDomainModel(): DailyExposureAverage =
    DailyExposureAverage(
        dayStartMs = day,
        avgDb = avgDb,
        maxDb = maxDb,
    )

private fun WeightedMeasurementPoint.toDomainModel(): WeightedExposureMeasurement =
    WeightedExposureMeasurement(
        timestamp = timestamp,
        dbWeighted = dbWeighted,
    )

private fun EnvironmentMixCounts.toDomainModel(): EnvironmentExposureMixCounts =
    EnvironmentExposureMixCounts(
        quietCount = quietCount,
        moderateCount = moderateCount,
        loudCount = loudCount,
        criticalCount = criticalCount,
        totalCount = totalCount,
    )
