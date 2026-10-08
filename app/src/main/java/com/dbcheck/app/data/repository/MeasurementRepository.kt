package com.dbcheck.app.data.repository

import com.dbcheck.app.data.local.db.dao.EnvironmentMixCounts
import com.dbcheck.app.data.local.db.dao.MeasurementDao
import com.dbcheck.app.data.local.db.dao.WeightedMeasurementPoint
import com.dbcheck.app.data.local.db.entity.MeasurementEntity
import com.dbcheck.app.di.DefaultDispatcher
import com.dbcheck.app.domain.analytics.DailyExposureAverage
import com.dbcheck.app.domain.analytics.EnvironmentExposureMixCounts
import com.dbcheck.app.domain.analytics.HistoricalExposureIntervals
import com.dbcheck.app.domain.analytics.HourlyExposureAverage
import com.dbcheck.app.domain.analytics.WeightedExposureMeasurement
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
                            responseTime = measurement.responseTime,
                        )
                    }
                }

        fun getHourlyAveragesLast24H(): Flow<List<HourlyExposureAverage>> =
            getMeasurementsForRollingWindow(LAST_24_HOURS_MILLIS).map { measurements ->
                MeasurementBucketAverages.hourly(
                    measurements = measurements,
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
                MeasurementBucketAverages.daily(measurements)
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
        private fun getMeasurementsForRollingWindow(windowMillis: Long): Flow<List<WeightedExposureMeasurement>> =
            rollingWindowRanges(windowMillis)
                .flatMapLatest { window ->
                    getWeightedMeasurementsInRange(
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
    sessionId = sessionId,
    frequencyWeighting = frequencyWeighting,
    coverageStartMs = coverageStartMs,
    coverageEndMs = coverageEndMs,
)

private fun MeasurementEntity.toSessionMeasurement(): SessionMeasurement = SessionMeasurement(
        timestamp = timestamp,
        dbValue = dbValue,
        dbWeighted = dbWeighted,
        peakDb = peakDb,
        aWeightedDb = aWeightedDb,
        responseTime = responseTime,
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
        measurements: List<WeightedExposureMeasurement>,
        zoneId: ZoneId = ZoneOffset.UTC,
    ): List<HourlyExposureAverage> = HistoricalExposureIntervals.buckets(measurements, zoneId, hourly = true)
        .map { (startMs, intervals) ->
            val observations = observations(measurements, startMs, zoneId, hourly = true)
            HourlyExposureAverage(
                hour = Instant.ofEpochMilli(startMs).atZone(zoneId).hour,
                avgDb = requireNotNull(HistoricalExposureIntervals.average(intervals)),
                maxDb = maxOf(
                    intervals.maxOf { it.db },
                    observations.maxOfOrNull { it.dbWeighted } ?: Float.NEGATIVE_INFINITY,
                ),
                sampleCount = observations.size,
                hourStartMs = startMs,
                durationMs = intervals.sumOf { it.durationMs },
            )
        }

    fun daily(
        measurements: List<WeightedExposureMeasurement>,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<DailyExposureAverage> = HistoricalExposureIntervals.buckets(measurements, zoneId, hourly = false)
        .map { (startMs, intervals) ->
            val observations = observations(measurements, startMs, zoneId, hourly = false)
            DailyExposureAverage(
                dayStartMs = startMs,
                avgDb = requireNotNull(HistoricalExposureIntervals.average(intervals)),
                maxDb = maxOf(
                    intervals.maxOf { it.db },
                    observations.maxOfOrNull { it.dbWeighted } ?: Float.NEGATIVE_INFINITY,
                ),
                sampleCount = observations.size,
                durationMs = intervals.sumOf { it.durationMs },
            )
        }

    private fun observations(
        points: List<WeightedExposureMeasurement>,
        startMs: Long,
        zoneId: ZoneId,
        hourly: Boolean,
    ): List<WeightedExposureMeasurement> =
        points.filter {
            it.frequencyWeighting == "A" && it.timestamp >= it.coverageStartMs && it.timestamp <= it.coverageEndMs &&
                HistoricalExposureIntervals
                    .bucketStart(it.timestamp, zoneId, hourly)
                    .toInstant()
                    .toEpochMilli() == startMs
        }
}
