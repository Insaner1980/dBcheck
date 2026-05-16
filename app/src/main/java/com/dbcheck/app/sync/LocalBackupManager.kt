package com.dbcheck.app.sync

import android.content.Context
import android.database.Cursor
import androidx.sqlite.db.SimpleSQLiteQuery
import com.dbcheck.app.data.local.db.DbCheckDatabase
import com.dbcheck.app.di.IoDispatcher
import com.dbcheck.app.util.toUserFacingMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
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
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : BackupGateway {
        companion object {
            private const val DATABASE_NAME = "dbcheck.db"
            private const val BACKUP_DIR = "backups"
            private const val BACKUP_PREFIX = "dbcheck_backup"
            private const val PRE_RESTORE_PREFIX = "dbcheck_pre_restore"
            private const val WAL_CHECKPOINT_QUERY = "PRAGMA wal_checkpoint(FULL)"
            private const val SQLITE_HEADER = "SQLite format 3\u0000"
            private const val SQLITE_HEADER_SIZE_BYTES = 100
            private const val SQLITE_USER_VERSION_OFFSET = 60
            private const val MIN_SUPPORTED_BACKUP_VERSION = 1
            private const val SCHEMA_SCAN_BYTES = 128 * 1024

            private val REQUIRED_SCHEMA_MARKERS =
                listOf(
                    "room_master_table",
                    "sessions",
                    "measurements",
                    "hearing_test_results",
                )
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
                var databaseClosed = false
                runCatching {
                    checkpointDatabase()

                    val dbFile = databaseFile()
                    check(dbFile.isFile) { "Database file not found" }

                    val safetyBackupFile = createBackupFile(PRE_RESTORE_PREFIX)
                    dbFile.copyTo(safetyBackupFile, overwrite = false)

                    database.close()
                    databaseClosed = true
                    deleteDatabaseSidecar(dbFile, "-wal")
                    deleteDatabaseSidecar(dbFile, "-shm")
                    backupFile.copyTo(dbFile, overwrite = true)

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

                !canonicalFile.hasDbCheckDatabaseFormat() ->
                    InvalidBackup("Backup file is not a valid dBcheck database")

                else -> ValidBackup(canonicalFile)
            }
        }

        private fun Cursor.readWalCheckpointResult(): WalCheckpointResult = WalCheckpointResult(
            isBusy = getInt(0) != 0,
            logFrames = getInt(1),
            checkpointedFrames = getInt(2),
        )

        private fun File.hasDbCheckDatabaseFormat(): Boolean {
            val probe = readProbeBytes()
            return length() >= SQLITE_HEADER_SIZE_BYTES &&
                probe.startsWithSqliteHeader() &&
                probe.hasSupportedUserVersion() &&
                probe.hasRequiredSchemaMarkers()
        }

        private fun File.readProbeBytes(): ByteArray {
            val probeSize = minOf(length(), SCHEMA_SCAN_BYTES.toLong()).toInt()
            val probe = ByteArray(probeSize)
            inputStream().use { input ->
                var offset = 0
                while (offset < probe.size) {
                    val read = input.read(probe, offset, probe.size - offset)
                    if (read == -1) break
                    offset += read
                }
                return if (offset == probe.size) probe else probe.copyOf(offset)
            }
        }

        private fun ByteArray.startsWithSqliteHeader(): Boolean {
            val header = SQLITE_HEADER.toByteArray(Charsets.US_ASCII)
            if (size < header.size) return false
            return header.indices.all { this[it] == header[it] }
        }

        private fun ByteArray.hasSupportedUserVersion(): Boolean {
            if (size < SQLITE_USER_VERSION_OFFSET + Int.SIZE_BYTES) return false

            val userVersion =
                ByteBuffer
                    .wrap(this, SQLITE_USER_VERSION_OFFSET, Int.SIZE_BYTES)
                    .int
            return userVersion in MIN_SUPPORTED_BACKUP_VERSION..DbCheckDatabase.SCHEMA_VERSION
        }

        private fun ByteArray.hasRequiredSchemaMarkers(): Boolean {
            val schemaProbe = toString(Charsets.ISO_8859_1)
            return REQUIRED_SCHEMA_MARKERS.all(schemaProbe::contains)
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

private data class WalCheckpointResult(val isBusy: Boolean, val logFrames: Int, val checkpointedFrames: Int) {
    val isComplete: Boolean
        get() = !isBusy && logFrames == checkpointedFrames
}

private sealed interface BackupValidation

private data class ValidBackup(val file: File) : BackupValidation

private data class InvalidBackup(val reason: String) : BackupValidation
