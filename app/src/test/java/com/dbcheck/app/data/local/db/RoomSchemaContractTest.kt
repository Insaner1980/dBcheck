package com.dbcheck.app.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path
import kotlin.io.path.readText

class RoomSchemaContractTest {
    @Test
    fun databaseVersionMatchesSchemaMigrationVersion() {
        val source = mainSource("data/local/db/DbCheckDatabase.kt").readText()

        assertTrue(source.contains("version = DbCheckDatabase.SCHEMA_VERSION"))
        assertTrue(source.contains("const val SCHEMA_VERSION = 8"))
    }

    @Test
    fun exportedRoomSchemasAreNotIgnored() {
        val gitignore = Path.of("..", ".gitignore").readText().lines().map { it.trim() }

        assertFalse(gitignore.contains("/app/schemas"))
    }

    @Test
    fun sessionEntityIndexesHistoryAndActiveSessionQueries() {
        val source = mainSource("data/local/db/entity/SessionEntity.kt").readText()

        assertTrue(source.contains("val activeSlot: Int? ="))
        assertTrue(source.contains("unique = true"))
        assertTrue(source.contains("""value = ["activeSlot"]"""))
        assertTrue(source.contains("""value = ["isActive", "startTime"]"""))
        assertTrue(source.contains("""value = ["startTime"]"""))
    }

    @Test
    fun sessionEntityStoresOptionalApproximateLocationMetadata() {
        val source = mainSource("data/local/db/entity/SessionEntity.kt").readText()

        assertTrue(source.contains("val locationLatitude: Double? = null"))
        assertTrue(source.contains("val locationLongitude: Double? = null"))
        assertTrue(source.contains("val locationAccuracyMeters: Float? = null"))
        assertTrue(source.contains("val locationCapturedAt: Long? = null"))
        assertFalse(source.contains("altitude"))
        assertFalse(source.contains("bearing"))
        assertFalse(source.contains("speed"))
    }

    @Test
    fun activeSessionQueryIsDeterministicWhenMultipleRowsExist() {
        val source = mainSource("data/local/db/dao/SessionDao.kt").readText()

        assertTrue(
            source.contains(
                "SELECT * FROM sessions WHERE activeSlot = 1 " +
                    "ORDER BY startTime DESC, id DESC LIMIT 1",
            ),
        )
    }

    @Test
    fun latestQueriesBreakTimestampTiesByNewestRowId() {
        val sessionDao = mainSource("data/local/db/dao/SessionDao.kt").readText()
        val hearingTestDao = mainSource("data/local/db/dao/HearingTestDao.kt").readText()

        assertTrue(
            sessionDao.contains(
                "ORDER BY startTime DESC, id DESC LIMIT :limit",
            ),
        )
        assertTrue(
            hearingTestDao.contains(
                "SELECT * FROM hearing_test_results ORDER BY timestamp DESC, id DESC LIMIT 1",
            ),
        )
    }

    @Test
    fun measurementEntityIndexesSessionTimeSeriesQueries() {
        val source = mainSource("data/local/db/entity/MeasurementEntity.kt").readText()

        assertTrue(source.contains("""value = ["sessionId", "timestamp"]"""))
        assertTrue(source.contains("""value = ["timestamp"]"""))
        assertTrue(source.contains("val peakDb: Float"))
        assertTrue(source.contains("val aWeightedDb: Float"))
        assertTrue(source.contains("val responseTime: String"))
    }

    @Test
    fun exportedSchemaFourContainsMeasurementCoreColumns() {
        val schema = Path
            .of("schemas", "com.dbcheck.app.data.local.db.DbCheckDatabase", "4.json")
            .readText()

        assertTrue(schema.contains("\"version\": 4"))
        assertTrue(schema.contains("\"fieldPath\": \"aWeightedDb\""))
        assertTrue(schema.contains("\"columnName\": \"aWeightedDb\""))
        assertTrue(schema.contains("\"fieldPath\": \"responseTime\""))
        assertTrue(schema.contains("\"columnName\": \"responseTime\""))
    }

    @Test
    fun soundDetectionEventEntityStoresOnlyAggregatedColumnsAndCascadesWithSessions() {
        val source = mainSource("data/local/db/entity/SoundDetectionEventEntity.kt").readText()

        assertTrue(source.contains("""tableName = "sound_detection_events""""))
        assertTrue(source.contains("onDelete = ForeignKey.CASCADE"))
        assertTrue(source.contains("val sessionId: Long"))
        assertTrue(source.contains("val timestamp: Long"))
        assertTrue(source.contains("val label: String"))
        assertTrue(source.contains("val confidence: Float"))
        assertFalse(source.contains("FloatArray"))
        assertFalse(source.contains("rawAudio"))
        assertFalse(source.contains("window"))
    }

    @Test
    fun exportedSchemaFiveContainsSoundDetectionEvents() {
        val schema = Path
            .of("schemas", "com.dbcheck.app.data.local.db.DbCheckDatabase", "5.json")
            .readText()

        assertTrue(schema.contains("\"version\": 5"))
        assertTrue(schema.contains("\"tableName\": \"sound_detection_events\""))
        assertTrue(schema.contains("\"columnName\": \"label\""))
        assertTrue(schema.contains("\"columnName\": \"confidence\""))
        assertFalse(schema.contains("rawAudio"))
    }

    @Test
    fun exportedSchemaSixContainsNullableSessionLocationColumns() {
        val schema = Path
            .of("schemas", "com.dbcheck.app.data.local.db.DbCheckDatabase", "6.json")
            .readText()

        assertTrue(schema.contains("\"version\": 6"))
        assertTrue(schema.contains("\"fieldPath\": \"locationLatitude\""))
        assertTrue(schema.contains("\"columnName\": \"locationLatitude\""))
        assertTrue(schema.contains("\"fieldPath\": \"locationLongitude\""))
        assertTrue(schema.contains("\"columnName\": \"locationLongitude\""))
        assertTrue(schema.contains("\"fieldPath\": \"locationAccuracyMeters\""))
        assertTrue(schema.contains("\"columnName\": \"locationAccuracyMeters\""))
        assertTrue(schema.contains("\"fieldPath\": \"locationCapturedAt\""))
        assertTrue(schema.contains("\"columnName\": \"locationCapturedAt\""))
        assertTrue(
            schema.contains(
                "`locationLatitude` REAL, `locationLongitude` REAL, " +
                    "`locationAccuracyMeters` REAL, `locationCapturedAt` INTEGER",
            ),
        )
        assertFalse(schema.contains("`locationLatitude` REAL NOT NULL"))
        assertFalse(schema.contains("`locationLongitude` REAL NOT NULL"))
        assertFalse(schema.contains("`locationAccuracyMeters` REAL NOT NULL"))
        assertFalse(schema.contains("`locationCapturedAt` INTEGER NOT NULL"))
    }

    @Test
    fun calibrationProfileEntityStoresFlatCalibrationProfileMetadata() {
        val source = mainSource("data/local/db/entity/CalibrationProfileEntity.kt").readText()

        assertTrue(source.contains("""tableName = "calibration_profiles""""))
        assertTrue(source.contains("val name: String"))
        assertTrue(source.contains("val micSensitivityOffset: Float"))
        assertTrue(source.contains("val octaveBandOffsets: String"))
        assertTrue(source.contains("""@ColumnInfo(defaultValue = "''")"""))
        assertTrue(source.contains("val isDefault: Boolean = false"))
        assertTrue(source.contains("val createdAt: Long"))
        assertTrue(source.contains("val updatedAt: Long"))
        assertTrue(source.contains("""value = ["name"]"""))
    }

    @Test
    fun exportedSchemaSevenContainsCalibrationProfiles() {
        val schema = Path
            .of("schemas", "com.dbcheck.app.data.local.db.DbCheckDatabase", "7.json")
            .readText()

        assertTrue(schema.contains("\"version\": 7"))
        assertTrue(schema.contains("\"tableName\": \"calibration_profiles\""))
        assertTrue(schema.contains("\"fieldPath\": \"micSensitivityOffset\""))
        assertTrue(schema.contains("\"columnName\": \"micSensitivityOffset\""))
        assertTrue(schema.contains("\"fieldPath\": \"isDefault\""))
        assertTrue(schema.contains("\"fieldPath\": \"updatedAt\""))
    }

    @Test
    fun exportedSchemaEightContainsCalibrationProfileOctaveOffsets() {
        val schema = Path
            .of("schemas", "com.dbcheck.app.data.local.db.DbCheckDatabase", "8.json")
            .readText()

        assertTrue(schema.contains("\"version\": 8"))
        assertTrue(schema.contains("\"tableName\": \"calibration_profiles\""))
        assertTrue(schema.contains("\"fieldPath\": \"octaveBandOffsets\""))
        assertTrue(schema.contains("\"columnName\": \"octaveBandOffsets\""))
        assertTrue(schema.contains("\"defaultValue\": \"''\""))
    }

    @Test
    fun hearingTestEntityIndexesLatestResultQuery() {
        val source = mainSource("data/local/db/entity/HearingTestResultEntity.kt").readText()

        assertTrue(source.contains("""value = ["timestamp"]"""))
    }

    @Test
    fun migrationOneToTwoCreatesDeclaredIndexes() {
        val migration = migrationOneToTwo()
        val database = mockk<SupportSQLiteDatabase>(relaxed = true)

        assertEquals(1, migration.startVersion)
        assertEquals(2, migration.endVersion)

        migration.migrate(database)

        verify {
            database.execSQL("ALTER TABLE `sessions` ADD COLUMN `activeSlot` INTEGER")
            database.execSQL(
                "UPDATE `sessions` SET `activeSlot` = 1 WHERE `id` = " +
                    "(SELECT `id` FROM `sessions` WHERE `isActive` = 1 ORDER BY `startTime` DESC, `id` DESC LIMIT 1)",
            )
            database.execSQL(
                "UPDATE `sessions` SET `isActive` = 0, `endTime` = COALESCE(`endTime`, `startTime`) " +
                    "WHERE `isActive` = 1 AND `activeSlot` IS NULL",
            )
            database.execSQL("DROP INDEX IF EXISTS `index_measurements_sessionId`")
            database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_sessions_activeSlot` " +
                    "ON `sessions` (`activeSlot`)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_sessions_isActive_startTime` " +
                    "ON `sessions` (`isActive`, `startTime`)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_sessions_startTime` " +
                    "ON `sessions` (`startTime`)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_measurements_sessionId_timestamp` " +
                    "ON `measurements` (`sessionId`, `timestamp`)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_hearing_test_results_timestamp` " +
                    "ON `hearing_test_results` (`timestamp`)",
            )
        }
    }

    @Test
    fun migrationTwoToThreeAddsMeasurementPeakDb() {
        val migration = migrationTwoToThree()
        val database = mockk<SupportSQLiteDatabase>(relaxed = true)

        assertEquals(2, migration.startVersion)
        assertEquals(3, migration.endVersion)

        migration.migrate(database)

        verify {
            database.execSQL("ALTER TABLE `measurements` ADD COLUMN `peakDb` REAL NOT NULL DEFAULT 0")
            database.execSQL("UPDATE `measurements` SET `peakDb` = `dbWeighted`")
        }
    }

    @Test
    fun migrationThreeToFourAddsMeasurementAWeightedDbAndResponseTimeMetadata() {
        val migration = migrationThreeToFour()
        val database = mockk<SupportSQLiteDatabase>(relaxed = true)

        assertEquals(3, migration.startVersion)
        assertEquals(4, migration.endVersion)

        migration.migrate(database)

        verify {
            database.execSQL("ALTER TABLE `measurements` ADD COLUMN `aWeightedDb` REAL NOT NULL DEFAULT 0")
            database.execSQL("UPDATE `measurements` SET `aWeightedDb` = `dbWeighted`")
            database.execSQL("ALTER TABLE `measurements` ADD COLUMN `responseTime` TEXT NOT NULL DEFAULT 'FAST'")
        }
    }

    @Test
    fun migrationFourToFiveCreatesSoundDetectionEventTable() {
        val migration = migrationFourToFive()
        val database = mockk<SupportSQLiteDatabase>(relaxed = true)

        assertEquals(4, migration.startVersion)
        assertEquals(5, migration.endVersion)

        migration.migrate(database)

        verify {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `sound_detection_events` " +
                    "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`sessionId` INTEGER NOT NULL, " +
                    "`timestamp` INTEGER NOT NULL, " +
                    "`label` TEXT NOT NULL, " +
                    "`confidence` REAL NOT NULL, " +
                    "FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_sound_detection_events_sessionId_timestamp` " +
                    "ON `sound_detection_events` (`sessionId`, `timestamp`)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_sound_detection_events_timestamp` " +
                    "ON `sound_detection_events` (`timestamp`)",
            )
        }
    }

    @Test
    fun migrationFiveToSixAddsNullableSessionLocationColumns() {
        val migration = migrationFiveToSix()
        val database = mockk<SupportSQLiteDatabase>(relaxed = true)

        assertEquals(5, migration.startVersion)
        assertEquals(6, migration.endVersion)

        migration.migrate(database)

        verify {
            database.execSQL("ALTER TABLE `sessions` ADD COLUMN `locationLatitude` REAL")
            database.execSQL("ALTER TABLE `sessions` ADD COLUMN `locationLongitude` REAL")
            database.execSQL("ALTER TABLE `sessions` ADD COLUMN `locationAccuracyMeters` REAL")
            database.execSQL("ALTER TABLE `sessions` ADD COLUMN `locationCapturedAt` INTEGER")
        }
    }

    @Test
    fun migrationSixToSevenCreatesCalibrationProfileTable() {
        val migration = migrationSixToSeven()
        val database = mockk<SupportSQLiteDatabase>(relaxed = true)

        assertEquals(6, migration.startVersion)
        assertEquals(7, migration.endVersion)

        migration.migrate(database)

        verify {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `calibration_profiles` " +
                    "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`micSensitivityOffset` REAL NOT NULL, " +
                    "`isDefault` INTEGER NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "`updatedAt` INTEGER NOT NULL)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_calibration_profiles_name` " +
                    "ON `calibration_profiles` (`name`)",
            )
        }
    }

    @Test
    fun migrationSevenToEightAddsCalibrationProfileOctaveOffsets() {
        val migration = migrationSevenToEight()
        val database = mockk<SupportSQLiteDatabase>(relaxed = true)

        assertEquals(7, migration.startVersion)
        assertEquals(8, migration.endVersion)

        migration.migrate(database)

        verify {
            database.execSQL(
                "ALTER TABLE `calibration_profiles` " +
                    "ADD COLUMN `octaveBandOffsets` TEXT NOT NULL DEFAULT ''",
            )
        }
    }

    @Test
    fun databaseModuleRegistersSessionLocationMigration() {
        val source = mainSource("di/DatabaseModule.kt").readText()

        assertTrue(source.contains("DbCheckMigrations.MIGRATION_5_6"))
    }

    @Test
    fun databaseModuleRegistersCalibrationProfileMigration() {
        val source = mainSource("di/DatabaseModule.kt").readText()

        assertTrue(source.contains("DbCheckMigrations.MIGRATION_6_7"))
        assertTrue(source.contains("DbCheckMigrations.MIGRATION_7_8"))
    }

    @Test
    fun historySessionQueriesOnlyReturnCompletedSessionsWithMeasurements() {
        val sessionDao = mainSource("data/local/db/dao/SessionDao.kt").readText()

        assertTrue(sessionDao.contains("private const val COMPLETED_HISTORY_SESSION_FILTER"))
        assertTrue(sessionDao.contains("isActive = 0"))
        assertTrue(sessionDao.contains("endTime IS NOT NULL"))
        assertTrue(sessionDao.contains("endTime > startTime"))
        assertTrue(sessionDao.contains("EXISTS (SELECT 1 FROM measurements"))
        assertTrue(sessionDao.contains("measurements.sessionId = sessions.id"))
        assertTrue(sessionDao.contains("private const val SELECT_COMPLETED_HISTORY_SESSIONS"))
        assertTrue(sessionDao.contains("private const val SELECT_COMPLETED_HISTORY_SESSIONS_IN_FREE_WINDOW"))
        assertTrue(sessionDao.contains("SELECT * FROM sessions WHERE \$COMPLETED_HISTORY_SESSION_FILTER"))
        assertTrue(sessionDao.contains("AND startTime >= :sevenDaysAgo"))
    }
}

private fun migrationOneToTwo(): Migration {
    val migrationsClass = Class.forName("com.dbcheck.app.data.local.db.DbCheckMigrations")
    return migrationsClass.getField("MIGRATION_1_2").get(null) as Migration
}

private fun migrationTwoToThree(): Migration {
    val migrationsClass = Class.forName("com.dbcheck.app.data.local.db.DbCheckMigrations")
    return migrationsClass.getField("MIGRATION_2_3").get(null) as Migration
}

private fun migrationThreeToFour(): Migration {
    val migrationsClass = Class.forName("com.dbcheck.app.data.local.db.DbCheckMigrations")
    return migrationsClass.getField("MIGRATION_3_4").get(null) as Migration
}

private fun migrationFourToFive(): Migration {
    val migrationsClass = Class.forName("com.dbcheck.app.data.local.db.DbCheckMigrations")
    return migrationsClass.getField("MIGRATION_4_5").get(null) as Migration
}

private fun migrationFiveToSix(): Migration {
    val migrationsClass = Class.forName("com.dbcheck.app.data.local.db.DbCheckMigrations")
    return migrationsClass.getField("MIGRATION_5_6").get(null) as Migration
}

private fun migrationSixToSeven(): Migration {
    val migrationsClass = Class.forName("com.dbcheck.app.data.local.db.DbCheckMigrations")
    return migrationsClass.getField("MIGRATION_6_7").get(null) as Migration
}

private fun migrationSevenToEight(): Migration {
    val migrationsClass = Class.forName("com.dbcheck.app.data.local.db.DbCheckMigrations")
    return migrationsClass.getField("MIGRATION_7_8").get(null) as Migration
}

private fun mainSource(relativePath: String): Path =
    Path.of("src", "main", "java", "com", "dbcheck", "app", *relativePath.split("/").toTypedArray())
