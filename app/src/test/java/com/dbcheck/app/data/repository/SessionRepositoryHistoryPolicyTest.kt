package com.dbcheck.app.data.repository

import com.dbcheck.app.data.local.db.DbCheckDatabase
import com.dbcheck.app.data.local.db.dao.MeasurementDao
import com.dbcheck.app.data.local.db.dao.SessionDao
import com.dbcheck.app.data.local.db.entity.SessionEntity
import com.dbcheck.app.data.local.preferences.UserPreferencesDataStore
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

    private fun createRepository(): SessionRepository = SessionRepository(
        database = database,
        sessionDao = sessionDao,
        measurementDao = measurementDao,
        preferencesDataStore = preferencesDataStore,
    )

    private fun session(id: Long): SessionEntity = SessionEntity(
        id = id,
        startTime = 1_700_000_000_000L,
        endTime = 1_700_000_060_000L,
        avgDb = 72f,
        frequencyWeighting = "A",
    )
}
