package com.dbcheck.app.data.local.db

import android.app.Application
import androidx.lifecycle.ViewModelStore
import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.data.local.db.entity.MeasurementEntity
import com.dbcheck.app.data.local.db.entity.SessionEntity
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.MeasurementRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.data.repository.SessionRepository
import com.dbcheck.app.data.repository.SleepSessionRepository
import com.dbcheck.app.domain.analytics.ExposureAnalyticsCalculator
import com.dbcheck.app.testStringContext
import com.dbcheck.app.ui.history.HistoryViewModel
import com.dbcheck.app.ui.history.state.HistoryUiState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HistoricalExposureContractTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private lateinit var database: DbCheckDatabase
    private lateinit var repository: MeasurementRepository
    private val stores = mutableListOf<ViewModelStore>()
    private val hourStart = Instant.now().atZone(ZoneOffset.UTC).minusHours(2)
        .withMinute(0).withSecond(0).withNano(0).toInstant().toEpochMilli()

    @Before
    fun setUp() {
        database = createInMemoryDbCheckDatabase()
        repository = MeasurementRepository(database.measurementDao(), Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        stores.forEach { it.clear() }
        database.close()
    }

    @Test
    fun separateSessionsDoNotTurnTheirGapIntoSafeHours() = runTest {
        insert(1, "A", 0L to 60f, 10_000L to 60f)
        insert(2, "A", 1_200_000L to 60f, 1_210_000L to 60f)
        val hourly = repository.getHourlyAveragesLast24H().first()
        val viewModel = history(mockk { every { getHourlyAveragesLast24H() } returns flowOf(hourly) })
        runCurrent()
        val state = viewModel.uiState.value as HistoryUiState.Success
        assertEquals(20_000f / 3_600_000f, state.safeHours!!, 0.00001f)
        assertEquals(20_000L, hourly.sumOf { it.durationMs })
    }

    @Test
    fun exposureRejectsNonAWeightingIncludingLegacyAFieldFallback() = runTest {
        insert(1, "A", 0L to 60f, 1_000L to 60f)
        insert(2, "C", 2_000L to 100f, 3_000L to 100f)
        insert(3, "Z", 4_000L to 110f, 5_000L to 110f)
        insert(4, "unknown", 5_100L to 120f, 5_200L to 120f)
        val points = repository.getWeightedMeasurementsInRange(hourStart, hourStart + 6_000).first()
        assertEquals(setOf(1L, 2L, 3L, 4L), points.map { it.sessionId }.toSet())
        assertEquals(setOf("A", "C", "Z", "unknown"), points.map { it.frequencyWeighting }.toSet())
        assertEquals(60f, ExposureAnalyticsCalculator.calculateLaeq(points)!!, 0.001f)
        assertEquals(60f, repository.getDailyAveragesLast7Days().first().single().avgDb, 0.001f)
    }

    @Test
    fun extraPeakObservationIsWeightedByElapsedTime() = runTest {
        insert(1, "A", 0L to 60f, 100L to 90f, 1_000L to 60f)
        val points = repository.getWeightedMeasurementsInRange(hourStart, hourStart + 1_000).first()
        // (100 ms * 10^6 + 900 ms * 10^9) / 1000 ms.
        assertEquals(89.54291f, ExposureAnalyticsCalculator.calculateLaeq(points)!!, 0.0001f)
        assertEquals(1_000L, repository.getHourlyAveragesLast24H().first().single().durationMs)
        val report = ExposureAnalyticsCalculator.buildYearlyReport(points, 1, hourStart + 1_000, ZoneOffset.UTC)
        assertEquals(listOf(0, 10, 0, 90), report.zoneDistribution.map { it.percent })
    }

    @Test
    fun singleObservationDoesNotEstablishExposure() = runTest {
        insert(1, "A", 0L to 60f)
        val points = repository.getWeightedMeasurementsInRange(hourStart, hourStart + 1_000).first()
        assertNull(ExposureAnalyticsCalculator.calculateLaeq(points))
        assertEquals(emptyList<Any>(), repository.getHourlyAveragesLast24H().first())
    }

    @Test
    fun nonAOnlyHistoryDoesNotBecomeMeasuredSafeTime() = runTest {
        insert(1, "C", 0L to 40f, 10_000L to 40f)
        val hourly = repository.getHourlyAveragesLast24H().first()
        val rows = repository.getWeightedMeasurementsInRange(hourStart, hourStart + 10_000).first()
        assertNull(ExposureAnalyticsCalculator.calculateLaeq(rows))
        assertEquals(emptyList<Any>(), hourly)
        assertEquals(0L, repository.getEnvironmentMixLast7Days().first().totalCount)
    }

    @Test
    fun terminalAndFutureRowsDoNotInventCoverageToTheQueryEnd() = runTest {
        insert(1, "A", 0L to 60f, 1_000L to 60f, 10_000L to 120f)
        val points = repository.getWeightedMeasurementsInRange(hourStart, hourStart + 5_000).first()
        assertEquals(listOf(hourStart, hourStart + 1_000), points.map { it.timestamp })
        assertEquals(60f, ExposureAnalyticsCalculator.calculateLaeq(points)!!, 0f)
        assertEquals(
            1_000L,
            com.dbcheck.app.domain.analytics.HistoricalExposureIntervals.from(points).sumOf { it.durationMs }
        )
    }

    @Test
    fun duplicateTimestampUsesLastPersistedRowWithoutMultiplyingDuration() = runTest {
        insert(1, "A", 0L to 60f, 0L to 90f, 1_000L to 60f)
        val hourly = repository.getHourlyAveragesLast24H().first().single()
        assertEquals(90f, hourly.avgDb, 0f)
        assertEquals(1_000L, hourly.durationMs)
    }

    @Test
    fun reportingStartKeepsPrecedingObservationForClippedInterval() = runTest {
        insert(1, "A", 0L to 60f, 1_000L to 90f, 2_000L to 60f)
        val points = repository.getWeightedMeasurementsInRange(hourStart + 500, hourStart + 2_000).first()
        val trend = ExposureAnalyticsCalculator.buildMonthlyTrend(
            points, nowMs = hourStart + 2_000, zoneId = ZoneOffset.UTC,
        )
        // 500 ms at 60 dBA and 1000 ms at 90 dBA, with no extrapolated tail.
        assertEquals(88.24126f, trend.laeqDb!!, 0.0001f)
    }

    private suspend fun insert(id: Long, weighting: String, vararg readings: Pair<Long, Float>) {
        database.sessionDao().insertSession(SessionEntity(
            id = id, startTime = hourStart + readings.first().first,
            endTime = hourStart + readings.last().first, frequencyWeighting = weighting,
        ))
        database.measurementDao().insertMeasurements(readings.map { (offset, db) ->
            MeasurementEntity(sessionId = id, timestamp = hourStart + offset, dbValue = db, dbWeighted = db)
        })
    }

    private fun history(measurements: MeasurementRepository): HistoryViewModel {
        val sessions = mockk<SessionRepository> { every { getRecentSessions(20) } returns flowOf(emptyList()) }
        val preferences = mockk<PreferencesRepository> {
            every { userPreferences } returns flowOf(UserPreferences(isProUser = true))
        }
        val sleep = mockk<SleepSessionRepository> { every { getSleepSessionIds() } returns flowOf(emptySet()) }
        return HistoryViewModel(testStringContext(), sessions, measurements, preferences, sleep).also {
            val store = ViewModelStore()
            store.put("history", it)
            stores += store
        }
    }
}
