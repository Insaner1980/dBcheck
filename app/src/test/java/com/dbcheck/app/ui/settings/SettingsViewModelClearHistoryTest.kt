package com.dbcheck.app.ui.settings

import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.data.export.ExportCsvUseCase
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.service.ClearHistoryResult
import com.dbcheck.app.service.HistoryClearService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelClearHistoryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val harness = SettingsViewModelTestHarness(UserPreferences(isProUser = false))
    private val exportCsvUseCase = mockk<ExportCsvUseCase>(relaxed = true)
    private val backupGateway = TestBackupGateway()
    private val historyClearService = mockk<HistoryClearService>()

    @Test
    fun freeUserCanClearHistoryAfterSafetyConfirmation() = runTest {
        coEvery { historyClearService.clearHistory() } returns ClearHistoryResult(deletedSessionCount = 3)
        val viewModel = createViewModel()

        viewModel.requestClearHistory()
        assertTrue(viewModel.uiState.value.clearHistoryConfirmationVisible)

        viewModel.confirmClearHistory()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.clearHistoryConfirmationVisible)
        assertFalse(viewModel.uiState.value.isHistoryClearing)
        assertEquals("History cleared", viewModel.uiState.value.historyClearMessage)
        assertNull(viewModel.uiState.value.historyClearErrorMessage)
        coVerify(exactly = 1) { historyClearService.clearHistory() }
    }

    @Test
    fun clearHistoryIsBlockedWhileRecording() = runTest {
        harness.recordingFlow.value = true
        val viewModel = createViewModel()

        viewModel.requestClearHistory()

        assertFalse(viewModel.uiState.value.clearHistoryConfirmationVisible)
        assertEquals("Stop recording before clearing history", viewModel.uiState.value.historyClearErrorMessage)
        coVerify(exactly = 0) { historyClearService.clearHistory() }
    }

    private fun createViewModel(): SettingsViewModel = harness.createViewModel(
        exportCsvUseCase = exportCsvUseCase,
        backupGateway = backupGateway,
        historyClearService = historyClearService,
    )
}
