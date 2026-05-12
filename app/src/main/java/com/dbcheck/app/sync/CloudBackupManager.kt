package com.dbcheck.app.sync

import android.content.Context
import androidx.sqlite.db.SimpleSQLiteQuery
import com.dbcheck.app.data.local.db.DbCheckDatabase
import com.dbcheck.app.di.IoDispatcher
import com.dbcheck.app.util.toUserFacingMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudBackupManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val database: DbCheckDatabase,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : BackupGateway {
        companion object {
            private const val DATABASE_NAME = "dbcheck.db"
            private const val BACKUP_DIR = "backups"
            private const val BACKUP_PREFIX = "dbcheck_backup"
            private const val PRE_RESTORE_PREFIX = "dbcheck_pre_restore"
            private const val WAL_CHECKPOINT_QUERY = "PRAGMA wal_checkpoint(FULL)"
        }

        override fun listBackups(): List<LocalBackup> {
            val backupDir = backupDirectory()
            return backupDir
                .listFiles()
                ?.filter { it.isFile && it.extension.equals("db", ignoreCase = true) }
                ?.sortedByDescending { it.lastModified() }
                ?.map(LocalBackup::fromFile)
                ?: emptyList()
        }

        override suspend fun createLocalBackup(): BackupResult =
            withContext(ioDispatcher) {
                runCatching {
                    checkpointDatabase()

                    val dbFile = databaseFile()
                    check(dbFile.isFile) { "Database file not found" }

                    val backupFile = createBackupFile(BACKUP_PREFIX)
                    dbFile.copyTo(backupFile, overwrite = false)
                    LocalBackup.fromFile(backupFile)
                }.fold(
                    onSuccess = BackupResult::Created,
                    onFailure = { error -> BackupResult.Failed(error.toUserFacingMessage("Backup failed")) },
                )
            }

        override suspend fun restoreFromBackup(backup: LocalBackup): RestoreResult =
            withContext(ioDispatcher) {
                val validatedBackup = validateBackupFile(backup.file)
                if (validatedBackup is InvalidBackup) {
                    return@withContext RestoreResult.Failed(validatedBackup.reason)
                }

                val backupFile = (validatedBackup as ValidBackup).file
                runCatching {
                    checkpointDatabase()

                    val dbFile = databaseFile()
                    check(dbFile.isFile) { "Database file not found" }

                    val safetyBackupFile = createBackupFile(PRE_RESTORE_PREFIX)
                    dbFile.copyTo(safetyBackupFile, overwrite = false)

                    database.close()
                    deleteDatabaseSidecar(dbFile, "-wal")
                    deleteDatabaseSidecar(dbFile, "-shm")
                    backupFile.copyTo(dbFile, overwrite = true)

                    RestoreResult.Restored(
                        restoredBackup = LocalBackup.fromFile(backupFile),
                        safetyBackup = LocalBackup.fromFile(safetyBackupFile),
                    )
                }.getOrElse { error ->
                    RestoreResult.Failed(error.toUserFacingMessage("Restore failed"))
                }
            }

        private fun checkpointDatabase() {
            val cursor = database.query(SimpleSQLiteQuery(WAL_CHECKPOINT_QUERY))
            cursor.close()
        }

        private fun validateBackupFile(file: File): BackupValidation {
            val canonicalFile = file.canonicalFile
            val canonicalBackupDir = backupDirectory().canonicalFile

            return when {
                !canonicalFile.extension.equals("db", ignoreCase = true) ->
                    InvalidBackup("Backup file is not a dBcheck database backup")

                canonicalFile.parentFile?.canonicalFile != canonicalBackupDir ->
                    InvalidBackup("Backup file is not managed by dBcheck")

                !canonicalFile.isFile ->
                    InvalidBackup("Backup file not found")

                else -> ValidBackup(canonicalFile)
            }
        }

        private fun backupDirectory(): File =
            File(context.filesDir, BACKUP_DIR).apply { mkdirs() }

        private fun databaseFile(): File = context.getDatabasePath(DATABASE_NAME)

        private fun createBackupFile(prefix: String): File {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val backupDir = backupDirectory()
            val baseName = "${prefix}_$timestamp"
            var backupFile = File(backupDir, "$baseName.db")
            var counter = 1

            while (backupFile.exists()) {
                backupFile = File(backupDir, "${baseName}_$counter.db")
                counter += 1
            }

            return backupFile
        }

        private fun deleteDatabaseSidecar(
            dbFile: File,
            suffix: String,
        ) {
            val sidecar = File(dbFile.path + suffix)
            check(!sidecar.exists() || sidecar.delete()) {
                "Unable to replace database sidecar ${sidecar.name}"
            }
        }
    }

private sealed interface BackupValidation

private data class ValidBackup(val file: File) : BackupValidation

private data class InvalidBackup(val reason: String) : BackupValidation
