package com.dbcheck.app.ui.settings

import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.clearForTest
import com.dbcheck.app.data.export.ExportCsvUseCase
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.sync.BackupGateway
import com.dbcheck.app.sync.BackupResult
import com.dbcheck.app.sync.LocalBackup
import com.dbcheck.app.sync.RestoreResult
import com.dbcheck.app.ui.settings.state.LocalBackupUiState
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.io.File

class SettingsViewModelBackupTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val harness = SettingsViewModelTestHarness(UserPreferences(isProUser = false))
    private val billingGateway = TestBillingGateway()
    private val exportCsvUseCase = mockk<ExportCsvUseCase>()
    private val backupGateway = FakeBackupGateway()

    @Test
    fun initLoadsLocalBackupsIntoSettingsState() = runTest {
            val backup = localBackup("dbcheck_backup_20260509_120000.db")
            backupGateway.backups = listOf(backup)

            val viewModel = createViewModel()
            try {

                assertEquals(
                    listOf(backup.toUiState(displayName = "dBcheck_backup_20260509_120000.db")),
                    viewModel.uiState.value.localBackups,
                )
            } finally {
                viewModel.clearForTest()
            }
        }

    @Test
    fun createLocalBackupRefreshesListAndShowsSuccessMessage() = runTest {
            val created = localBackup("dBcheck_backup_20260509_120000.db")
            backupGateway.createResult = BackupResult.Created(created)
            val viewModel = createViewModel()
            try {

                viewModel.createLocalBackup()

                assertEquals(listOf(created.toUiState()), viewModel.uiState.value.localBackups)
                assertEquals("Backup created", viewModel.uiState.value.backupMessage)
                assertNull(viewModel.uiState.value.backupErrorMessage)
                assertFalse(viewModel.uiState.value.isBackupCreating)
                assertEquals(1, backupGateway.createCalls)
            } finally {
                viewModel.clearForTest()
            }
        }

    @Test
    fun createLocalBackupFailureShowsErrorAndClearsLoading() = runTest {
            backupGateway.createResult = BackupResult.Failed("Disk full")
            val viewModel = createViewModel()
            try {

                viewModel.createLocalBackup()

                assertEquals("Disk full", viewModel.uiState.value.backupErrorMessage)
                assertNull(viewModel.uiState.value.backupMessage)
                assertFalse(viewModel.uiState.value.isBackupCreating)
            } finally {
                viewModel.clearForTest()
            }
        }

    @Test
    fun restoreRequestOpensAndDismissesConfirmationCandidate() = runTest {
            val backup = localBackup("dbcheck_backup_20260509_120000.db")
            val backupUi = backup.toUiState()
            val viewModel = createViewModel()
            try {

                viewModel.requestRestoreBackup(backupUi)
                assertEquals(backupUi, viewModel.uiState.value.restoreCandidate)

                viewModel.dismissRestoreBackup()
                assertNull(viewModel.uiState.value.restoreCandidate)
            } finally {
                viewModel.clearForTest()
            }
        }

    @Test
    fun confirmRestoreBackupCallsRestartAfterSuccessfulRestoreWithoutEventCollector() = runTest {
            val backup = localBackup("dbcheck_backup_20260509_120000.db")
            val backupUi = backup.toUiState()
            val safety = localBackup("dbcheck_pre_restore_20260509_120100.db")
            backupGateway.restoreResult = RestoreResult.Restored(restoredBackup = backup, safetyBackup = safety)
            val viewModel = createViewModel()
            try {
                var restartCalls = 0
                viewModel.requestRestoreBackup(backupUi)

                viewModel.confirmRestoreBackup(onRestartAfterRestore = { restartCalls += 1 })

                assertEquals(1, restartCalls)
                assertNull(viewModel.uiState.value.restoreCandidate)
                assertFalse(viewModel.uiState.value.isBackupRestoring)
                assertEquals(1, backupGateway.restoreCalls)
            } finally {
                viewModel.clearForTest()
            }
        }

    @Test
    fun confirmRestoreRestartsAfterPostCloseFailure() = runTest {
        val backup = localBackup("dbcheck_backup_20260509_120000.db")
        backupGateway.restoreResult = RestoreResult.Failed("Restore failed", restartRequired = true)
        val viewModel = createViewModel()
        try {
            var restartCalls = 0
            viewModel.requestRestoreBackup(backup.toUiState())

            viewModel.confirmRestoreBackup(onRestartAfterRestore = { restartCalls += 1 })

            assertEquals(1, restartCalls)
            assertNull(viewModel.uiState.value.restoreCandidate)
            assertFalse(viewModel.uiState.value.isBackupRestoring)
            assertEquals("Restore failed", viewModel.uiState.value.backupErrorMessage)
            assertEquals(1, backupGateway.restoreCalls)
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun successfulRestoreRestartsEvenWhenViewModelIsClearedDuringDatabaseReplacement() = runTest {
        val backup = localBackup("dbcheck_backup.db")
        assertRestoreSurvivesViewModelClear(
            RestoreResult.Restored(restoredBackup = backup, safetyBackup = localBackup("dbcheck_safety.db")),
        )
    }

    @Test
    fun postCloseRestoreFailureRestartsEvenWhenViewModelIsClearedDuringDatabaseReplacement() = runTest {
        assertRestoreSurvivesViewModelClear(RestoreResult.Failed("Restore failed", restartRequired = true))
    }

    @Test
    fun passiveMonitoringBlocksBackupAndRestoreConfirmation() = runTest {
        val backup = localBackup("dbcheck_backup.db").toUiState()
        val viewModel = createViewModel()
        try {
            viewModel.requestRestoreBackup(backup)
            harness.passiveMonitoringFlow.value = true

            viewModel.confirmRestoreBackup()
            viewModel.createLocalBackup()
            viewModel.requestRestoreBackup(backup)

            assertEquals("Stop recording before managing backups", viewModel.uiState.value.backupErrorMessage)
            assertNull(viewModel.uiState.value.restoreCandidate)
            assertEquals(0, backupGateway.createCalls)
            assertEquals(0, backupGateway.restoreCalls)
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun activeRecordingBlocksCreateAndRestoreOperations() = runTest {
            harness.recordingFlow.value = true
            val backup = localBackup("dbcheck_backup_20260509_120000.db")
            val viewModel = createViewModel()
            try {

                viewModel.createLocalBackup()
                viewModel.requestRestoreBackup(backup.toUiState())

                assertEquals("Stop recording before managing backups", viewModel.uiState.value.backupErrorMessage)
                assertNull(viewModel.uiState.value.restoreCandidate)
                assertEquals(0, backupGateway.createCalls)
                assertEquals(0, backupGateway.restoreCalls)
            } finally {
                viewModel.clearForTest()
            }
        }

    private suspend fun assertRestoreSurvivesViewModelClear(result: RestoreResult) {
        val replacementStarted = CompletableDeferred<Unit>()
        val finishReplacement = CompletableDeferred<Unit>()
        val restarted = CompletableDeferred<Unit>()
        backupGateway.restoreResult = result
        backupGateway.beforeRestoreResult = {
            withContext(NonCancellable + Dispatchers.Default) {
                replacementStarted.complete(Unit)
                finishReplacement.await()
            }
        }
        val viewModel = createViewModel()
        viewModel.requestRestoreBackup(localBackup("dbcheck_backup.db").toUiState())
        viewModel.confirmRestoreBackup(onRestartAfterRestore = { restarted.complete(Unit) })
        replacementStarted.await()
        viewModel.clearForTest()
        finishReplacement.complete(Unit)

        withContext(Dispatchers.Default) {
            withTimeout(5_000L) { restarted.await() }
        }
        assertEquals(1, backupGateway.restoreCalls)
        assertFalse(viewModel.uiState.value.isBackupRestoring)
    }

    private fun createViewModel(): SettingsViewModel = harness.createViewModel(
        billingGateway = billingGateway,
        exportCsvUseCase = exportCsvUseCase,
        backupGateway = backupGateway,
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
    var beforeRestoreResult: suspend () -> Unit = {}

    override suspend fun listBackups(): List<LocalBackup> = backups

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
        beforeRestoreResult()
        return restoreResult
    }
}
