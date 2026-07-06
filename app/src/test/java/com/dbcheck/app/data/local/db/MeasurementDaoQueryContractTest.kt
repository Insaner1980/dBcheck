package com.dbcheck.app.data.local.db

import android.app.Application
import com.dbcheck.app.data.local.db.entity.MeasurementEntity
import com.dbcheck.app.data.local.db.entity.SessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class MeasurementDaoQueryContractTest {
    private lateinit var database: DbCheckDatabase

    @Before
    fun setUp() {
        database = createInMemoryDbCheckDatabase()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun exportPageQueryUsesStableKeysetPagination() = runTest {
        insertSession()
        database.measurementDao().insertMeasurements(
            listOf(
                measurement(id = 1L, timestamp = 100L),
                measurement(id = 2L, timestamp = 100L),
                measurement(id = 3L, timestamp = 101L),
                measurement(id = 4L, timestamp = 102L),
            ),
        )

        val firstPage =
            database.measurementDao().getMeasurementsForSessionExportPage(
                sessionId = SESSION_ID,
                afterTimestamp = Long.MIN_VALUE,
                afterId = Long.MIN_VALUE,
                limit = 3,
            )
        val secondPage =
            database.measurementDao().getMeasurementsForSessionExportPage(
                sessionId = SESSION_ID,
                afterTimestamp = firstPage.last().timestamp,
                afterId = firstPage.last().id,
                limit = 3,
            )

        assertEquals(listOf(1L, 2L, 3L), firstPage.map { it.id })
        assertEquals(listOf(4L), secondPage.map { it.id })
    }

    @Test
    fun weightedRangeQueryUsesInclusiveTimeWindowAndStableOrdering() = runTest {
        insertSession()
        database.measurementDao().insertMeasurements(
            listOf(
                measurement(id = 1L, timestamp = 99L, dbWeighted = 59f),
                measurement(id = 2L, timestamp = 100L, dbWeighted = 60f),
                measurement(id = 3L, timestamp = 150L, dbWeighted = 70f),
                measurement(id = 4L, timestamp = 200L, dbWeighted = 80f),
                measurement(id = 5L, timestamp = 201L, dbWeighted = 81f),
            ),
        )

        val points = database.measurementDao().getWeightedMeasurementsInRange(100L, 200L).first()

        assertEquals(listOf(100L, 150L, 200L), points.map { it.timestamp })
        assertEquals(listOf(60f, 70f, 80f), points.map { it.dbWeighted })
    }

    @Test
    fun environmentMixQueryBucketsWeightedSamplesByNoiseBoundaries() = runTest {
        insertSession()
        database.measurementDao().insertMeasurements(
            listOf(
                measurement(id = 1L, timestamp = 100L, dbWeighted = 39.9f),
                measurement(id = 2L, timestamp = 101L, dbWeighted = 40f),
                measurement(id = 3L, timestamp = 102L, dbWeighted = 69.9f),
                measurement(id = 4L, timestamp = 103L, dbWeighted = 70f),
                measurement(id = 5L, timestamp = 104L, dbWeighted = 84.9f),
                measurement(id = 6L, timestamp = 105L, dbWeighted = 85f),
            ),
        )

        val counts =
            database
                .measurementDao()
                .getEnvironmentMixCountsInRange(
                    startTime = 100L,
                    endTime = 105L,
                    quietMaxDb = 40f,
                    moderateMaxDb = 70f,
                    loudMaxDb = 85f,
                )
                .first()

        assertEquals(1L, counts.quietCount)
        assertEquals(2L, counts.moderateCount)
        assertEquals(2L, counts.loudCount)
        assertEquals(1L, counts.criticalCount)
        assertEquals(6L, counts.totalCount)
    }

    private suspend fun insertSession() {
        database.sessionDao().insertSession(
            SessionEntity(
                id = SESSION_ID,
                startTime = 1L,
                endTime = 2L,
                avgDb = 70f,
                frequencyWeighting = "A",
            ),
        )
    }

    private fun measurement(id: Long, timestamp: Long, dbWeighted: Float = 70f): MeasurementEntity = MeasurementEntity(
        id = id,
        sessionId = SESSION_ID,
        timestamp = timestamp,
        dbValue = dbWeighted,
        dbWeighted = dbWeighted,
    )

    private companion object {
        const val SESSION_ID = 7L
    }
}
