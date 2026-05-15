package com.dbcheck.app.sync

import java.io.File

data class LocalBackup(
    val file: File,
    val createdAtMillis: Long = file.lastModified(),
    val sizeBytes: Long = file.length(),
) {
    val fileName: String
        get() = file.name

    companion object {
        fun fromFile(file: File): LocalBackup =
            LocalBackup(
                file = file,
                createdAtMillis = file.lastModified(),
                sizeBytes = file.length(),
            )
    }
}

sealed interface BackupResult {
    data class Created(val backup: LocalBackup) : BackupResult

    data class Failed(val reason: String) : BackupResult
}

sealed interface RestoreResult {
    data class Restored(
        val restoredBackup: LocalBackup,
        val safetyBackup: LocalBackup,
    ) : RestoreResult

    data class Failed(val reason: String, val restartRequired: Boolean = false) : RestoreResult
}

interface BackupGateway {
    fun listBackups(): List<LocalBackup>

    suspend fun createLocalBackup(): BackupResult

    suspend fun restoreFromBackup(backup: LocalBackup): RestoreResult
}
