package com.dbcheck.app.sync

import android.content.Context
import android.database.Cursor
import androidx.sqlite.db.SimpleSQLiteQuery
import com.dbcheck.app.data.local.db.DbCheckDatabase
import com.dbcheck.app.di.IoDispatcher
import com.dbcheck.app.util.toUserFacingMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalBackupManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val database: DbCheckDatabase,
        private val backupDatabaseValidator: BackupDatabaseValidator,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : BackupGateway {
        private val backupOperationMutex = Mutex()

        companion object {
            private const val DATABASE_NAME = "dbcheck.db"
            private const val BACKUP_DIR = "backups"
            private const val BACKUP_PREFIX = "dbcheck_backup"
            private const val PRE_RESTORE_PREFIX = "dbcheck_pre_restore"
            private const val WAL_CHECKPOINT_QUERY = "PRAGMA wal_checkpoint(TRUNCATE)"
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
                backupOperationMutex.withLock {
                    runCatching {
                        checkpointDatabase()

                        val dbFile = databaseFile()
                        check(dbFile.isFile) { "Database file not found" }

                        val backupFile = createBackupFile(BACKUP_PREFIX)
                        val tempBackupFile = createTempFile(backupFile.parentFile, backupFile.name)
                        copyFileDurably(dbFile, tempBackupFile)
                        check(tempBackupFile.hasValidDbCheckDatabase()) { "Backup validation failed" }
                        moveReplacing(tempBackupFile, backupFile)
                        LocalBackup.fromFile(backupFile)
                    }.fold(
                        onSuccess = BackupResult::Created,
                        onFailure = { error -> BackupResult.Failed(error.toUserFacingMessage("Backup failed")) },
                    )
                }
            }

        override suspend fun restoreFromBackup(backup: LocalBackup): RestoreResult =
            withContext(ioDispatcher) {
                backupOperationMutex.withLock {
                    val validatedBackup = validateBackupFile(backup.file)
                    if (validatedBackup is InvalidBackup) {
                        return@withLock RestoreResult.Failed(validatedBackup.reason)
                    }

                    val backupFile = (validatedBackup as ValidBackup).file
                    var databaseClosed = false
                    runCatching {
                        checkpointDatabase()

                        val dbFile = databaseFile()
                        check(dbFile.isFile) { "Database file not found" }

                        val safetyBackupFile = createBackupFile(PRE_RESTORE_PREFIX)
                        val tempSafetyBackupFile = createTempFile(safetyBackupFile.parentFile, safetyBackupFile.name)
                        copyFileDurably(dbFile, tempSafetyBackupFile)
                        check(tempSafetyBackupFile.hasValidDbCheckDatabase()) { "Safety backup validation failed" }
                        moveReplacing(tempSafetyBackupFile, safetyBackupFile)

                        val stagedRestoreFile = createTempFile(dbFile.parentFile, dbFile.name)
                        copyFileDurably(backupFile, stagedRestoreFile)
                        check(stagedRestoreFile.hasValidDbCheckDatabase()) { "Restore staging validation failed" }

                        database.close()
                        databaseClosed = true
                        runCatching {
                            deleteDatabaseSidecar(dbFile, "-wal")
                            deleteDatabaseSidecar(dbFile, "-shm")
                            moveReplacing(stagedRestoreFile, dbFile)
                        }.onFailure {
                            rollbackDatabaseFile(safetyBackupFile, dbFile)
                        }.getOrThrow()

                        RestoreResult.Restored(
                            restoredBackup = LocalBackup.fromFile(backupFile),
                            safetyBackup = LocalBackup.fromFile(safetyBackupFile),
                        )
                    }.getOrElse { error ->
                        RestoreResult.Failed(
                            reason = error.toUserFacingMessage("Restore failed"),
                            restartRequired = databaseClosed,
                        )
                    }
                }
            }

        private fun checkpointDatabase() {
            val cursor = database.query(SimpleSQLiteQuery(WAL_CHECKPOINT_QUERY))
            cursor.use { checkpointCursor ->
                check(checkpointCursor.moveToFirst()) { "Database checkpoint result missing" }
                val result = checkpointCursor.readWalCheckpointResult()
                check(result.isComplete) { "Database checkpoint did not complete" }
            }
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

                !canonicalFile.hasValidDbCheckDatabase() ->
                    InvalidBackup("Backup file is not a valid dBcheck database")

                else -> ValidBackup(canonicalFile)
            }
        }

        private fun Cursor.readWalCheckpointResult(): WalCheckpointResult = WalCheckpointResult(
            isBusy = getInt(0) != 0,
            logFrames = getInt(1),
            checkpointedFrames = getInt(2),
        )

        private fun File.hasValidDbCheckDatabase(): Boolean =
            backupDatabaseValidator.isValidDbCheckDatabase(absolutePath)

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

private data class WalCheckpointResult(val isBusy: Boolean, val logFrames: Int, val checkpointedFrames: Int) {
    val isComplete: Boolean
        get() = !isBusy && logFrames == checkpointedFrames
}

private sealed interface BackupValidation

private data class ValidBackup(val file: File) : BackupValidation

private data class InvalidBackup(val reason: String) : BackupValidation

private fun createTempFile(directory: File?, targetName: String): File {
    checkNotNull(directory) { "Backup directory not available" }
    return File(directory, ".$targetName.tmp").also { temp ->
        if (temp.exists()) {
            check(temp.delete()) { "Unable to replace stale temporary backup file" }
        }
    }
}

private fun copyFileDurably(source: File, target: File) {
    source.inputStream().use { input ->
        FileOutputStream(target).use { output ->
            input.copyTo(output)
            output.fd.sync()
        }
    }
}

private fun moveReplacing(source: File, target: File) {
    runCatching {
        Files.move(
            source.toPath(),
            target.toPath(),
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING,
        )
    }.getOrElse { error ->
        if (error is AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } else {
            throw error
        }
    }
}

private fun rollbackDatabaseFile(safetyBackupFile: File, dbFile: File) {
    val rollbackFile = createTempFile(dbFile.parentFile, dbFile.name)
    copyFileDurably(safetyBackupFile, rollbackFile)
    moveReplacing(rollbackFile, dbFile)
}
