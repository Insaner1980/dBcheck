package com.dbcheck.app.data.repository

import com.dbcheck.app.data.local.db.DbCheckDatabase
import com.dbcheck.app.data.local.db.dao.MeasurementDao
import com.dbcheck.app.data.local.db.dao.SessionDao
import com.dbcheck.app.data.local.db.entity.SessionEntity
import com.dbcheck.app.data.local.preferences.UserPreferencesDataStore
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.domain.session.SessionHistoryQuery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.slot
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRepositoryHistoryPolicyTest {
    private val database = mockk<DbCheckDatabase>(relaxed = true)
    private val measurementDao = mockk<MeasurementDao>(relaxed = true)
    private val sessionDao = mockk<SessionDao>(relaxed = true)
    private val preferencesDataStore = mockk<UserPreferencesDataStore>()

    @Test
    fun proUserHistoryUsesAllCompletedSessions() = runTest {
        val session = session(id = 7L)
        every { preferencesDataStore.userPreferences } returns flowOf(UserPreferences(isProUser = true))
        every { sessionDao.getAllSessions() } returns flowOf(listOf(session))
        val repository = createRepository()

        val sessions = repository.getSessions().first()

        assertEquals(listOf(7L), sessions.map { it.id })
        verify { sessionDao.getAllSessions() }
        verify(exactly = 0) { sessionDao.getSessionsLast7Days(any()) }
    }

    @Test
    fun freeUserHistoryUsesSevenDayWindow() = runTest {
        val session = session(id = 8L)
        every { preferencesDataStore.userPreferences } returns flowOf(UserPreferences(isProUser = false))
        every { sessionDao.getSessionsLast7Days(any()) } returns flowOf(listOf(session))
        val repository = createRepository()

        val sessions = repository.getSessions().first()

        assertEquals(listOf(8L), sessions.map { it.id })
        verify { sessionDao.getSessionsLast7Days(any()) }
        verify(exactly = 0) { sessionDao.getAllSessions() }
    }

    @Test
    fun filteredFreeUserHistoryKeepsSevenDayWindowAndMapsFilters() = runTest {
        val historyStartTime = slot<Long>()
        every { preferencesDataStore.userPreferences } returns flowOf(UserPreferences(isProUser = false))
        every {
            sessionDao.searchSessions(
                historyStartTime = capture(historyStartTime),
                nameOrTagPattern = "%Office\\_One%",
                startTimeFrom = 1_700_000_000_000L,
                startTimeTo = 1_700_086_400_000L,
                minAvgDb = 70f,
                maxAvgDb = 90f,
                frequencyWeighting = "A",
                hasLocation = 1,
            )
        } returns flowOf(listOf(session(id = 21L)))
        val repository = createRepository()

        val sessions =
            repository.getFilteredSessions(
                SessionHistoryQuery(
                    nameOrTag = " Office_One ",
                    startTimeFrom = 1_700_000_000_000L,
                    startTimeTo = 1_700_086_400_000L,
                    minAvgDb = 70f,
                    maxAvgDb = 90f,
                    frequencyWeighting = "A",
                    hasLocation = true,
                ),
            ).first()

        assertEquals(listOf(21L), sessions.map { it.id })
        assertTrue(historyStartTime.captured > Long.MIN_VALUE)
        verify(exactly = 0) { sessionDao.getAllSessions() }
        verify(exactly = 0) { sessionDao.getSessionsLast7Days(any()) }
    }

    @Test
    fun cleanupOldSessionsDeletesOnlyForFreeUsers() = runTest {
        every { preferencesDataStore.userPreferences } returns flowOf(UserPreferences(isProUser = false))
        val repository = createRepository()

        repository.cleanupOldSessions()

        coVerify(exactly = 1) { sessionDao.deleteSessionsOlderThan(any()) }
    }

    @Test
    fun cleanupOldSessionsKeepsProHistory() = runTest {
        every { preferencesDataStore.userPreferences } returns flowOf(UserPreferences(isProUser = true))
        val repository = createRepository()

        repository.cleanupOldSessions()

        coVerify(exactly = 0) { sessionDao.deleteSessionsOlderThan(any()) }
    }

    @Test
    fun readFlowsMapSessionEntitiesToDomainModels() = runTest {
        val active = session(id = 10L, isActive = true, tags = "Work,Duplicate,duplicate")
        val completed = session(id = 11L, isActive = false, tags = "Focus")
        every { sessionDao.getActiveSession() } returns flowOf(active)
        every { sessionDao.getSessionById(11L) } returns flowOf(completed)
        every { sessionDao.getRecentSessions(5) } returns flowOf(listOf(completed))
        every { sessionDao.getAllSessions() } returns flowOf(listOf(completed, active))
        every { sessionDao.getSessionsInRange(100L, 200L) } returns flowOf(listOf(completed))
        val repository = createRepository()

        val activeSession = repository.getActiveSession().first()
        val sessionById = repository.getSessionById(11L).first()
        val recentSessions = repository.getRecentSessions(limit = 5).first()
        val completedSessions = repository.getAllCompletedSessions().first()
        val sessionsInRange = repository.getSessionsInRange(100L, 200L).first()

        checkNotNull(activeSession)
        assertEquals(10L, activeSession.id)
        assertEquals(listOf("Work", "Duplicate"), activeSession.tags)
        assertEquals("A", activeSession.frequencyWeighting)
        checkNotNull(sessionById)
        assertFalse(sessionById.isActive)
        assertEquals(listOf(11L), recentSessions.map { it.id })
        assertEquals(listOf(11L, 10L), completedSessions.map { it.id })
        assertEquals(listOf(11L), sessionsInRange.map { it.id })
    }

    @Test
    fun completedSessionCountInRangeIgnoresActiveRows() = runTest {
        every { sessionDao.getSessionsInRange(100L, 200L) } returns
            flowOf(
                listOf(
                    session(id = 12L, isActive = false),
                    session(id = 13L, isActive = true),
                    session(id = 14L, isActive = false),
                ),
            )
        val repository = createRepository()

        assertEquals(2, repository.getCompletedSessionCountInRange(100L, 200L).first())
    }

    @Test
    fun updateSessionMetadataNormalizesValuesBeforePersisting() = runTest {
        val repository = createRepository()

        repository.updateSessionMetadata(
            id = 15L,
            name = "  Morning calibration  ",
            emoji = "  *  ",
            tags = listOf(" Office ", "office", " Long,Name "),
        )

        coVerify(exactly = 1) {
            sessionDao.updateSessionMetadata(
                id = 15L,
                name = "Morning calibration",
                emoji = "*",
                tags = "Office,Long Name",
            )
        }
    }

    private fun createRepository(): SessionRepository = SessionRepository(
        database = database,
        sessionDao = sessionDao,
        measurementDao = measurementDao,
        preferencesDataStore = preferencesDataStore,
    )

    private fun session(id: Long, isActive: Boolean = false, tags: String? = null): SessionEntity = SessionEntity(
        id = id,
        startTime = 1_700_000_000_000L,
        endTime = 1_700_000_060_000L,
        avgDb = 72f,
        tags = tags,
        isActive = isActive,
        frequencyWeighting = "A",
    )
}
