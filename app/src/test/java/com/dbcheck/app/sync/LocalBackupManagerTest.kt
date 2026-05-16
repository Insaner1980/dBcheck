package com.dbcheck.app.sync

import android.content.Context
import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteQuery
import com.dbcheck.app.data.local.db.DbCheckDatabase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LocalBackupManagerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val filesDir: File by lazy { temporaryFolder.newFolder("files") }
    private val databaseDir: File by lazy { temporaryFolder.newFolder("databases") }
    private val databaseFile: File by lazy { File(databaseDir, "dbcheck.db") }
    private var checkpointCursor = checkpointCursor()
    private val database =
        mockk<DbCheckDatabase>(relaxed = true) {
            every { query(any<SupportSQLiteQuery>()) } answers { checkpointCursor }
        }
    private val context =
        mockk<Context> {
            every { filesDir } answers { this@LocalBackupManagerTest.filesDir }
            every { getDatabasePath("dbcheck.db") } answers { databaseFile }
        }
    private var backupDatabaseValid = true
    private val backupDatabaseValidator =
        mockk<BackupDatabaseValidator> {
            every { isValidDbCheckDatabase(any()) } answers { backupDatabaseValid }
        }

    @Test
    fun listBackupsSortsNewestDatabaseFilesFirst() {
        val backupDir = File(filesDir, "backups").apply { mkdirs() }
        val older = File(backupDir, "dbcheck_backup_20260508_120000.db").apply { writeText("older") }
        val newer = File(backupDir, "dbcheck_backup_20260509_120000.db").apply { writeText("newer") }
        File(backupDir, "notes.txt").writeText("ignored")
        older.setLastModified(100L)
        newer.setLastModified(200L)

        val backups = createManager().listBackups()

        assertEquals(
            listOf(newer.name, older.name),
            backups.map { it.file.name },
        )
    }

    @Test
    fun createLocalBackupCopiesDatabaseWithoutClosingRoomSingleton() = runTest {
            databaseFile.writeText(validDbCheckDatabaseProbe("current database"))

            val result = createManager().createLocalBackup()

            assertTrue(result is BackupResult.Created)
            val backup = (result as BackupResult.Created).backup
            assertEquals(File(filesDir, "backups").canonicalFile, backup.file.parentFile?.canonicalFile)
            assertEquals("db", backup.file.extension)
            assertEquals(validDbCheckDatabaseProbe("current database"), backup.file.readText())
            verify {
                database.query(match<SupportSQLiteQuery> { it.sql == "PRAGMA wal_checkpoint(TRUNCATE)" })
            }
            verify(exactly = 0) { database.close() }
        }

    @Test
    fun createBackupFailsOnBusyCheckpoint() = runTest {
            databaseFile.writeText(validDbCheckDatabaseProbe("current database"))
            checkpointCursor = checkpointCursor(isBusy = 1, logFrames = 3, checkpointedFrames = 1)

            val result = createManager().createLocalBackup()

            assertTrue(result is BackupResult.Failed)
            assertEquals("Backup failed", (result as BackupResult.Failed).reason)
            assertTrue(File(filesDir, "backups").listFiles()?.isEmpty() ?: true)
            verify(exactly = 0) { database.close() }
        }

    @Test
    fun createLocalBackupFailureReturnsGenericReason() = runTest {
            val result = createManager().createLocalBackup()

            assertTrue(result is BackupResult.Failed)
            assertEquals("Backup failed", (result as BackupResult.Failed).reason)
        }

    @Test
    fun restoreFromBackupCreatesSafetyBackupDeletesSidecarsAndClosesRoomSingleton() = runTest {
            databaseFile.writeText(validDbCheckDatabaseProbe("current database"))
            File(databaseFile.path + "-wal").writeText("wal")
            File(databaseFile.path + "-shm").writeText("shm")
            val backup = managedBackup(validDbCheckDatabaseProbe("restored database"))

            val result = createManager().restoreFromBackup(backup)

            assertTrue(result is RestoreResult.Restored)
            val restored = result as RestoreResult.Restored
            assertEquals(validDbCheckDatabaseProbe("restored database"), databaseFile.readText())
            assertTrue(restored.safetyBackup.file.name.startsWith("dbcheck_pre_restore_"))
            assertEquals(validDbCheckDatabaseProbe("current database"), restored.safetyBackup.file.readText())
            assertFalse(File(databaseFile.path + "-wal").exists())
            assertFalse(File(databaseFile.path + "-shm").exists())
            verify { database.close() }
        }

    @Test
    fun restoreFailsOnBusyCheckpoint() = runTest {
            databaseFile.writeText(validDbCheckDatabaseProbe("current database"))
            checkpointCursor = checkpointCursor(isBusy = 1, logFrames = 3, checkpointedFrames = 1)
            val backup = managedBackup(validDbCheckDatabaseProbe("restored database"))

            val result = createManager().restoreFromBackup(backup)

            assertTrue(result is RestoreResult.Failed)
            assertEquals("Restore failed", (result as RestoreResult.Failed).reason)
            assertEquals(validDbCheckDatabaseProbe("current database"), databaseFile.readText())
            verify(exactly = 0) { database.close() }
        }

    @Test
    fun restoreFromBackupRejectsNonDatabaseFileWithoutClosingRoomSingleton() = runTest {
            databaseFile.writeText(validDbCheckDatabaseProbe("current database"))
            val backupFile =
                File(filesDir, "backups/not_a_database.txt").apply {
                    parentFile?.mkdirs()
                    writeText("not a db")
                }
            val backup = LocalBackup(file = backupFile, createdAtMillis = 200L, sizeBytes = backupFile.length())

            val result = createManager().restoreFromBackup(backup)

            assertTrue(result is RestoreResult.Failed)
            assertEquals("Backup file is not a dBcheck database backup", (result as RestoreResult.Failed).reason)
            assertEquals(validDbCheckDatabaseProbe("current database"), databaseFile.readText())
            verify(exactly = 0) { database.close() }
        }

    @Test
    fun restoreRejectsInvalidManagedDb() = runTest {
            databaseFile.writeText(validDbCheckDatabaseProbe("current database"))
            assertInvalidManagedBackupRejected("not a sqlite database")
        }

    @Test
    fun restoreRejectsManagedDbWithoutRoomIdentityHash() = runTest {
            databaseFile.writeText(validDbCheckDatabaseProbe("current database"))
            assertInvalidManagedBackupRejected(validDbCheckDatabaseProbe("restored database", includeIdentityHash = false))
        }

    @Test
    fun restoreRejectsMarkerSpoofedDatabaseProbe() = runTest {
            databaseFile.writeText(validDbCheckDatabaseProbe("current database"))
            assertInvalidManagedBackupRejected(validDbCheckDatabaseProbe("marker spoof without sqlite validation"))
        }

    @Test
    fun restorePostCloseFailureRequiresRestart() = runTest {
            databaseFile.writeText(validDbCheckDatabaseProbe("current database"))
            File(databaseFile.path + "-wal").apply {
                mkdir()
                File(this, "locked").writeText("blocks directory delete")
            }
            val backup = managedBackup(validDbCheckDatabaseProbe("restored database"))

            val result = createManager().restoreFromBackup(backup)

            assertTrue(result is RestoreResult.Failed)
            assertEquals("Restore failed", (result as RestoreResult.Failed).reason)
            assertTrue(result.restartRequired)
            verify { database.close() }
        }

    @Test
    fun restoreFromBackupRejectsFilesOutsideBackupDirectoryWithoutClosingRoomSingleton() = runTest {
            databaseFile.writeText(validDbCheckDatabaseProbe("current database"))
            val outsideFile = temporaryFolder.newFile("outside.db").apply { writeText("outside") }
            val backup = LocalBackup(file = outsideFile, createdAtMillis = 200L, sizeBytes = outsideFile.length())

            val result = createManager().restoreFromBackup(backup)

            assertTrue(result is RestoreResult.Failed)
            assertEquals("Backup file is not managed by dBcheck", (result as RestoreResult.Failed).reason)
            assertEquals(validDbCheckDatabaseProbe("current database"), databaseFile.readText())
            verify(exactly = 0) { database.close() }
        }

    @Test
    fun restoreFailureReturnsGenericReason() = runTest {
            val backup = managedBackup(validDbCheckDatabaseProbe("restored database"))

            val result = createManager().restoreFromBackup(backup)

            assertTrue(result is RestoreResult.Failed)
            assertEquals("Restore failed", (result as RestoreResult.Failed).reason)
        }

    private fun createManager(): LocalBackupManager = LocalBackupManager(
            context = context,
            database = database,
            backupDatabaseValidator = backupDatabaseValidator,
            ioDispatcher = Dispatchers.Unconfined,
        )

    private fun managedBackup(content: String): LocalBackup {
        val backupFile =
            File(filesDir, "backups/dbcheck_backup_20260509_120000.db").apply {
                parentFile?.mkdirs()
                writeText(content)
            }
        return LocalBackup(file = backupFile, createdAtMillis = 200L, sizeBytes = backupFile.length())
    }

    private suspend fun assertInvalidManagedBackupRejected(content: String) {
        val backup = managedBackup(content)
        backupDatabaseValid = false

        val result = createManager().restoreFromBackup(backup)

        assertTrue(result is RestoreResult.Failed)
        assertEquals("Backup file is not a valid dBcheck database", (result as RestoreResult.Failed).reason)
        assertEquals(validDbCheckDatabaseProbe("current database"), databaseFile.readText())
        verify(exactly = 0) { database.close() }
    }

    private fun checkpointCursor(isBusy: Int = 0, logFrames: Int = 0, checkpointedFrames: Int = 0): Cursor =
        mockk(relaxed = true) {
            every { moveToFirst() } returns true
            every { getInt(0) } returns isBusy
            every { getInt(1) } returns logFrames
            every { getInt(2) } returns checkpointedFrames
        }

    private fun validDbCheckDatabaseProbe(extraContent: String, includeIdentityHash: Boolean = true): String =
        buildString {
            append("SQLite format 3\u0000")
            append("\u0000".repeat(44))
            append("\u0000\u0000\u0000\u0003")
            append("room_master_table sessions measurements hearing_test_results")
            if (includeIdentityHash) {
                append(" b1ad50c964bb5a4f1d99ab7ec30d8466")
            }
            append(extraContent)
        }
}
