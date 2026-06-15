package com.dbcheck.app.data.repository

import com.dbcheck.app.data.local.db.dao.SoundDetectionEventDao
import com.dbcheck.app.data.local.db.entity.SoundDetectionEventEntity
import com.dbcheck.app.domain.audio.SoundDetectionEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SoundDetectionRepositoryTest {
    private val soundDetectionEventDao = mockk<SoundDetectionEventDao>()
    private val repository = SoundDetectionRepository(soundDetectionEventDao)

    @Test
    fun recordEventMapsDomainEventToEntity() = runTest {
        val insertedEvent = slot<SoundDetectionEventEntity>()
        coEvery { soundDetectionEventDao.insertEvent(capture(insertedEvent)) } returns Unit

        repository.recordEvent(
            SoundDetectionEvent(
                sessionId = 7L,
                timestamp = 1_700_000_001_000L,
                label = "Speech",
                confidence = 0.82f,
            ),
        )

        assertEquals(7L, insertedEvent.captured.sessionId)
        assertEquals(1_700_000_001_000L, insertedEvent.captured.timestamp)
        assertEquals("Speech", insertedEvent.captured.label)
        assertEquals(0.82f, insertedEvent.captured.confidence)
        coVerify(exactly = 1) { soundDetectionEventDao.insertEvent(any()) }
    }

    @Test
    fun deleteAllEventsDelegatesToDao() = runTest {
        coEvery { soundDetectionEventDao.deleteAllEvents() } returns Unit

        repository.deleteAllEvents()

        coVerify(exactly = 1) { soundDetectionEventDao.deleteAllEvents() }
    }
}
