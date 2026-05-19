package com.dbcheck.app.service

import com.dbcheck.app.sync.BackupGateway
import com.dbcheck.app.sync.BackupResult
import com.dbcheck.app.sync.LocalBackup
import com.dbcheck.app.sync.RestoreResult
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class LocalBackupInfo(val filePath: String, val fileName: String, val createdAtMillis: Long, val sizeBytes: Long)

sealed interface LocalBackupResult {
    data class Created(val backup: LocalBackupInfo) : LocalBackupResult

    data class Failed(val reason: String) : LocalBackupResult
}

sealed interface LocalRestoreResult {
    data class Restored(val restoredBackup: LocalBackupInfo, val safetyBackup: LocalBackupInfo) : LocalRestoreResult

    data class Failed(val reason: String, val restartRequired: Boolean = false) : LocalRestoreResult
}

@Singleton
class BackupService
    @Inject
    constructor(private val backupGateway: BackupGateway) {
        fun listBackups(): List<LocalBackupInfo> = backupGateway.listBackups().map { it.toInfo() }

        suspend fun createLocalBackup(): LocalBackupResult = when (val result = backupGateway.createLocalBackup()) {
                is BackupResult.Created -> LocalBackupResult.Created(result.backup.toInfo())
                is BackupResult.Failed -> LocalBackupResult.Failed(result.reason)
            }

        suspend fun restoreFromBackup(backup: LocalBackupInfo): LocalRestoreResult =
            when (val result = backupGateway.restoreFromBackup(backup.toLocalBackup())) {
                is RestoreResult.Restored ->
                    LocalRestoreResult.Restored(
                        restoredBackup = result.restoredBackup.toInfo(),
                        safetyBackup = result.safetyBackup.toInfo(),
                    )

                is RestoreResult.Failed ->
                    LocalRestoreResult.Failed(
                        reason = result.reason,
                        restartRequired = result.restartRequired,
                    )
            }
    }

private fun LocalBackup.toInfo(): LocalBackupInfo = LocalBackupInfo(
        filePath = file.absolutePath,
        fileName = fileName,
        createdAtMillis = createdAtMillis,
        sizeBytes = sizeBytes,
    )

private fun LocalBackupInfo.toLocalBackup(): LocalBackup = LocalBackup.fromFile(File(filePath))
