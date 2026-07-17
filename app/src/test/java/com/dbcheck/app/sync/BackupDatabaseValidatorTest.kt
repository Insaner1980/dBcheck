package com.dbcheck.app.sync

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import com.dbcheck.app.data.local.db.DbCheckDatabase
import com.dbcheck.app.projectFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
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
    fun validDatabaseWithCurrentRoomIdentityHashPassesValidation() {
        val databaseFile = createDatabaseFile()
        createDbCheckDatabase(databaseFile, roomIdentityHash = CURRENT_ROOM_IDENTITY_HASH)

        assertTrue(validator.isValidDbCheckDatabase(databaseFile.absolutePath))
    }

    @Test
    fun validDatabaseWithEveryExportedRoomSchemaIdentityHashPassesValidation() {
        val unsupportedVersions = (1..DbCheckDatabase.SCHEMA_VERSION)
            .map { version -> exportedSchemaIdentity(version) }
            .filterNot { schema ->
                val databaseFile = createDatabaseFile()
                createDbCheckDatabase(
                    databaseFile,
                    userVersion = schema.version,
                    roomIdentityHash = schema.identityHash,
                )

                validator.isValidDbCheckDatabase(databaseFile.absolutePath)
            }
            .map { schema -> schema.version }

        assertTrue("Unsupported exported schema versions: $unsupportedVersions", unsupportedVersions.isEmpty())
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

    private fun createDatabaseFile(): File = temporaryFolder.newFolder().resolve("backup.db")

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
        const val SUPPORTED_ROOM_IDENTITY_HASH = "01ba54961f26e6fc079f94b5a4b70a99"
        const val CURRENT_ROOM_IDENTITY_HASH = "3b7807f7ad6982a9676bda9f07ed3a2d"
    }
}

private data class RoomSchemaIdentity(val version: Int, val identityHash: String)

private fun exportedSchemaIdentity(version: Int): RoomSchemaIdentity {
    val schema = projectFile("schemas/com.dbcheck.app.data.local.db.DbCheckDatabase/$version.json").readText()
    val schemaVersion = requireNotNull(SCHEMA_VERSION_REGEX.find(schema)) {
        "Missing Room schema version in $version.json"
    }.groupValues[1].toInt()
    val identityHash = requireNotNull(IDENTITY_HASH_REGEX.find(schema)) {
        "Missing Room identity hash in $version.json"
    }.groupValues[1]

    return RoomSchemaIdentity(schemaVersion, identityHash)
}

private val SCHEMA_VERSION_REGEX = """"version":\s*(\d+)""".toRegex()
private val IDENTITY_HASH_REGEX = """"identityHash":\s*"([^"]+)"""".toRegex()
