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

class CloudBackupManagerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val filesDir: File by lazy { temporaryFolder.newFolder("files") }
    private val databaseDir: File by lazy { temporaryFolder.newFolder("databases") }
    private val databaseFile: File by lazy { File(databaseDir, "dbcheck.db") }
    private val cursor =
        mockk<Cursor>(relaxed = true)
    private val database =
        mockk<DbCheckDatabase>(relaxed = true) {
            every { query(any<SupportSQLiteQuery>()) } returns cursor
        }
    private val context =
        mockk<Context> {
            every { filesDir } answers { this@CloudBackupManagerTest.filesDir }
            every { getDatabasePath("dbcheck.db") } answers { databaseFile }
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
    fun createLocalBackupCopiesDatabaseWithoutClosingRoomSingleton() =
        runTest {
            databaseFile.writeText("current database")

            val result = createManager().createLocalBackup()

            assertTrue(result is BackupResult.Created)
            val backup = (result as BackupResult.Created).backup
            assertEquals(File(filesDir, "backups").canonicalFile, backup.file.parentFile?.canonicalFile)
            assertEquals("db", backup.file.extension)
            assertEquals("current database", backup.file.readText())
            verify {
                database.query(match<SupportSQLiteQuery> { it.sql == "PRAGMA wal_checkpoint(FULL)" })
            }
            verify(exactly = 0) { database.close() }
        }

    @Test
    fun createLocalBackupFailureReturnsGenericReason() =
        runTest {
            val result = createManager().createLocalBackup()

            assertTrue(result is BackupResult.Failed)
            assertEquals("Backup failed", (result as BackupResult.Failed).reason)
        }

    @Test
    fun restoreFromBackupCreatesSafetyBackupDeletesSidecarsAndClosesRoomSingleton() =
        runTest {
            databaseFile.writeText("current database")
            File(databaseFile.path + "-wal").writeText("wal")
            File(databaseFile.path + "-shm").writeText("shm")
            val backupFile =
                File(filesDir, "backups/dbcheck_backup_20260509_120000.db").apply {
                    parentFile?.mkdirs()
                    writeText("restored database")
                }
            val backup = LocalBackup(file = backupFile, createdAtMillis = 200L, sizeBytes = backupFile.length())

            val result = createManager().restoreFromBackup(backup)

            assertTrue(result is RestoreResult.Restored)
            val restored = result as RestoreResult.Restored
            assertEquals("restored database", databaseFile.readText())
            assertTrue(restored.safetyBackup.file.name.startsWith("dbcheck_pre_restore_"))
            assertEquals("current database", restored.safetyBackup.file.readText())
            assertFalse(File(databaseFile.path + "-wal").exists())
            assertFalse(File(databaseFile.path + "-shm").exists())
            verify { database.close() }
        }

    @Test
    fun restoreFromBackupRejectsNonDatabaseFileWithoutClosingRoomSingleton() =
        runTest {
            databaseFile.writeText("current database")
            val backupFile =
                File(filesDir, "backups/not_a_database.txt").apply {
                    parentFile?.mkdirs()
                    writeText("not a db")
                }
            val backup = LocalBackup(file = backupFile, createdAtMillis = 200L, sizeBytes = backupFile.length())

            val result = createManager().restoreFromBackup(backup)

            assertTrue(result is RestoreResult.Failed)
            assertEquals("Backup file is not a dBcheck database backup", (result as RestoreResult.Failed).reason)
            assertEquals("current database", databaseFile.readText())
            verify(exactly = 0) { database.close() }
        }

    @Test
    fun restoreFromBackupRejectsFilesOutsideBackupDirectoryWithoutClosingRoomSingleton() =
        runTest {
            databaseFile.writeText("current database")
            val outsideFile = temporaryFolder.newFile("outside.db").apply { writeText("outside") }
            val backup = LocalBackup(file = outsideFile, createdAtMillis = 200L, sizeBytes = outsideFile.length())

            val result = createManager().restoreFromBackup(backup)

            assertTrue(result is RestoreResult.Failed)
            assertEquals("Backup file is not managed by dBcheck", (result as RestoreResult.Failed).reason)
            assertEquals("current database", databaseFile.readText())
            verify(exactly = 0) { database.close() }
        }

    @Test
    fun restoreFailureReturnsGenericReason() =
        runTest {
            val backupFile =
                File(filesDir, "backups/dbcheck_backup_20260509_120000.db").apply {
                    parentFile?.mkdirs()
                    writeText("restored database")
                }
            val backup = LocalBackup(file = backupFile, createdAtMillis = 200L, sizeBytes = backupFile.length())

            val result = createManager().restoreFromBackup(backup)

            assertTrue(result is RestoreResult.Failed)
            assertEquals("Restore failed", (result as RestoreResult.Failed).reason)
        }

    private fun createManager(): CloudBackupManager =
        CloudBackupManager(
            context = context,
            database = database,
            ioDispatcher = Dispatchers.Unconfined,
        )
}
