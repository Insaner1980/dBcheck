package com.dbcheck.app.service

import com.dbcheck.app.data.repository.PassiveMonitoringRepository
import com.dbcheck.app.data.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.Executors

class HistoryClearServiceTest {
    private val sessionRepository = mockk<SessionRepository>()
    private val passiveMonitoringRepository = mockk<PassiveMonitoringRepository>()
    private val wavRecordingFileStore = mockk<WavRecordingFileStore>()

    @Test
    fun clearHistoryDeletesInactiveDatabaseRowsAndAssociatedWavRecordings() = runTest {
        coEvery { sessionRepository.clearInactiveHistory() } returns listOf(7L, 8L)
        coEvery { passiveMonitoringRepository.clearAllSamples() } returns Unit
        every { wavRecordingFileStore.deleteRecordingForSession(any()) } returns true
        val service = createService()

        val result = service.clearHistory()

        assertEquals(2, result.deletedSessionCount)
        coVerify(exactly = 1) { sessionRepository.clearInactiveHistory() }
        coVerify(exactly = 1) { passiveMonitoringRepository.clearAllSamples() }
        verify(exactly = 1) { wavRecordingFileStore.deleteRecordingForSession(7L) }
        verify(exactly = 1) { wavRecordingFileStore.deleteRecordingForSession(8L) }
    }

    @Test
    fun clearHistoryDeletesWavRecordingsOnIoDispatcher() = runTest {
        coEvery { sessionRepository.clearInactiveHistory() } returns listOf(7L)
        coEvery { passiveMonitoringRepository.clearAllSamples() } returns Unit
        var deleteThreadName: String? = null
        every { wavRecordingFileStore.deleteRecordingForSession(7L) } answers {
            deleteThreadName = Thread.currentThread().name
            true
        }
        val executor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "history-clear-io") }
        val dispatcher = executor.asCoroutineDispatcher()
        val service = createService(ioDispatcher = dispatcher)

        try {
            service.clearHistory()

            assertEquals("history-clear-io", deleteThreadName)
        } finally {
            dispatcher.close()
            executor.shutdown()
        }
    }

    private fun createService(
        ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.Unconfined,
    ): HistoryClearService = HistoryClearService(
        sessionRepository,
        passiveMonitoringRepository,
        wavRecordingFileStore,
        ioDispatcher,
    )
}
