package com.dbcheck.app.sync

import android.content.Context
import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteQuery
import com.dbcheck.app.data.local.db.DbCheckDatabase
import com.dbcheck.app.testStringContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.Executors

class LocalBackupManagerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val filesDir: File by lazy { temporaryFolder.newFolder("files") }
    private val databaseDir: File by lazy { temporaryFolder.newFolder("databases") }
    private val databaseFile: File by lazy { File(databaseDir, DbCheckDatabase.DATABASE_NAME) }
    private var checkpointCursor = checkpointCursor()
    private val supportDatabase = mockk<SupportSQLiteDatabase>(relaxed = true)
    private val supportOpenHelper =
        mockk<SupportSQLiteOpenHelper> {
            every { writableDatabase } returns supportDatabase
        }
    private val database =
        mockk<DbCheckDatabase>(relaxed = true) {
            every { query(any<SupportSQLiteQuery>()) } answers { checkpointCursor }
            every { openHelper } returns supportOpenHelper
        }
    private val backupDatabaseValidator =
        mockk<BackupDatabaseValidator> {
            every { isValidDbCheckDatabase(any()) } returns true
        }
    private val context: Context =
        testStringContext().also { context ->
            every { context.filesDir } answers { this@LocalBackupManagerTest.filesDir }
            every { context.getDatabasePath(DbCheckDatabase.DATABASE_NAME) } answers { databaseFile }
        }

    @Test
    fun listBackupsSortsNewestDatabaseFilesFirst() = runTest {
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
    fun listBackupsRunsOnIoDispatcher() = runTest {
        val backupDir = File(filesDir, "backups").apply { mkdirs() }
        File(backupDir, "dbcheck_backup_20260509_120000.db").writeText("backup")
        var filesDirThreadName: String? = null
        every { context.filesDir } answers {
            filesDirThreadName = Thread.currentThread().name
            this@LocalBackupManagerTest.filesDir
        }
        val executor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "backup-list-io") }
        val dispatcher = executor.asCoroutineDispatcher()
        try {
            createManager(ioDispatcher = dispatcher).listBackups()

            assertEquals("backup-list-io", filesDirThreadName)
        } finally {
            dispatcher.close()
            executor.shutdown()
        }
    }

    @Test
    fun createLocalBackupCopiesDatabaseWithoutClosingRoomSingleton() = runTest {
            databaseFile.writeText("current database")

            val result = createManager().createLocalBackup()

            assertTrue(result is BackupResult.Created)
            val backup = (result as BackupResult.Created).backup
            assertEquals(File(filesDir, "backups").canonicalFile, backup.file.parentFile?.canonicalFile)
            assertTrue(backup.file.name.startsWith("dBcheck_backup_"))
            assertEquals("db", backup.file.extension)
            assertEquals("current database", backup.file.readText())
            verify {
                database.query(match<SupportSQLiteQuery> { it.sql == "PRAGMA wal_checkpoint(TRUNCATE)" })
            }
            verify(exactly = 0) { database.close() }
        }

    @Test
    fun activeMeasurementBlocksBackupAndRestoreAtManagerBoundary() = runTest {
            databaseFile.writeText("current database")
            val backup = managedBackup(extraContent = "restored database")
            val databaseGate = MeasurementDatabaseGate()
            val measurementOwner = Any()
            assertTrue(databaseGate.tryAcquire(measurementOwner))

            val manager = createManager(databaseGate = databaseGate)
            val backupResult = manager.createLocalBackup()
            val restoreResult = manager.restoreFromBackup(backup)

            assertTrue(backupResult is BackupResult.Failed)
            assertEquals(
                "Stop recording before managing backups",
                (backupResult as BackupResult.Failed).reason,
            )
            assertTrue(restoreResult is RestoreResult.Failed)
            assertEquals(
                "Stop recording before managing backups",
                (restoreResult as RestoreResult.Failed).reason,
            )
            verify(exactly = 0) { database.query(any<SupportSQLiteQuery>()) }
            verify(exactly = 0) { database.close() }
            databaseGate.release(measurementOwner)
        }

    @Test
    fun createBackupFailsOnBusyCheckpoint() = runTest {
            databaseFile.writeText("current database")
            checkpointCursor = checkpointCursor(isBusy = 1, logFrames = 3, checkpointedFrames = 1)

            val result = createManager().createLocalBackup()

            assertTrue(result is BackupResult.Failed)
            assertEquals("Backup failed", (result as BackupResult.Failed).reason)
            assertTrue(File(filesDir, "backups").listFiles()?.isEmpty() ?: true)
            verify(exactly = 0) { database.close() }
        }

    @Test
    fun createBackupRetriesCheckpointWhenWalChangesBeforeExclusiveCopyLock() = runTest {
            databaseFile.writeText("current database")
            val walFile = File(databaseFile.path + "-wal")
            var checkpointCalls = 0
            var transactionStarts = 0
            every { database.query(any<SupportSQLiteQuery>()) } answers {
                checkpointCalls += 1
                if (checkpointCalls > 1) {
                    walFile.delete()
                }
                checkpointCursor
            }
            every { supportDatabase.beginTransaction() } answers {
                transactionStarts += 1
                if (transactionStarts == 1) {
                    walFile.writeText("concurrent write")
                }
            }

            val result = createManager().createLocalBackup()

            assertTrue(result is BackupResult.Created)
            assertEquals(2, checkpointCalls)
            verify(exactly = 2) { supportDatabase.beginTransaction() }
            verify(exactly = 2) { supportDatabase.endTransaction() }
            verify(exactly = 1) { supportDatabase.setTransactionSuccessful() }
        }

    @Test
    fun createLocalBackupFailureReturnsGenericReason() = runTest {
            val result = createManager().createLocalBackup()

            assertTrue(result is BackupResult.Failed)
            assertEquals("Backup failed", (result as BackupResult.Failed).reason)
        }

    @Test
    fun restoreFromBackupCreatesSafetyBackupDeletesSidecarsAndClosesRoomSingleton() = runTest {
            databaseFile.writeText("current database")
            val walFile = File(databaseFile.path + "-wal").apply { writeText("wal") }
            File(databaseFile.path + "-shm").writeText("shm")
            every { database.query(any<SupportSQLiteQuery>()) } answers {
                walFile.writeText("")
                checkpointCursor
            }
            val backup = managedBackup(extraContent = "restored database")

            val result = createManager().restoreFromBackup(backup)

            assertTrue(result is RestoreResult.Restored)
            val restored = result as RestoreResult.Restored
            assertEquals(validDbCheckDatabaseProbe("restored database"), databaseFile.readText())
            assertTrue(restored.safetyBackup.file.name.startsWith("dBcheck_pre_restore_"))
            assertEquals("current database", restored.safetyBackup.file.readText())
            assertFalse(File(databaseFile.path + "-wal").exists())
            assertFalse(File(databaseFile.path + "-shm").exists())
            verify { database.close() }
        }

    @Test
    fun restoreFailsOnBusyCheckpoint() = runTest {
            databaseFile.writeText("current database")
            checkpointCursor = checkpointCursor(isBusy = 1, logFrames = 3, checkpointedFrames = 1)
            val backup = managedBackup(extraContent = "restored database")

            val result = createManager().restoreFromBackup(backup)

            assertTrue(result is RestoreResult.Failed)
            assertEquals("Restore failed", (result as RestoreResult.Failed).reason)
            assertEquals("current database", databaseFile.readText())
            verify(exactly = 0) { database.close() }
        }

    @Test
    fun restoreFromBackupRejectsNonDatabaseFileWithoutClosingRoomSingleton() = runTest {
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
    fun restoreRejectsInvalidManagedDb() = runTest {
            databaseFile.writeText("current database")
            val backupFile = managedBackupFile(content = "not a sqlite database")
            every {
                backupDatabaseValidator.isValidDbCheckDatabase(backupFile.canonicalFile.absolutePath)
            } returns false
            val backup = LocalBackup(file = backupFile, createdAtMillis = 200L, sizeBytes = backupFile.length())

            val result = createManager().restoreFromBackup(backup)

            assertTrue(result is RestoreResult.Failed)
            assertEquals("Backup file is not a valid dBcheck database", (result as RestoreResult.Failed).reason)
            assertEquals("current database", databaseFile.readText())
            verify(exactly = 0) { database.close() }
        }

    @Test
    fun restorePostCloseFailureRequiresRestart() = runTest {
            databaseFile.writeText("current database")
            File(databaseFile.path + "-wal").apply {
                mkdir()
                File(this, "locked").writeText("blocks directory delete")
            }
            val backup = managedBackup(extraContent = "restored database")

            val result = createManager().restoreFromBackup(backup)

            assertTrue(result is RestoreResult.Failed)
            assertEquals("Restore failed", (result as RestoreResult.Failed).reason)
            assertTrue(result.restartRequired)
            assertEquals("current database", databaseFile.readText())
            verify { database.close() }
        }

    @Test
    fun restoreFromBackupRejectsFilesOutsideBackupDirectoryWithoutClosingRoomSingleton() = runTest {
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
    fun restoreFailureReturnsGenericReason() = runTest {
            val backup = managedBackup(extraContent = "restored database")

            val result = createManager().restoreFromBackup(backup)

            assertTrue(result is RestoreResult.Failed)
            assertEquals("Restore failed", (result as RestoreResult.Failed).reason)
        }

    private fun createManager(
        ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.Unconfined,
        databaseGate: MeasurementDatabaseGate = MeasurementDatabaseGate(),
    ): LocalBackupManager = LocalBackupManager(
            context = context,
            database = database,
            backupDatabaseValidator = backupDatabaseValidator,
            measurementDatabaseGate = databaseGate,
            ioDispatcher = ioDispatcher,
        )

    private fun checkpointCursor(isBusy: Int = 0, logFrames: Int = 0, checkpointedFrames: Int = 0): Cursor =
        mockk(relaxed = true) {
            every { moveToFirst() } returns true
            every { getInt(0) } returns isBusy
            every { getInt(1) } returns logFrames
            every { getInt(2) } returns checkpointedFrames
        }

    private fun managedBackup(extraContent: String): LocalBackup {
        val backupFile = managedBackupFile(content = validDbCheckDatabaseProbe(extraContent))
        return LocalBackup(file = backupFile, createdAtMillis = 200L, sizeBytes = backupFile.length())
    }

    private fun managedBackupFile(content: String): File =
        File(filesDir, "backups/dbcheck_backup_20260509_120000.db").apply {
            parentFile?.mkdirs()
            writeText(content)
        }

    private fun validDbCheckDatabaseProbe(extraContent: String): String = buildString {
        append("SQLite format 3\u0000")
        append("\u0000".repeat(44))
        append("\u0000\u0000\u0000\u0003")
        append("room_master_table sessions measurements hearing_test_results")
        append(extraContent)
    }
}
