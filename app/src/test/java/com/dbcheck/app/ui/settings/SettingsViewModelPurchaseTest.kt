package com.dbcheck.app.ui.settings

import android.app.Activity
import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.billing.PurchaseEvent
import com.dbcheck.app.billing.PurchaseLaunchResult
import com.dbcheck.app.data.export.ExportCsvUseCase
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.service.AudioSessionManager
import com.dbcheck.app.service.BackupService
import com.dbcheck.app.service.HealthConnectService
import com.dbcheck.app.service.HistoryClearService
import com.dbcheck.app.sync.HealthConnectManager
import com.dbcheck.app.sync.HealthConnectStatus
import com.dbcheck.app.testStringContext
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class SettingsViewModelPurchaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferencesFlow = MutableStateFlow(UserPreferences(isProUser = false))
    private val preferencesRepository =
        mockk<PreferencesRepository> {
            every { userPreferences } returns preferencesFlow
            coEvery { updateDebugForceFreeEnabled(any()) } just runs
        }
    private val healthConnectManager =
        mockk<HealthConnectManager> {
            coEvery { getStatus() } returns HealthConnectStatus()
        }
    private val billingGateway = TestBillingGateway()
    private val activity = mockk<Activity>()
    private val backupGateway = TestBackupGateway()
    private val audioSessionManager =
        mockk<AudioSessionManager> {
            every { isRecording } returns MutableStateFlow(false)
        }

    @Test
    fun launchStartedClearsLoadingWithoutError() = runTest {
            billingGateway.launchResult = PurchaseLaunchResult.Started
            val viewModel = createViewModel()

            viewModel.launchProPurchase(activity)

            assertFalse(viewModel.uiState.value.isPurchaseLaunching)
            assertNull(viewModel.uiState.value.purchaseErrorMessage)
        }

    @Test
    fun unavailableLaunchShowsError() = runTest {
            billingGateway.launchResult = PurchaseLaunchResult.Unavailable("Product is not available")
            val viewModel = createViewModel()

            viewModel.launchProPurchase(activity)

            assertFalse(viewModel.uiState.value.isPurchaseLaunching)
            assertEquals("Product is not available", viewModel.uiState.value.purchaseErrorMessage)
        }

    @Test
    fun launchPurchaseExceptionShowsErrorAndClearsLoading() = runTest {
            billingGateway.launchFailure = IllegalStateException("billing disconnected")
            val viewModel = createViewModel()

            viewModel.launchProPurchase(activity)

            assertFalse(viewModel.uiState.value.isPurchaseLaunching)
            assertEquals("Unable to start purchase", viewModel.uiState.value.purchaseErrorMessage)
        }

    @Test
    fun completedPurchaseShowsSuccessMessage() = runTest {
            val viewModel = createViewModel()

            billingGateway.events.emit(PurchaseEvent.Completed)

            assertFalse(viewModel.uiState.value.isPurchaseLaunching)
            assertEquals("dBcheck Pro unlocked", viewModel.uiState.value.purchaseMessage)
            assertNull(viewModel.uiState.value.purchaseErrorMessage)
        }

    @Test
    fun cancelledPurchaseClearsLoadingWithoutPersistentError() = runTest {
            val viewModel = createViewModel()

            billingGateway.events.emit(PurchaseEvent.Cancelled)

            assertFalse(viewModel.uiState.value.isPurchaseLaunching)
            assertNull(viewModel.uiState.value.purchaseErrorMessage)
        }

    @Test
    fun cancelledPurchaseClearsPreviousPendingMessage() = runTest {
            val viewModel = createViewModel()

            billingGateway.events.emit(PurchaseEvent.Pending)
            billingGateway.events.emit(PurchaseEvent.Cancelled)

            assertNull(viewModel.uiState.value.purchaseMessage)
            assertNull(viewModel.uiState.value.purchaseErrorMessage)
        }

    @Test
    fun pendingPurchaseShowsPendingMessageWithoutUnlockingError() = runTest {
            val viewModel = createViewModel()

            billingGateway.events.emit(PurchaseEvent.Pending)

            assertFalse(viewModel.uiState.value.isPurchaseLaunching)
            assertEquals(
                "Purchase pending. Complete payment in Google Play to unlock dBcheck Pro",
                viewModel.uiState.value.purchaseMessage,
            )
            assertNull(viewModel.uiState.value.purchaseErrorMessage)
        }

    @Test
    fun failedPurchaseClearsPreviousPurchaseMessage() = runTest {
            val viewModel = createViewModel()

            billingGateway.events.emit(PurchaseEvent.Pending)
            billingGateway.events.emit(PurchaseEvent.Failed("Purchase failed"))

            assertNull(viewModel.uiState.value.purchaseMessage)
            assertEquals("Purchase failed", viewModel.uiState.value.purchaseErrorMessage)
        }

    @Test
    fun debugForceFreeUpdatePersistsPreference() = runTest {
            val viewModel = createViewModel()

            viewModel.updateDebugForceFree(true)

            coVerify { preferencesRepository.updateDebugForceFreeEnabled(true) }
        }

    private fun createViewModel(): SettingsViewModel = SettingsViewModel(
            context = testStringContext(),
            preferencesRepository = preferencesRepository,
            calibrationProfileRepository = testCalibrationProfileRepository(),
            healthConnectService = HealthConnectService(healthConnectManager),
            billingGateway = billingGateway,
            exportCsvUseCase = mockk<ExportCsvUseCase>(),
            backupService = BackupService(backupGateway),
            audioSessionManager = audioSessionManager,
            historyClearService = mockk<HistoryClearService>(relaxed = true),
        )
}
