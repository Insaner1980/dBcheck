package com.dbcheck.app.ui.settings

import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.data.export.ExportCsvUseCase
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.service.AudioSessionManager
import com.dbcheck.app.service.BackupService
import com.dbcheck.app.service.ClearHistoryResult
import com.dbcheck.app.service.HealthConnectService
import com.dbcheck.app.service.HistoryClearService
import com.dbcheck.app.sync.HealthConnectManager
import com.dbcheck.app.sync.HealthConnectStatus
import com.dbcheck.app.testStringContext
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val preferencesFlow = MutableStateFlow(UserPreferences(isProUser = false))
    private val preferencesRepository =
        mockk<PreferencesRepository> {
            every { userPreferences } returns preferencesFlow
        }
    private val healthConnectManager =
        mockk<HealthConnectManager> {
            coEvery { getStatus() } returns HealthConnectStatus()
        }
    private val exportCsvUseCase = mockk<ExportCsvUseCase>(relaxed = true)
    private val backupGateway = TestBackupGateway()
    private val isRecording = MutableStateFlow(false)
    private val audioSessionManager =
        mockk<AudioSessionManager> {
            every { isRecording } returns this@SettingsViewModelClearHistoryTest.isRecording
        }
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
        isRecording.value = true
        val viewModel = createViewModel()

        viewModel.requestClearHistory()

        assertFalse(viewModel.uiState.value.clearHistoryConfirmationVisible)
        assertEquals("Stop recording before clearing history", viewModel.uiState.value.historyClearErrorMessage)
        coVerify(exactly = 0) { historyClearService.clearHistory() }
    }

    private fun createViewModel(): SettingsViewModel = SettingsViewModel(
        context = testStringContext(),
        preferencesRepository = preferencesRepository,
        calibrationProfileRepository = testCalibrationProfileRepository(),
        healthConnectService = HealthConnectService(healthConnectManager),
        billingGateway = TestBillingGateway(),
        exportCsvUseCase = exportCsvUseCase,
        backupService = BackupService(backupGateway),
        audioSessionManager = audioSessionManager,
        historyClearService = historyClearService,
    )
}
