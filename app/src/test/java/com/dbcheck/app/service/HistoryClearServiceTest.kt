package com.dbcheck.app.service

import android.content.Context
import com.dbcheck.app.data.repository.PassiveMonitoringRepository
import com.dbcheck.app.data.repository.SessionRepository
import com.dbcheck.app.widget.DbCheckWidgetReceiver
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Executors

class HistoryClearServiceTest {
    private val context = mockk<Context>()
    private val sessionRepository = mockk<SessionRepository>()
    private val passiveMonitoringRepository = mockk<PassiveMonitoringRepository>()
    private val wavRecordingFileStore = mockk<WavRecordingFileStore>()

    @Before
    fun setUp() {
        mockkObject(DbCheckWidgetReceiver.Companion)
        coEvery { DbCheckWidgetReceiver.updateAllWidgets(context) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkObject(DbCheckWidgetReceiver.Companion)
    }

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
        coVerify(exactly = 1) { DbCheckWidgetReceiver.updateAllWidgets(context) }
    }

    @Test
    fun clearHistoryDoesNotRefreshWidgetsWhenNoSessionsWereDeleted() = runTest {
        coEvery { sessionRepository.clearInactiveHistory() } returns emptyList()
        coEvery { passiveMonitoringRepository.clearAllSamples() } returns Unit

        val result = createService().clearHistory()

        assertEquals(0, result.deletedSessionCount)
        coVerify(exactly = 1) { passiveMonitoringRepository.clearAllSamples() }
        coVerify(exactly = 0) { DbCheckWidgetReceiver.updateAllWidgets(any()) }
        verify(exactly = 0) { wavRecordingFileStore.deleteRecordingForSession(any()) }
    }

    @Test
    fun widgetRefreshFailureDoesNotPreventRemainingHistoryCleanup() = runTest {
        coEvery { sessionRepository.clearInactiveHistory() } returns listOf(7L)
        coEvery { passiveMonitoringRepository.clearAllSamples() } returns Unit
        every { wavRecordingFileStore.deleteRecordingForSession(7L) } returns true
        coEvery { DbCheckWidgetReceiver.updateAllWidgets(context) } throws IllegalStateException("Widget unavailable")

        val result = createService().clearHistory()

        assertEquals(1, result.deletedSessionCount)
        coVerify(exactly = 1) { DbCheckWidgetReceiver.updateAllWidgets(context) }
        coVerify(exactly = 1) { passiveMonitoringRepository.clearAllSamples() }
        verify(exactly = 1) { wavRecordingFileStore.deleteRecordingForSession(7L) }
    }

    @Test
    fun widgetRefreshCancellationPropagates() = runTest {
        val cancellation = CancellationException("Cancelled")
        coEvery { sessionRepository.clearInactiveHistory() } returns listOf(7L)
        coEvery { DbCheckWidgetReceiver.updateAllWidgets(context) } throws cancellation

        val error = runCatching { createService().clearHistory() }.exceptionOrNull()

        assertSame(cancellation, error)
        coVerify(exactly = 0) { passiveMonitoringRepository.clearAllSamples() }
        verify(exactly = 0) { wavRecordingFileStore.deleteRecordingForSession(any()) }
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
        context,
        sessionRepository,
        passiveMonitoringRepository,
        wavRecordingFileStore,
        ioDispatcher,
    )
}
