package com.dbcheck.app.service

import com.dbcheck.app.data.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryClearServiceTest {
    private val sessionRepository = mockk<SessionRepository>()
    private val wavRecordingFileStore = mockk<WavRecordingFileStore>()
    private val service = HistoryClearService(sessionRepository, wavRecordingFileStore)

    @Test
    fun clearHistoryDeletesInactiveDatabaseRowsAndAssociatedWavRecordings() = runTest {
        coEvery { sessionRepository.clearInactiveHistory() } returns listOf(7L, 8L)
        every { wavRecordingFileStore.deleteRecordingForSession(any()) } returns true

        val result = service.clearHistory()

        assertEquals(2, result.deletedSessionCount)
        coVerify(exactly = 1) { sessionRepository.clearInactiveHistory() }
        verify(exactly = 1) { wavRecordingFileStore.deleteRecordingForSession(7L) }
        verify(exactly = 1) { wavRecordingFileStore.deleteRecordingForSession(8L) }
    }
}
