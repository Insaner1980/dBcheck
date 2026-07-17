package com.dbcheck.app.data.repository

import com.dbcheck.app.data.local.db.DbCheckDatabase
import com.dbcheck.app.data.local.db.DbCheckSchema
import com.dbcheck.app.data.local.db.dao.MeasurementDao
import com.dbcheck.app.data.local.db.dao.SessionDao
import com.dbcheck.app.data.local.db.entity.SessionEntity
import com.dbcheck.app.data.local.preferences.UserPreferencesDataStore
import com.dbcheck.app.domain.session.SessionAudioInputDeviceMetadata
import com.dbcheck.app.domain.session.SessionLocationMetadata
import com.dbcheck.app.domain.session.SessionTimeZoneOffsetResolver
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

        val audioInputDevice =
            SessionAudioInputDeviceMetadata(
                selectedDeviceId = 12,
                selectedDeviceName = "USB-C microphone",
                routedDeviceName = "USB-C microphone",
            )
        val sessionId =
            repository.createActiveSession(
                startTime = START_TIME,
                frequencyWeighting = "C",
                audioInputDevice = audioInputDevice,
            )

        assertEquals(SESSION_ID, sessionId)
        assertEquals(START_TIME, insertedSession.captured.startTime)
        assertEquals(
            SessionTimeZoneOffsetResolver.offsetSecondsAt(START_TIME),
            insertedSession.captured.startUtcOffsetSeconds,
        )
        assertTrue(insertedSession.captured.isActive)
        assertEquals(DbCheckSchema.ACTIVE_SESSION_SLOT, insertedSession.captured.activeSlot)
        assertEquals("C", insertedSession.captured.frequencyWeighting)
        assertEquals(12, insertedSession.captured.selectedAudioInputDeviceId)
        assertEquals("USB-C microphone", insertedSession.captured.selectedAudioInputDeviceName)
        assertEquals("USB-C microphone", insertedSession.captured.routedAudioInputDeviceName)
        coVerify(exactly = 1) { sessionDao.insertSession(any()) }
    }

    @Test
    fun updateSessionLocationPersistsOptionalLocationMetadata() = runTest {
        val location =
            SessionLocationMetadata(
                latitude = 60.1699,
                longitude = 24.9384,
                accuracyMeters = 18.5f,
                capturedAt = START_TIME + 1_000L,
            )
        coEvery {
            sessionDao.updateSessionLocation(
                id = SESSION_ID,
                latitude = any(),
                longitude = any(),
                accuracyMeters = any(),
                capturedAt = any(),
            )
        } returns Unit

        repository.updateSessionLocation(id = SESSION_ID, location = location)

        coVerify(exactly = 1) {
            sessionDao.updateSessionLocation(
                id = SESSION_ID,
                latitude = 60.1699,
                longitude = 24.9384,
                accuracyMeters = 18.5f,
                capturedAt = START_TIME + 1_000L,
            )
        }
    }

    private companion object {
        const val SESSION_ID = 42L
        const val START_TIME = 1_700_000_000_000L
    }
}
