package com.dbcheck.app.data.repository

import com.dbcheck.app.data.local.db.dao.EnvironmentMixCounts
import com.dbcheck.app.data.local.db.dao.MeasurementDao
import com.dbcheck.app.data.local.db.dao.WeightedMeasurementPoint
import com.dbcheck.app.data.local.db.entity.MeasurementEntity
import com.dbcheck.app.domain.analytics.DailyExposureAverage
import com.dbcheck.app.domain.analytics.EnvironmentExposureMixCounts
import com.dbcheck.app.domain.analytics.HourlyExposureAverage
import com.dbcheck.app.domain.analytics.WeightedExposureMeasurement
import com.dbcheck.app.domain.noise.DecibelMath
import com.dbcheck.app.domain.noise.NoiseLevel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeasurementRepository
    @Inject
    constructor(private val measurementDao: MeasurementDao) {
        suspend fun insertMeasurement(measurement: MeasurementEntity) = measurementDao.insertMeasurement(measurement)

        suspend fun insertMeasurements(measurements: List<MeasurementEntity>) =
            measurementDao.insertMeasurements(measurements)

        fun getMeasurementsForSession(sessionId: Long): Flow<List<MeasurementEntity>> =
            measurementDao.getMeasurementsForSession(sessionId)

        fun getLast24HoursMeasurements(): Flow<List<MeasurementEntity>> =
            getMeasurementsForRollingWindow(LAST_24_HOURS_MILLIS)

        fun getHourlyAveragesLast24H(): Flow<List<HourlyExposureAverage>> =
            getMeasurementsForRollingWindow(LAST_24_HOURS_MILLIS).map { measurements ->
                MeasurementBucketAverages.hourly(measurements.map { it.toWeightedMeasurementPoint() })
            }

        fun getWeightedMeasurementsInRange(startTime: Long, endTime: Long): Flow<List<WeightedExposureMeasurement>> =
            measurementDao.getWeightedMeasurementsInRange(
                startTime = startTime,
                endTime = endTime,
            ).map { measurements ->
                measurements.map { it.toDomainModel() }
            }

        fun getDailyAveragesLast7Days(): Flow<List<DailyExposureAverage>> =
            getMeasurementsForRollingWindow(LAST_7_DAYS_MILLIS).map { measurements ->
                MeasurementBucketAverages.daily(measurements.map { it.toWeightedMeasurementPoint() })
            }

        @OptIn(ExperimentalCoroutinesApi::class)
        fun getEnvironmentMixLast7Days(): Flow<EnvironmentExposureMixCounts> =
            rollingWindowStartTimes(LAST_7_DAYS_MILLIS)
                .flatMapLatest { since ->
                    measurementDao.getEnvironmentMixCounts(
                        since = since,
                        quietMaxDb = NoiseLevel.QUIET.maxDb,
                        moderateMaxDb = NoiseLevel.NORMAL.maxDb,
                        loudMaxDb = NoiseLevel.ELEVATED.maxDb,
                    )
                }.map { it.toDomainModel() }

        @OptIn(ExperimentalCoroutinesApi::class)
        private fun getMeasurementsForRollingWindow(windowMillis: Long): Flow<List<MeasurementEntity>> =
            rollingWindowStartTimes(windowMillis)
                .flatMapLatest { since -> measurementDao.getMeasurementsSince(since) }

        private companion object {
            const val DAY_MILLIS = 24 * 60 * 60 * 1000L
            const val LAST_24_HOURS_MILLIS = DAY_MILLIS
            const val LAST_7_DAYS_MILLIS = 7 * DAY_MILLIS
            const val ROLLING_WINDOW_REFRESH_MILLIS = 60_000L
        }

        private fun rollingWindowStartTimes(windowMillis: Long): Flow<Long> = flow {
            while (currentCoroutineContext().isActive) {
                emit(System.currentTimeMillis() - windowMillis)
                delay(ROLLING_WINDOW_REFRESH_MILLIS)
            }
        }
    }

private fun WeightedMeasurementPoint.toDomainModel(): WeightedExposureMeasurement = WeightedExposureMeasurement(
        timestamp = timestamp,
        dbWeighted = dbWeighted,
    )

private fun MeasurementEntity.toWeightedMeasurementPoint(): WeightedMeasurementPoint = WeightedMeasurementPoint(
        timestamp = timestamp,
        dbWeighted = dbWeighted,
    )

private fun EnvironmentMixCounts.toDomainModel(): EnvironmentExposureMixCounts = EnvironmentExposureMixCounts(
        quietCount = quietCount,
        moderateCount = moderateCount,
        loudCount = loudCount,
        criticalCount = criticalCount,
        totalCount = totalCount,
    )

internal object MeasurementBucketAverages {
    fun hourly(measurements: List<WeightedMeasurementPoint>): List<HourlyExposureAverage> = measurements
            .groupBy { point -> ((point.timestamp / HOUR_MILLIS) % HOURS_PER_DAY).toInt() }
            .toSortedMap()
            .map { (hour, points) ->
                HourlyExposureAverage(
                    hour = hour,
                    avgDb = energyAverage(points),
                    maxDb = points.maxOf { it.dbWeighted },
                    sampleCount = points.size,
                )
            }

    fun daily(measurements: List<WeightedMeasurementPoint>): List<DailyExposureAverage> = measurements
            .groupBy { point -> (point.timestamp / DAY_MILLIS) * DAY_MILLIS }
            .toSortedMap()
            .map { (dayStartMs, points) ->
                DailyExposureAverage(
                    dayStartMs = dayStartMs,
                    avgDb = energyAverage(points),
                    maxDb = points.maxOf { it.dbWeighted },
                    sampleCount = points.size,
                )
            }

    private fun energyAverage(points: List<WeightedMeasurementPoint>): Float =
        DecibelMath.energyAverageDb(points.map { it.dbWeighted }) ?: 0f

    private const val HOUR_MILLIS = 60L * 60L * 1_000L
    private const val HOURS_PER_DAY = 24L
    private const val DAY_MILLIS = 24L * HOUR_MILLIS
}
