package com.dbcheck.app.data.local.db

import android.app.Application
import com.dbcheck.app.data.local.db.dao.SessionSearchAverageDbRange
import com.dbcheck.app.data.local.db.dao.SessionSearchQuery
import com.dbcheck.app.data.local.db.dao.SessionSearchTimeRange
import com.dbcheck.app.data.local.db.entity.MeasurementEntity
import com.dbcheck.app.data.local.db.entity.SessionEntity
import com.dbcheck.app.data.local.db.entity.SoundDetectionEventEntity
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
class SessionDaoHistorySearchQueryTest {
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
    fun searchSessionsAppliesNameTagDateLevelWeightingAndLocationFilters() = runTest {
        insertCompletedSession(
            session(
                id = 1L,
                startTime = 1_000L,
                name = "Office baseline",
                tags = "Calibration",
                avgDb = 72f,
                frequencyWeighting = "A",
                withLocation = true,
            ),
        )
        insertCompletedSession(
            session(
                id = 2L,
                startTime = 1_100L,
                name = "Home baseline",
                tags = "Office",
                avgDb = 80f,
                frequencyWeighting = "C",
                withLocation = false,
            ),
        )
        insertCompletedSession(
            session(
                id = 3L,
                startTime = 1_200L,
                name = "Workshop",
                tags = "Office,Focus",
                avgDb = 86f,
                frequencyWeighting = "A",
                withLocation = true,
            ),
        )
        insertCompletedSession(
            session(
                id = 4L,
                startTime = 1_300L,
                name = "Office quiet",
                tags = "Focus",
                avgDb = 50f,
                frequencyWeighting = "A",
                withLocation = true,
            ),
        )

        val sessions =
            database.sessionDao().searchSessions(
                SessionSearchQuery(
                    historyStartTime = 900L,
                    nameOrTagPattern = "%Office%",
                    timeRange =
                        SessionSearchTimeRange(
                            startTimeFrom = 1_000L,
                            startTimeTo = 1_250L,
                        ),
                    averageDbRange =
                        SessionSearchAverageDbRange(
                            minAvgDb = 70f,
                            maxAvgDb = 90f,
                        ),
                    frequencyWeighting = "A",
                    hasLocation = 1,
                ),
            ).first()

        assertEquals(listOf(3L, 1L), sessions.map { it.id })
    }

    @Test
    fun searchSessionsKeepsStableOrderingForEqualStartTimes() = runTest {
        insertCompletedSession(session(id = 1L, startTime = 1_000L))
        insertCompletedSession(session(id = 3L, startTime = 1_100L))
        insertCompletedSession(session(id = 2L, startTime = 1_100L))

        val sessions =
            database.sessionDao().searchSessions(
                SessionSearchQuery(historyStartTime = 0L),
            ).first()

        assertEquals(listOf(3L, 2L, 1L), sessions.map { it.id })
    }

    @Test
    fun deleteInactiveSessionsKeepsActiveSessionAndCascadesHistoryChildren() = runTest {
        database.sessionDao().insertSession(session(id = 1L, startTime = 1_000L))
        database.sessionDao().insertSession(session(id = 2L, startTime = 2_000L, isActive = true))
        database.measurementDao().insertMeasurements(
            listOf(
                MeasurementEntity(id = 1L, sessionId = 1L, timestamp = 1_001L, dbValue = 70f, dbWeighted = 70f),
                MeasurementEntity(id = 2L, sessionId = 2L, timestamp = 2_001L, dbValue = 60f, dbWeighted = 60f),
            ),
        )
        database.soundDetectionEventDao().insertEvent(
            SoundDetectionEventEntity(id = 1L, sessionId = 1L, timestamp = 1_002L, label = "Speech", confidence = 0.8f),
        )
        database.soundDetectionEventDao().insertEvent(
            SoundDetectionEventEntity(id = 2L, sessionId = 2L, timestamp = 2_002L, label = "Music", confidence = 0.7f),
        )

        val inactiveSessionIds = database.sessionDao().getInactiveSessionIds()
        val deletedCount = database.sessionDao().deleteInactiveSessions()

        assertEquals(listOf(1L), inactiveSessionIds)
        assertEquals(1, deletedCount)
        assertEquals(null, database.sessionDao().getSessionById(1L).first())
        assertEquals(2L, database.sessionDao().getSessionById(2L).first()?.id)
        assertEquals(emptyList<Long>(), database.measurementDao().getMeasurementsForSession(1L).first().map { it.id })
        assertEquals(listOf(2L), database.measurementDao().getMeasurementsForSession(2L).first().map { it.id })
        assertEquals(emptyList<Long>(), database.soundDetectionEventDao().getEventsForSession(1L).first().map { it.id })
        assertEquals(listOf(2L), database.soundDetectionEventDao().getEventsForSession(2L).first().map { it.id })
    }

    private suspend fun insertCompletedSession(session: SessionEntity) {
        database.sessionDao().insertSession(session)
        database.measurementDao().insertMeasurements(
            listOf(
                MeasurementEntity(
                    sessionId = session.id,
                    timestamp = session.startTime + 1L,
                    dbValue = session.avgDb,
                    dbWeighted = session.avgDb,
                ),
            ),
        )
    }

    private fun session(
        id: Long,
        startTime: Long,
        name: String? = null,
        tags: String? = null,
        avgDb: Float = 72f,
        frequencyWeighting: String = "A",
        withLocation: Boolean = false,
        isActive: Boolean = false,
    ): SessionEntity = SessionEntity(
        id = id,
        startTime = startTime,
        endTime = if (isActive) null else startTime + 60_000L,
        avgDb = avgDb,
        name = name,
        tags = tags,
        isActive = isActive,
        frequencyWeighting = frequencyWeighting,
        locationLatitude = if (withLocation) 60.1699 else null,
        locationLongitude = if (withLocation) 24.9384 else null,
        locationAccuracyMeters = if (withLocation) 25f else null,
        locationCapturedAt = if (withLocation) startTime + 1_000L else null,
    )
}
