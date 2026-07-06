package com.dbcheck.app.sync

import android.content.Context
import android.database.Cursor
import androidx.sqlite.db.SimpleSQLiteQuery
import com.dbcheck.app.R
import com.dbcheck.app.data.local.db.DbCheckDatabase
import com.dbcheck.app.di.IoDispatcher
import com.dbcheck.app.util.ProductIdentity
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
            private const val BACKUP_DIR = "backups"
            private const val BACKUP_PREFIX = "${ProductIdentity.FILE_NAME_PREFIX}_backup"
            private const val PRE_RESTORE_PREFIX = "${ProductIdentity.FILE_NAME_PREFIX}_pre_restore"
            private const val DATABASE_FILE_EXTENSION = "db"
            private const val BACKUP_TIMESTAMP_PATTERN = "yyyyMMdd_HHmmss"
            private const val WAL_CHECKPOINT_QUERY = "PRAGMA wal_checkpoint(TRUNCATE)"
            private const val WAL_SIDE_CAR_SUFFIX = "-wal"
            private const val SHM_SIDE_CAR_SUFFIX = "-shm"
            private const val ERROR_DATABASE_FILE_NOT_FOUND = "Database file not found"
            private const val ERROR_BACKUP_VALIDATION_FAILED = "Backup validation failed"
            private const val ERROR_SAFETY_BACKUP_VALIDATION_FAILED = "Safety backup validation failed"
            private const val ERROR_RESTORE_STAGING_VALIDATION_FAILED = "Restore staging validation failed"
            private const val ERROR_CHECKPOINT_RESULT_MISSING = "Database checkpoint result missing"
            private const val ERROR_CHECKPOINT_INCOMPLETE = "Database checkpoint did not complete"
            private const val ERROR_REPLACE_DATABASE_SIDECAR = "Unable to replace database sidecar"
        }

        override suspend fun listBackups(): List<LocalBackup> = withContext(ioDispatcher) {
            val backupDir = backupDirectory()
            backupDir
                .listFiles()
                ?.filter { it.isFile && it.extension.equals(DATABASE_FILE_EXTENSION, ignoreCase = true) }
                ?.sortedByDescending { it.lastModified() }
                ?.map(LocalBackup::fromFile)
                ?: emptyList()
        }

        override suspend fun createLocalBackup(): BackupResult = withContext(ioDispatcher) {
                backupOperationMutex.withLock {
                    runCatching {
                        checkpointDatabase()

                        val dbFile = databaseFile()
                        check(dbFile.isFile) { ERROR_DATABASE_FILE_NOT_FOUND }

                        val backupFile = createBackupFile(BACKUP_PREFIX)
                        val tempBackupFile = createTempFile(backupFile.parentFile, backupFile.name)
                        copyFileDurably(dbFile, tempBackupFile)
                        check(tempBackupFile.hasValidDbCheckDatabase()) { ERROR_BACKUP_VALIDATION_FAILED }
                        moveReplacing(tempBackupFile, backupFile)
                        LocalBackup.fromFile(backupFile)
                    }.fold(
                        onSuccess = BackupResult::Created,
                        onFailure = { error ->
                            BackupResult.Failed(
                                error.toUserFacingMessage(context.getString(R.string.settings_backup_failed)),
                            )
                        },
                    )
                }
            }

        override suspend fun restoreFromBackup(backup: LocalBackup): RestoreResult = withContext(ioDispatcher) {
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
                        check(dbFile.isFile) { ERROR_DATABASE_FILE_NOT_FOUND }

                        val safetyBackupFile = createBackupFile(PRE_RESTORE_PREFIX)
                        val tempSafetyBackupFile = createTempFile(safetyBackupFile.parentFile, safetyBackupFile.name)
                        copyFileDurably(dbFile, tempSafetyBackupFile)
                        check(tempSafetyBackupFile.hasValidDbCheckDatabase()) { ERROR_SAFETY_BACKUP_VALIDATION_FAILED }
                        moveReplacing(tempSafetyBackupFile, safetyBackupFile)

                        val stagedRestoreFile = createTempFile(dbFile.parentFile, dbFile.name)
                        copyFileDurably(backupFile, stagedRestoreFile)
                        check(stagedRestoreFile.hasValidDbCheckDatabase()) { ERROR_RESTORE_STAGING_VALIDATION_FAILED }

                        database.close()
                        databaseClosed = true
                        runCatching {
                            deleteDatabaseSidecar(dbFile, WAL_SIDE_CAR_SUFFIX)
                            deleteDatabaseSidecar(dbFile, SHM_SIDE_CAR_SUFFIX)
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
                            reason =
                                error.toUserFacingMessage(
                                    context.getString(R.string.settings_backup_restore_failed),
                                ),
                            restartRequired = databaseClosed,
                        )
                    }
                }
            }

        private fun checkpointDatabase() {
            val cursor = database.query(SimpleSQLiteQuery(WAL_CHECKPOINT_QUERY))
            cursor.use { checkpointCursor ->
                check(checkpointCursor.moveToFirst()) { ERROR_CHECKPOINT_RESULT_MISSING }
                val result = checkpointCursor.readWalCheckpointResult()
                check(result.isComplete) { ERROR_CHECKPOINT_INCOMPLETE }
            }
        }

        private fun validateBackupFile(file: File): BackupValidation {
            val canonicalFile = file.canonicalFile
            val canonicalBackupDir = backupDirectory().canonicalFile

            return when {
                !canonicalFile.extension.equals(DATABASE_FILE_EXTENSION, ignoreCase = true) ->
                    InvalidBackup(context.getString(R.string.settings_backup_not_db_file))

                canonicalFile.parentFile?.canonicalFile != canonicalBackupDir ->
                    InvalidBackup(context.getString(R.string.settings_backup_not_managed))

                !canonicalFile.isFile ->
                    InvalidBackup(context.getString(R.string.settings_backup_not_found))

                !canonicalFile.hasValidDbCheckDatabase() ->
                    InvalidBackup(context.getString(R.string.settings_backup_invalid_database))

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

        private fun backupDirectory(): File = File(context.filesDir, BACKUP_DIR).apply { mkdirs() }

        private fun databaseFile(): File = context.getDatabasePath(DbCheckDatabase.DATABASE_NAME)

        private fun createBackupFile(prefix: String): File {
            val timestamp = SimpleDateFormat(BACKUP_TIMESTAMP_PATTERN, Locale.US).format(Date())
            val backupDir = backupDirectory()
            val baseName = "${prefix}_$timestamp"
            var backupFile = File(backupDir, "$baseName.$DATABASE_FILE_EXTENSION")
            var counter = 1

            while (backupFile.exists()) {
                backupFile = File(backupDir, "${baseName}_$counter.$DATABASE_FILE_EXTENSION")
                counter += 1
            }

            return backupFile
        }

        private fun deleteDatabaseSidecar(dbFile: File, suffix: String) {
            val sidecar = File(dbFile.path + suffix)
            check(!sidecar.exists() || sidecar.delete()) {
                "$ERROR_REPLACE_DATABASE_SIDECAR ${sidecar.name}"
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
    checkNotNull(directory) { ERROR_BACKUP_DIRECTORY_NOT_AVAILABLE }
    return File(directory, "$TEMP_FILE_PREFIX$targetName$TEMP_FILE_SUFFIX")
        .also { temp ->
            if (temp.exists()) {
                check(temp.delete()) { ERROR_REPLACE_STALE_TEMP_BACKUP }
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

private const val TEMP_FILE_PREFIX = "."
private const val TEMP_FILE_SUFFIX = ".tmp"
private const val ERROR_BACKUP_DIRECTORY_NOT_AVAILABLE = "Backup directory not available"
private const val ERROR_REPLACE_STALE_TEMP_BACKUP = "Unable to replace stale temporary backup file"
