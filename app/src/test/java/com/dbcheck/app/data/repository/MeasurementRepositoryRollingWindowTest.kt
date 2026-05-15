package com.dbcheck.app.data.repository

import com.dbcheck.app.data.local.db.dao.EnvironmentMixCounts
import com.dbcheck.app.data.local.db.dao.MeasurementDao
import com.dbcheck.app.data.local.db.dao.WeightedMeasurementPoint
import com.dbcheck.app.data.local.db.entity.MeasurementEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
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
        val now = System.currentTimeMillis()
        val dao =
            RecordingMeasurementDao(
                measurements =
                    listOf(
                        measurement(timestamp = now + DAY_MS, dbWeighted = 95f),
                    ),
            )
        val repository = MeasurementRepository(dao, StandardTestDispatcher(testScheduler))

        val averages = repository.getDailyAveragesLast7Days().first()

        assertTrue(averages.isEmpty())
    }

    @Test
    fun environmentMixExcludesFutureMeasurementsFromRollingWindow() = runTest {
        val now = System.currentTimeMillis()
        val dao =
            RecordingMeasurementDao(
                measurements =
                    listOf(
                        measurement(timestamp = now + DAY_MS, dbWeighted = 95f),
                    ),
            )
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
    ) : MeasurementDao {
        val measurementRangeCalls = mutableListOf<Pair<Long, Long>>()
        val environmentMixRangeCalls = mutableListOf<Pair<Long, Long>>()
        val measurementFlowThreadNames = mutableListOf<String>()

        override suspend fun insertMeasurements(measurements: List<MeasurementEntity>) = Unit

        override fun getMeasurementsForSession(sessionId: Long): Flow<List<MeasurementEntity>> = flowOf(emptyList())

        override suspend fun getMeasurementsForSessionExportPage(
            sessionId: Long,
            afterTimestamp: Long,
            afterId: Long,
            limit: Int,
        ): List<MeasurementEntity> = emptyList()

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

        override fun getWeightedMeasurementsInRange(
            startTime: Long,
            endTime: Long,
        ): Flow<List<WeightedMeasurementPoint>> = flowOf(emptyList())

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

    private fun measurement(timestamp: Long, dbWeighted: Float): MeasurementEntity = MeasurementEntity(
        sessionId = 1L,
        timestamp = timestamp,
        dbValue = dbWeighted,
        dbWeighted = dbWeighted,
    )

    private companion object {
        const val ROLLING_WINDOW_REFRESH_MS = 60_000L
        const val DAY_MS = 24L * 60L * 60L * 1_000L
    }
}
