package com.dbcheck.app.ui.settings

import android.content.Intent
import app.cash.turbine.test
import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.clearForTest
import com.dbcheck.app.billing.PurchaseEvent
import com.dbcheck.app.data.export.ExportCsvUseCase
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelCsvExportTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val harness = SettingsViewModelTestHarness(UserPreferences(isProUser = true))
    private val preferencesFlow = harness.preferencesFlow
    private val billingGateway = TestBillingGateway()
    private val exportCsvUseCase = mockk<ExportCsvUseCase>()
    private val backupGateway = TestBackupGateway()

    @Test
    fun proUserCanCreateCsvExportIntent() = runTest {
            val csvIntent = Intent(Intent.ACTION_SEND_MULTIPLE)
            coEvery { exportCsvUseCase.export() } returns csvIntent
            val viewModel = createViewModel()
            try {

                viewModel.csvExportIntents.test {
                    viewModel.createCsvExportIntent()
                    assertSame(csvIntent, awaitItem())
                }
                assertFalse(viewModel.uiState.value.isCsvExporting)
                assertNull(viewModel.uiState.value.csvExportErrorMessage)
                coVerify(exactly = 1) { exportCsvUseCase.export() }
            } finally {
                viewModel.clearForTest()
            }
        }

    @Test
    fun freeUserCannotCreateCsvExportIntent() = runTest {
            preferencesFlow.value = UserPreferences(isProUser = false)
            val viewModel = createViewModel()
            try {

                viewModel.csvExportIntents.test {
                    viewModel.createCsvExportIntent()
                    expectNoEvents()
                }
                assertEquals("CSV export requires dBcheck Pro", viewModel.uiState.value.csvExportErrorMessage)
                coVerify(exactly = 0) { exportCsvUseCase.export() }
            } finally {
                viewModel.clearForTest()
            }
        }

    @Test
    fun csvExportFailureShowsErrorAndClearsLoading() = runTest {
            coEvery { exportCsvUseCase.export() } throws IllegalStateException("Disk full")
            val viewModel = createViewModel()
            try {

                viewModel.csvExportIntents.test {
                    viewModel.createCsvExportIntent()
                    runCurrent()
                    expectNoEvents()
                }
                assertFalse(viewModel.uiState.value.isCsvExporting)
                assertEquals("CSV export failed", viewModel.uiState.value.csvExportErrorMessage)
            } finally {
                viewModel.clearForTest()
            }
        }

    @Test
    fun csvExportCancellationIsNotShownAsFailure() = runTest {
            coEvery { exportCsvUseCase.export() } throws CancellationException("Export cancelled")
            val viewModel = createViewModel()
            try {

                viewModel.createCsvExportIntent()
                runCurrent()

                assertNull(viewModel.uiState.value.csvExportErrorMessage)
            } finally {
                viewModel.clearForTest()
            }
        }

    @Test
    fun csvShareStartedShowsSuccessAndClearsCsvError() = runTest {
            preferencesFlow.value = UserPreferences(isProUser = false)
            val viewModel = createViewModel()
            try {
                viewModel.createCsvExportIntent()

                viewModel.onCsvShareStarted()

                assertEquals("CSV export ready", viewModel.uiState.value.csvExportMessage)
                assertNull(viewModel.uiState.value.csvExportErrorMessage)
            } finally {
                viewModel.clearForTest()
            }
        }

    @Test
    fun clearCsvExportMessagesKeepsPurchaseMessages() = runTest {
            val viewModel = createViewModel()
            try {
                billingGateway.events.emit(PurchaseEvent.Completed)
                viewModel.onCsvShareUnavailable()

                viewModel.clearCsvExportMessages()

                assertNull(viewModel.uiState.value.csvExportMessage)
                assertNull(viewModel.uiState.value.csvExportErrorMessage)
                assertEquals("dBcheck Pro unlocked", viewModel.uiState.value.purchaseMessage)
            } finally {
                viewModel.clearForTest()
            }
        }

    private fun createViewModel(): SettingsViewModel = harness.createViewModel(
        billingGateway = billingGateway,
        exportCsvUseCase = exportCsvUseCase,
        backupGateway = backupGateway,
    )
}
