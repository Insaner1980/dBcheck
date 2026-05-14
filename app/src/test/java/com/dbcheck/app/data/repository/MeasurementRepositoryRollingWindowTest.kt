package com.dbcheck.app.data.repository

import com.dbcheck.app.data.local.db.dao.EnvironmentMixCounts
import com.dbcheck.app.data.local.db.dao.MeasurementDao
import com.dbcheck.app.data.local.db.dao.WeightedMeasurementPoint
import com.dbcheck.app.data.local.db.entity.MeasurementEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeasurementRepositoryRollingWindowTest {
    @Test
    fun hourlyAveragesRefreshRollingWindowWhileCollected() = runTest {
        val dao = RecordingMeasurementDao()
        val repository = MeasurementRepository(dao)

        backgroundScope.launch {
            repository.getHourlyAveragesLast24H().collect {}
        }
        runCurrent()

        assertEquals(1, dao.measurementsSinceCalls.size)

        advanceTimeBy(ROLLING_WINDOW_REFRESH_MS)
        runCurrent()

        assertEquals(2, dao.measurementsSinceCalls.size)
    }

    @Test
    fun environmentMixRefreshesRollingWindowWhileCollected() = runTest {
        val dao = RecordingMeasurementDao()
        val repository = MeasurementRepository(dao)

        backgroundScope.launch {
            repository.getEnvironmentMixLast7Days().collect {}
        }
        runCurrent()

        assertEquals(1, dao.environmentMixSinceCalls.size)

        advanceTimeBy(ROLLING_WINDOW_REFRESH_MS)
        runCurrent()

        assertEquals(2, dao.environmentMixSinceCalls.size)
    }

    private class RecordingMeasurementDao : MeasurementDao {
        val measurementsSinceCalls = mutableListOf<Long>()
        val environmentMixSinceCalls = mutableListOf<Long>()

        override suspend fun insertMeasurement(measurement: MeasurementEntity) = Unit

        override suspend fun insertMeasurements(measurements: List<MeasurementEntity>) = Unit

        override fun getMeasurementsForSession(sessionId: Long): Flow<List<MeasurementEntity>> = flowOf(emptyList())

        override suspend fun getMeasurementsForSessions(sessionIds: List<Long>): List<MeasurementEntity> = emptyList()

        override fun getMeasurementsSince(since: Long): Flow<List<MeasurementEntity>> {
            measurementsSinceCalls += since
            return flowOf(emptyList())
        }

        override fun getWeightedMeasurementsInRange(
            startTime: Long,
            endTime: Long,
        ): Flow<List<WeightedMeasurementPoint>> = flowOf(emptyList())

        override fun getEnvironmentMixCounts(
            since: Long,
            quietMaxDb: Float,
            moderateMaxDb: Float,
            loudMaxDb: Float,
        ): Flow<EnvironmentMixCounts> {
            environmentMixSinceCalls += since
            return flowOf(EnvironmentMixCounts())
        }
    }

    private companion object {
        const val ROLLING_WINDOW_REFRESH_MS = 60_000L
    }
}
