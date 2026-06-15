package com.dbcheck.app.data.local.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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

@RunWith(RobolectricTestRunner::class)
class SessionDaoHistorySearchQueryTest {
    private lateinit var database: DbCheckDatabase

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    DbCheckDatabase::class.java,
                )
                .allowMainThreadQueries()
                .build()
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
                historyStartTime = 900L,
                nameOrTagPattern = "%Office%",
                startTimeFrom = 1_000L,
                startTimeTo = 1_250L,
                minAvgDb = 70f,
                maxAvgDb = 90f,
                frequencyWeighting = "A",
                hasLocation = 1,
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
                historyStartTime = 0L,
                nameOrTagPattern = null,
                startTimeFrom = null,
                startTimeTo = null,
                minAvgDb = null,
                maxAvgDb = null,
                frequencyWeighting = null,
                hasLocation = null,
            ).first()

        assertEquals(listOf(3L, 2L, 1L), sessions.map { it.id })
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
    ): SessionEntity = SessionEntity(
        id = id,
        startTime = startTime,
        endTime = startTime + 60_000L,
        avgDb = avgDb,
        name = name,
        tags = tags,
        isActive = false,
        frequencyWeighting = frequencyWeighting,
        locationLatitude = if (withLocation) 60.1699 else null,
        locationLongitude = if (withLocation) 24.9384 else null,
        locationAccuracyMeters = if (withLocation) 25f else null,
        locationCapturedAt = if (withLocation) startTime + 1_000L else null,
    )
}
