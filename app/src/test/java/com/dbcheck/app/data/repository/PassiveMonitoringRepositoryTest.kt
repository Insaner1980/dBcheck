package com.dbcheck.app.data.repository

import android.app.Application
import com.dbcheck.app.data.local.db.createInMemoryDbCheckDatabase
import com.dbcheck.app.domain.passive.PassiveMonitoringSample
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PassiveMonitoringRepositoryTest {
    private val database = createInMemoryDbCheckDatabase()
    private val repository =
        PassiveMonitoringRepository(
            passiveMonitoringDao = database.passiveMonitoringDao(),
            defaultDispatcher = UnconfinedTestDispatcher(),
        )

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun dailySummaryAggregatesPassiveSamplesWithoutMeasurementRows() = runTest {
        repository.recordSample(sample(startedAt = DAY_START + 1_000L, readingCount = 2, averageDb = 70f, peakDb = 78f))
        repository.recordSample(sample(startedAt = DAY_START + 2_000L, readingCount = 4, averageDb = 80f, peakDb = 96f))
        repository.recordSample(sample(startedAt = NEXT_DAY_START + 1_000L, readingCount = 3, averageDb = 90f))

        val summary =
            repository
                .observeDailySummary(
                    startTimeMs = DAY_START,
                    endTimeMs = NEXT_DAY_START,
                ).first()

        assertTrue(summary.hasSamples)
        assertEquals(2, summary.sampleCount)
        assertEquals(6, summary.readingCount)
        assertEquals(70f, requireNotNull(summary.minDb), FLOAT_TOLERANCE)
        assertEquals(80f, requireNotNull(summary.maxDb), FLOAT_TOLERANCE)
        assertEquals(96f, requireNotNull(summary.peakDb), FLOAT_TOLERANCE)
        assertEquals(78.5f, requireNotNull(summary.averageDb), 0.1f)
        assertTrue(database.measurementDao().getMeasurementsInRange(DAY_START, NEXT_DAY_START).first().isEmpty())
    }

    @Test
    fun emptyDailySummaryIsExplicitlyUnavailable() = runTest {
        val summary =
            repository
                .observeDailySummary(
                    startTimeMs = DAY_START,
                    endTimeMs = NEXT_DAY_START,
                ).first()

        assertFalse(summary.hasSamples)
        assertEquals(0, summary.sampleCount)
        assertEquals(0, summary.readingCount)
    }

    private fun sample(
        startedAt: Long,
        readingCount: Int,
        averageDb: Float,
        peakDb: Float = averageDb,
    ): PassiveMonitoringSample = PassiveMonitoringSample(
            startedAtMs = startedAt,
            endedAtMs = startedAt + 1_000L,
            readingCount = readingCount,
            minDb = averageDb,
            averageDb = averageDb,
            maxDb = averageDb,
            peakDb = peakDb,
        )

    private companion object {
        const val DAY_START = 1_700_000_000_000L
        const val NEXT_DAY_START = DAY_START + 86_400_000L
        const val FLOAT_TOLERANCE = 0.001f
    }
}
