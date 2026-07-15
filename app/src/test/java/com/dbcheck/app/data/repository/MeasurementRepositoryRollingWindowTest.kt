package com.dbcheck.app.data.repository

import com.dbcheck.app.data.local.db.dao.EnvironmentMixCounts
import com.dbcheck.app.data.local.db.entity.MeasurementEntity
import com.dbcheck.app.domain.report.ReportMeasurement
import com.dbcheck.app.domain.session.SessionMeasurement
import com.dbcheck.app.test.EmptyMeasurementDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Executors

@OptIn(ExperimentalCoroutinesApi::class)
class MeasurementRepositoryRollingWindowTest {
    @Test
    fun sessionMeasurementsMapRowsToDomainModel() = runTest {
        val dao =
            RecordingMeasurementDao(
                sessionMeasurements =
                    listOf(
                        measurement(
                            sessionId = SESSION_ID,
                            timestamp = 100L,
                            dbValue = 61f,
                            dbWeighted = 62f,
                            peakDb = 72f,
                        ),
                    ),
            )
        val repository = MeasurementRepository(dao, UnconfinedTestDispatcher(testScheduler))

        val measurements = repository.getSessionMeasurements(SESSION_ID).first()

        assertEquals(
            listOf(SessionMeasurement(timestamp = 100L, dbValue = 61f, dbWeighted = 62f, peakDb = 72f)),
            measurements,
        )
        assertEquals(listOf(SESSION_ID), dao.sessionMeasurementCalls)
    }

    @Test
    fun reportMeasurementsMapSessionRowsToReportRows() = runTest {
        val dao =
            RecordingMeasurementDao(
                sessionMeasurements =
                    listOf(
                        measurement(
                            sessionId = SESSION_ID,
                            timestamp = 200L,
                            dbValue = 65f,
                            dbWeighted = 66f,
                            peakDb = 86f,
                        ),
                    ),
            )
        val repository = MeasurementRepository(dao, UnconfinedTestDispatcher(testScheduler))

        val measurements = repository.getReportMeasurementsForSession(SESSION_ID).first()

        assertEquals(
            listOf(ReportMeasurement(timestamp = 200L, dbWeighted = 66f, peakDb = 86f)),
            measurements,
        )
    }

    @Test
    fun hourlyAveragesRefreshRollingWindowWhileCollected() = runTest {
        val dao = RecordingMeasurementDao()
        val repository = MeasurementRepository(dao, StandardTestDispatcher(testScheduler))

        backgroundScope.launch {
            repository.getHourlyAveragesLast24H().collect {}
        }
        runCurrent()

        assertEquals(1, dao.measurementRangeCalls.size)

        advanceTimeBy(ROLLING_WINDOW_REFRESH_MS)
        runCurrent()

        assertEquals(2, dao.measurementRangeCalls.size)
    }

    @Test
    fun environmentMixRefreshesRollingWindowWhileCollected() = runTest {
        val dao = RecordingMeasurementDao()
        val repository = MeasurementRepository(dao, StandardTestDispatcher(testScheduler))

        backgroundScope.launch {
            repository.getEnvironmentMixLast7Days().collect {}
        }
        runCurrent()

        assertEquals(1, dao.environmentMixRangeCalls.size)

        advanceTimeBy(ROLLING_WINDOW_REFRESH_MS)
        runCurrent()

        assertEquals(2, dao.environmentMixRangeCalls.size)
    }

    @Test
    fun dailyAveragesExcludeFutureMeasurementsFromRollingWindow() = runTest {
        val dao = daoWithFutureMeasurement()
        val repository = MeasurementRepository(dao, StandardTestDispatcher(testScheduler))

        val averages = repository.getDailyAveragesLast7Days().first()

        assertTrue(averages.isEmpty())
    }

    @Test
    fun environmentMixExcludesFutureMeasurementsFromRollingWindow() = runTest {
        val dao = daoWithFutureMeasurement()
        val repository = MeasurementRepository(dao, StandardTestDispatcher(testScheduler))

        val counts = repository.getEnvironmentMixLast7Days().first()

        assertEquals(0L, counts.totalCount)
    }

    @Test
    fun hourlyAveragesCollectDaoFlowOnInjectedDispatcher() = runBlocking {
        val executor =
            Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "measurement-repository-test")
            }
        val dispatcher = executor.asCoroutineDispatcher()
        try {
            val dao = RecordingMeasurementDao(recordMeasurementFlowThread = true)
            val repository = MeasurementRepository(dao, dispatcher)

            repository.getHourlyAveragesLast24H().first()

            assertTrue(
                dao.measurementFlowThreadNames.any { threadName ->
                    threadName.contains("measurement-repository-test")
                },
            )
        } finally {
            dispatcher.close()
            executor.shutdown()
        }
    }

    private class RecordingMeasurementDao(
        private val recordMeasurementFlowThread: Boolean = false,
        private val measurements: List<MeasurementEntity> = emptyList(),
        private val sessionMeasurements: List<MeasurementEntity> = emptyList(),
    ) : EmptyMeasurementDao() {
        val measurementRangeCalls = mutableListOf<Pair<Long, Long>>()
        val environmentMixRangeCalls = mutableListOf<Pair<Long, Long>>()
        val measurementFlowThreadNames = mutableListOf<String>()
        val sessionMeasurementCalls = mutableListOf<Long>()

        override fun getMeasurementsForSession(sessionId: Long): Flow<List<MeasurementEntity>> {
            sessionMeasurementCalls += sessionId
            return flowOf(sessionMeasurements.filter { it.sessionId == sessionId })
        }

        override fun getMeasurementsInRange(startTime: Long, endTime: Long): Flow<List<MeasurementEntity>> {
            measurementRangeCalls += startTime to endTime
            val rows = measurements.filter { it.timestamp >= startTime && it.timestamp <= endTime }
            return if (recordMeasurementFlowThread) {
                flow {
                    measurementFlowThreadNames += Thread.currentThread().name
                    emit(rows)
                }
            } else {
                flowOf(rows)
            }
        }

        override fun getEnvironmentMixCountsInRange(
            startTime: Long,
            endTime: Long,
            quietMaxDb: Float,
            moderateMaxDb: Float,
            loudMaxDb: Float,
        ): Flow<EnvironmentMixCounts> {
            environmentMixRangeCalls += startTime to endTime
            val rows = measurements.filter { it.timestamp >= startTime && it.timestamp <= endTime }
            return flowOf(
                EnvironmentMixCounts(
                    quietCount = rows.count { it.dbWeighted < quietMaxDb }.toLong(),
                    moderateCount =
                        rows.count {
                            it.dbWeighted >= quietMaxDb &&
                                it.dbWeighted < moderateMaxDb
                        }.toLong(),
                    loudCount =
                        rows.count {
                            it.dbWeighted >= moderateMaxDb &&
                                it.dbWeighted < loudMaxDb
                        }.toLong(),
                    criticalCount = rows.count { it.dbWeighted >= loudMaxDb }.toLong(),
                    totalCount = rows.size.toLong(),
                ),
            )
        }
    }

    private fun measurement(
        timestamp: Long,
        dbWeighted: Float,
        sessionId: Long = 1L,
        dbValue: Float = dbWeighted,
        peakDb: Float = dbWeighted,
    ): MeasurementEntity = MeasurementEntity(
        sessionId = sessionId,
        timestamp = timestamp,
        dbValue = dbValue,
        dbWeighted = dbWeighted,
        peakDb = peakDb,
    )

    private fun daoWithFutureMeasurement(): RecordingMeasurementDao = RecordingMeasurementDao(
            measurements =
                listOf(
                    measurement(timestamp = System.currentTimeMillis() + DAY_MS, dbWeighted = 95f),
                ),
        )

    private companion object {
        const val SESSION_ID = 7L
        const val ROLLING_WINDOW_REFRESH_MS = 60_000L
        const val DAY_MS = 24L * 60L * 60L * 1_000L
    }
}
