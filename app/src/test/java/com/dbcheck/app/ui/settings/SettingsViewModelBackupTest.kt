package com.dbcheck.app.ui.settings

import app.cash.turbine.test
import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.billing.BillingGateway
import com.dbcheck.app.billing.PurchaseEvent
import com.dbcheck.app.billing.PurchaseLaunchResult
import com.dbcheck.app.data.export.ExportCsvUseCase
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.service.AudioSessionManager
import com.dbcheck.app.service.BackupService
import com.dbcheck.app.service.HealthConnectService
import com.dbcheck.app.sync.BackupGateway
import com.dbcheck.app.sync.BackupResult
import com.dbcheck.app.sync.HealthConnectManager
import com.dbcheck.app.sync.HealthConnectStatus
import com.dbcheck.app.sync.LocalBackup
import com.dbcheck.app.sync.RestoreResult
import com.dbcheck.app.testStringContext
import com.dbcheck.app.ui.settings.state.LocalBackupUiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.io.File

class SettingsViewModelBackupTest {
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
    private val billingGateway = BackupFakeBillingGateway()
    private val exportCsvUseCase = mockk<ExportCsvUseCase>()
    private val backupGateway = FakeBackupGateway()
    private val recordingFlow = MutableStateFlow(false)
    private val audioSessionManager =
        mockk<AudioSessionManager> {
            every { isRecording } returns recordingFlow
        }

    @Test
    fun initLoadsLocalBackupsIntoSettingsState() = runTest {
            val backup = localBackup("dbcheck_backup_20260509_120000.db")
            backupGateway.backups = listOf(backup)

            val viewModel = createViewModel()

            assertEquals(
                listOf(backup.toUiState(displayName = "dBcheck_backup_20260509_120000.db")),
                viewModel.uiState.value.localBackups,
            )
        }

    @Test
    fun createLocalBackupRefreshesListAndShowsSuccessMessage() = runTest {
            val created = localBackup("dBcheck_backup_20260509_120000.db")
            backupGateway.createResult = BackupResult.Created(created)
            val viewModel = createViewModel()

            viewModel.createLocalBackup()

            assertEquals(listOf(created.toUiState()), viewModel.uiState.value.localBackups)
            assertEquals("Backup created", viewModel.uiState.value.backupMessage)
            assertNull(viewModel.uiState.value.backupErrorMessage)
            assertFalse(viewModel.uiState.value.isBackupCreating)
            assertEquals(1, backupGateway.createCalls)
        }

    @Test
    fun createLocalBackupFailureShowsErrorAndClearsLoading() = runTest {
            backupGateway.createResult = BackupResult.Failed("Disk full")
            val viewModel = createViewModel()

            viewModel.createLocalBackup()

            assertEquals("Disk full", viewModel.uiState.value.backupErrorMessage)
            assertNull(viewModel.uiState.value.backupMessage)
            assertFalse(viewModel.uiState.value.isBackupCreating)
        }

    @Test
    fun restoreRequestOpensAndDismissesConfirmationCandidate() = runTest {
            val backup = localBackup("dbcheck_backup_20260509_120000.db")
            val backupUi = backup.toUiState()
            val viewModel = createViewModel()

            viewModel.requestRestoreBackup(backupUi)
            assertEquals(backupUi, viewModel.uiState.value.restoreCandidate)

            viewModel.dismissRestoreBackup()
            assertNull(viewModel.uiState.value.restoreCandidate)
        }

    @Test
    fun confirmRestoreBackupEmitsRestartEventAfterSuccessfulRestore() = runTest {
            val backup = localBackup("dbcheck_backup_20260509_120000.db")
            val backupUi = backup.toUiState()
            val safety = localBackup("dbcheck_pre_restore_20260509_120100.db")
            backupGateway.restoreResult = RestoreResult.Restored(restoredBackup = backup, safetyBackup = safety)
            val viewModel = createViewModel()
            viewModel.requestRestoreBackup(backupUi)

            viewModel.events.test {
                viewModel.confirmRestoreBackup()

                assertEquals(SettingsEvent.RestartAfterRestore, awaitItem())
            }
            assertNull(viewModel.uiState.value.restoreCandidate)
            assertFalse(viewModel.uiState.value.isBackupRestoring)
            assertEquals(1, backupGateway.restoreCalls)
        }

    @Test
    fun confirmRestoreRestartsAfterPostCloseFailure() = runTest {
        val backup = localBackup("dbcheck_backup_20260509_120000.db")
        backupGateway.restoreResult = RestoreResult.Failed("Restore failed", restartRequired = true)
        val viewModel = createViewModel()
        viewModel.requestRestoreBackup(backup.toUiState())

        viewModel.events.test {
            viewModel.confirmRestoreBackup()

            assertEquals(SettingsEvent.RestartAfterRestore, awaitItem())
        }
        assertNull(viewModel.uiState.value.restoreCandidate)
        assertFalse(viewModel.uiState.value.isBackupRestoring)
        assertEquals("Restore failed", viewModel.uiState.value.backupErrorMessage)
        assertEquals(1, backupGateway.restoreCalls)
    }

    @Test
    fun activeRecordingBlocksCreateAndRestoreOperations() = runTest {
            recordingFlow.value = true
            val backup = localBackup("dbcheck_backup_20260509_120000.db")
            val viewModel = createViewModel()

            viewModel.createLocalBackup()
            viewModel.requestRestoreBackup(backup.toUiState())

            assertEquals("Stop recording before managing backups", viewModel.uiState.value.backupErrorMessage)
            assertNull(viewModel.uiState.value.restoreCandidate)
            assertEquals(0, backupGateway.createCalls)
            assertEquals(0, backupGateway.restoreCalls)
        }

    private fun createViewModel(): SettingsViewModel = SettingsViewModel(
            context = testStringContext(),
            preferencesRepository = preferencesRepository,
            healthConnectService = HealthConnectService(healthConnectManager),
            billingGateway = billingGateway,
            exportCsvUseCase = exportCsvUseCase,
            backupService = BackupService(backupGateway),
            audioSessionManager = audioSessionManager,
        )

    private fun localBackup(fileName: String): LocalBackup {
        val file = File(fileName)
        return LocalBackup(file = file, createdAtMillis = 1_714_000_000_000L, sizeBytes = 2048L)
    }

    private fun LocalBackup.toUiState(displayName: String = fileName): LocalBackupUiState = LocalBackupUiState(
            filePath = file.absolutePath,
            fileName = fileName,
            displayName = displayName,
            createdAtMillis = createdAtMillis,
            sizeBytes = sizeBytes,
        )
}

private class FakeBackupGateway : BackupGateway {
    var backups: List<LocalBackup> = emptyList()
    var createResult: BackupResult = BackupResult.Failed("Not configured")
    var restoreResult: RestoreResult = RestoreResult.Failed("Not configured")
    var createCalls = 0
    var restoreCalls = 0

    override fun listBackups(): List<LocalBackup> = backups

    override suspend fun createLocalBackup(): BackupResult {
        createCalls += 1
        val result = createResult
        if (result is BackupResult.Created) {
            backups = listOf(result.backup) + backups
        }
        return result
    }

    override suspend fun restoreFromBackup(backup: LocalBackup): RestoreResult {
        restoreCalls += 1
        return restoreResult
    }
}

private class BackupFakeBillingGateway : BillingGateway {
    override val purchaseEvents = MutableSharedFlow<PurchaseEvent>()

    override suspend fun launchPurchaseFlow(activity: android.app.Activity): PurchaseLaunchResult =
        PurchaseLaunchResult.Started
}
