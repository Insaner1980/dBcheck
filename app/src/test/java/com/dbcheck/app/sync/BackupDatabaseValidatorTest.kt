package com.dbcheck.app.sync

import android.database.sqlite.SQLiteDatabase
import com.dbcheck.app.data.local.db.DbCheckDatabase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class BackupDatabaseValidatorTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val validator = BackupDatabaseValidator()

    @Test
    fun validDatabaseWithSupportedRoomIdentityHashPassesValidation() {
        val databaseFile = createDatabaseFile()
        createDbCheckDatabase(databaseFile)

        assertTrue(validator.isValidDbCheckDatabase(databaseFile.absolutePath))
    }

    @Test
    fun databaseWithoutRequiredTablesFailsValidation() {
        val databaseFile = createDatabaseFile()
        createDbCheckDatabase(databaseFile, createMeasurementsTable = false)

        assertFalse(validator.isValidDbCheckDatabase(databaseFile.absolutePath))
    }

    @Test
    fun databaseWithUnsupportedSchemaVersionFailsValidation() {
        val databaseFile = createDatabaseFile()
        createDbCheckDatabase(databaseFile, userVersion = DbCheckDatabase.SCHEMA_VERSION + 1)

        assertFalse(validator.isValidDbCheckDatabase(databaseFile.absolutePath))
    }

    @Test
    fun databaseWithUnsupportedRoomIdentityHashFailsValidation() {
        val databaseFile = createDatabaseFile()
        createDbCheckDatabase(databaseFile, roomIdentityHash = "unsupported")

        assertFalse(validator.isValidDbCheckDatabase(databaseFile.absolutePath))
    }

    @Test
    fun nonDatabaseFileFailsValidation() {
        val file = temporaryFolder.newFile("not-a-database.db").apply {
            writeText("not sqlite")
        }

        assertFalse(validator.isValidDbCheckDatabase(file.absolutePath))
    }

    private fun createDatabaseFile(): File = temporaryFolder.newFile("backup.db").also { file ->
        file.delete()
    }

    private fun createDbCheckDatabase(
        file: File,
        userVersion: Int = DbCheckDatabase.SCHEMA_VERSION,
        roomIdentityHash: String = SUPPORTED_ROOM_IDENTITY_HASH,
        createMeasurementsTable: Boolean = true,
    ) {
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("PRAGMA user_version = $userVersion")
            db.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            db.execSQL("INSERT INTO room_master_table (id, identity_hash) VALUES (42, '$roomIdentityHash')")
            db.execSQL("CREATE TABLE sessions (id INTEGER PRIMARY KEY)")
            if (createMeasurementsTable) {
                db.execSQL("CREATE TABLE measurements (id INTEGER PRIMARY KEY)")
            }
            db.execSQL("CREATE TABLE hearing_test_results (id INTEGER PRIMARY KEY)")
        }
    }

    private companion object {
        const val SUPPORTED_ROOM_IDENTITY_HASH = "8402c49af44a81e3a5655f2a6d98f931"
    }
}
