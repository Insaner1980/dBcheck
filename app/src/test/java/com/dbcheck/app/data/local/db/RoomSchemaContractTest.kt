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
        assertTrue(source.contains("const val SCHEMA_VERSION = 4"))
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

private fun mainSource(relativePath: String): Path =
    Path.of("src", "main", "java", "com", "dbcheck", "app", *relativePath.split("/").toTypedArray())
