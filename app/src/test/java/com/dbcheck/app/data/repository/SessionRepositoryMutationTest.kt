package com.dbcheck.app.data.repository

import com.dbcheck.app.data.local.db.DbCheckDatabase
import com.dbcheck.app.data.local.db.DbCheckSchema
import com.dbcheck.app.data.local.db.dao.MeasurementDao
import com.dbcheck.app.data.local.db.dao.SessionDao
import com.dbcheck.app.data.local.db.entity.SessionEntity
import com.dbcheck.app.data.local.preferences.UserPreferencesDataStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRepositoryMutationTest {
    private val database = mockk<DbCheckDatabase>(relaxed = true)
    private val sessionDao = mockk<SessionDao>()
    private val measurementDao = mockk<MeasurementDao>(relaxed = true)
    private val preferencesDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
    private val repository =
        SessionRepository(
            database = database,
            sessionDao = sessionDao,
            measurementDao = measurementDao,
            preferencesDataStore = preferencesDataStore,
        )

    @Test
    fun createActiveSessionPersistsActiveEntityWithWeighting() = runTest {
        val insertedSession = slot<SessionEntity>()
        coEvery { sessionDao.insertSession(capture(insertedSession)) } returns SESSION_ID

        val sessionId = repository.createActiveSession(startTime = START_TIME, frequencyWeighting = "C")

        assertEquals(SESSION_ID, sessionId)
        assertEquals(START_TIME, insertedSession.captured.startTime)
        assertTrue(insertedSession.captured.isActive)
        assertEquals(DbCheckSchema.ACTIVE_SESSION_SLOT, insertedSession.captured.activeSlot)
        assertEquals("C", insertedSession.captured.frequencyWeighting)
        coVerify(exactly = 1) { sessionDao.insertSession(any()) }
    }

    private companion object {
        const val SESSION_ID = 42L
        const val START_TIME = 1_700_000_000_000L
    }
}
