package com.dbcheck.app.data.repository

import com.dbcheck.app.data.local.db.dao.EnvironmentMixCounts
import com.dbcheck.app.data.local.db.dao.MeasurementDao
import com.dbcheck.app.data.local.db.dao.WeightedMeasurementPoint
import com.dbcheck.app.data.local.db.entity.MeasurementEntity
import com.dbcheck.app.di.DefaultDispatcher
import com.dbcheck.app.domain.analytics.DailyExposureAverage
import com.dbcheck.app.domain.analytics.EnvironmentExposureMixCounts
import com.dbcheck.app.domain.analytics.HourlyExposureAverage
import com.dbcheck.app.domain.analytics.WeightedExposureMeasurement
import com.dbcheck.app.domain.noise.DecibelMath
import com.dbcheck.app.domain.noise.NoiseLevel
import com.dbcheck.app.domain.report.ReportMeasurement
import com.dbcheck.app.domain.session.SessionMeasurement
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeasurementRepository
    @Inject
    constructor(
        private val measurementDao: MeasurementDao,
        @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    ) {
        fun getSessionMeasurements(sessionId: Long): Flow<List<SessionMeasurement>> =
            measurementDao.getMeasurementsForSession(sessionId)
                .map { measurements -> measurements.map { it.toSessionMeasurement() } }
                .flowOn(defaultDispatcher)

        fun getReportMeasurementsForSession(sessionId: Long): Flow<List<ReportMeasurement>> =
            getSessionMeasurements(sessionId)
                .map { measurements ->
                    measurements.map { measurement ->
                        ReportMeasurement(
                            timestamp = measurement.timestamp,
                            dbWeighted = measurement.dbWeighted,
                            peakDb = measurement.peakDb,
                        )
                    }
                }

        fun getHourlyAveragesLast24H(): Flow<List<HourlyExposureAverage>> =
            getMeasurementsForRollingWindow(LAST_24_HOURS_MILLIS).map { measurements ->
                MeasurementBucketAverages.hourly(
                    measurements = measurements.map { it.toWeightedMeasurementPoint() },
                    zoneId = ZoneId.systemDefault(),
                )
            }.flowOn(defaultDispatcher)

        fun getWeightedMeasurementsInRange(startTime: Long, endTime: Long): Flow<List<WeightedExposureMeasurement>> =
            measurementDao.getWeightedMeasurementsInRange(
                startTime = startTime,
                endTime = endTime,
            ).map { measurements ->
                measurements.map { it.toDomainModel() }
            }.flowOn(defaultDispatcher)

        fun getDailyAveragesLast7Days(): Flow<List<DailyExposureAverage>> =
            getMeasurementsForRollingWindow(LAST_7_DAYS_MILLIS).map { measurements ->
                MeasurementBucketAverages.daily(measurements.map { it.toWeightedMeasurementPoint() })
            }.flowOn(defaultDispatcher)

        @OptIn(ExperimentalCoroutinesApi::class)
        fun getEnvironmentMixLast7Days(): Flow<EnvironmentExposureMixCounts> = rollingWindowRanges(LAST_7_DAYS_MILLIS)
                .flatMapLatest { window ->
                    measurementDao.getEnvironmentMixCountsInRange(
                        startTime = window.startTime,
                        endTime = window.endTime,
                        quietMaxDb = NoiseLevel.QUIET.maxDb,
                        moderateMaxDb = NoiseLevel.NORMAL.maxDb,
                        loudMaxDb = NoiseLevel.ELEVATED.maxDb,
                    )
                }.map { it.toDomainModel() }
                .flowOn(defaultDispatcher)

        @OptIn(ExperimentalCoroutinesApi::class)
        private fun getMeasurementsForRollingWindow(windowMillis: Long): Flow<List<MeasurementEntity>> =
            rollingWindowRanges(windowMillis)
                .flatMapLatest { window ->
                    measurementDao.getMeasurementsInRange(
                        startTime = window.startTime,
                        endTime = window.endTime,
                    )
                }

        private companion object {
            const val DAY_MILLIS = 24 * 60 * 60 * 1000L
            const val LAST_24_HOURS_MILLIS = DAY_MILLIS
            const val LAST_7_DAYS_MILLIS = 7 * DAY_MILLIS
            const val ROLLING_WINDOW_REFRESH_MILLIS = 60_000L
        }

        private fun rollingWindowRanges(windowMillis: Long): Flow<RollingWindowRange> = flow {
            while (currentCoroutineContext().isActive) {
                val nowMs = System.currentTimeMillis()
                emit(RollingWindowRange(startTime = nowMs - windowMillis, endTime = nowMs))
                delay(ROLLING_WINDOW_REFRESH_MILLIS)
            }
        }
    }

private data class RollingWindowRange(val startTime: Long, val endTime: Long)

private fun WeightedMeasurementPoint.toDomainModel(): WeightedExposureMeasurement = WeightedExposureMeasurement(
        timestamp = timestamp,
        dbWeighted = dbWeighted,
    )

private fun MeasurementEntity.toWeightedMeasurementPoint(): WeightedMeasurementPoint = WeightedMeasurementPoint(
        timestamp = timestamp,
        dbWeighted = dbWeighted,
    )

private fun MeasurementEntity.toSessionMeasurement(): SessionMeasurement = SessionMeasurement(
        timestamp = timestamp,
        dbValue = dbValue,
        dbWeighted = dbWeighted,
        peakDb = peakDb,
    )

private fun EnvironmentMixCounts.toDomainModel(): EnvironmentExposureMixCounts = EnvironmentExposureMixCounts(
        quietCount = quietCount,
        moderateCount = moderateCount,
        loudCount = loudCount,
        criticalCount = criticalCount,
        totalCount = totalCount,
    )

internal object MeasurementBucketAverages {
    fun hourly(
        measurements: List<WeightedMeasurementPoint>,
        zoneId: ZoneId = ZoneOffset.UTC,
    ): List<HourlyExposureAverage> = measurements
            .groupBy { point -> point.hourStartMs(zoneId) }
            .toSortedMap()
            .map { (hourStartMs, points) ->
                HourlyExposureAverage(
                    hour = Instant.ofEpochMilli(hourStartMs).atZone(zoneId).hour,
                    avgDb = energyAverage(points),
                    maxDb = points.maxOf { it.dbWeighted },
                    sampleCount = points.size,
                    hourStartMs = hourStartMs,
                    durationMs = persistenceDurationMs(points),
                )
            }

    fun daily(
        measurements: List<WeightedMeasurementPoint>,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<DailyExposureAverage> = measurements
            .groupBy { point -> point.dayStartMs(zoneId) }
            .toSortedMap()
            .map { (dayStartMs, points) ->
                DailyExposureAverage(
                    dayStartMs = dayStartMs,
                    avgDb = energyAverage(points),
                    maxDb = points.maxOf { it.dbWeighted },
                    sampleCount = points.size,
                )
            }

    private fun energyAverage(points: List<WeightedMeasurementPoint>): Float = if (points.isEmpty()) {
            0f
        } else {
            val sortedPoints = points.sortedBy { it.timestamp }
            var totalEnergy = 0.0
            var totalWeight = 0.0
            sortedPoints.forEachIndexed { index, point ->
                val weight = sortedPoints.persistenceWeightAt(index)
                totalEnergy += DecibelMath.energyFromDb(point.dbWeighted) * weight
                totalWeight += weight
            }
            DecibelMath.energyAverageDb(totalEnergy, totalWeight) ?: 0f
        }

    private fun List<WeightedMeasurementPoint>.persistenceWeightAt(index: Int): Double {
        val point = this[index]
        val previous = getOrNull(index - 1)
        val next = getOrNull(index + 1)
        val interval =
            when {
                previous != null -> point.timestamp - previous.timestamp
                next != null -> next.timestamp - point.timestamp
                else -> DEFAULT_PERSISTENCE_WEIGHT_MS
            }
        return interval.coerceAtLeast(1L).toDouble()
    }

    private fun persistenceDurationMs(points: List<WeightedMeasurementPoint>): Long {
        val sortedPoints = points.sortedBy { it.timestamp }
        return sortedPoints
            .mapIndexed { index, _ -> sortedPoints.persistenceWeightAt(index).toLong() }
            .sum()
    }

    private fun WeightedMeasurementPoint.dayStartMs(zoneId: ZoneId): Long = Instant
            .ofEpochMilli(timestamp)
            .atZone(zoneId)
            .toLocalDate()
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

    private fun WeightedMeasurementPoint.hourStartMs(zoneId: ZoneId): Long = Instant
            .ofEpochMilli(timestamp)
            .atZone(zoneId)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .toInstant()
            .toEpochMilli()

    private const val DEFAULT_PERSISTENCE_WEIGHT_MS = 1_000L
}
