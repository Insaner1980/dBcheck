package com.dbcheck.app.ui.settings

import com.dbcheck.app.sync.BackupGateway
import com.dbcheck.app.sync.BackupResult
import com.dbcheck.app.sync.LocalBackup
import com.dbcheck.app.sync.RestoreResult

internal class SettingsBackupGatewayTestFake : BackupGateway {
    override fun listBackups(): List<LocalBackup> = emptyList()

    override suspend fun createLocalBackup(): BackupResult = BackupResult.Failed("Not configured")

    override suspend fun restoreFromBackup(backup: LocalBackup): RestoreResult = RestoreResult.Failed("Not configured")
}
