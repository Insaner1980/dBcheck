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
        assertTrue(source.contains("const val SCHEMA_VERSION = 13"))
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
    fun sessionEntityStoresOptionalSelectedAudioInputDeviceMetadata() {
        val source = mainSource("data/local/db/entity/SessionEntity.kt").readText()

        assertTrue(source.contains("val selectedAudioInputDeviceId: Int? = null"))
        assertTrue(source.contains("val selectedAudioInputDeviceName: String? = null"))
        assertTrue(source.contains("val routedAudioInputDeviceName: String? = null"))
    }

    @Test
    fun sessionEntityDoesNotStoreSleepSpecificMetadata() {
        val source = mainSource("data/local/db/entity/SessionEntity.kt").readText()

        assertFalse(source.contains("sleep"))
        assertFalse(source.contains("targetDurationMinutes"))
        assertFalse(source.contains("keepAwakeEnabled"))
        assertFalse(source.contains("notable"))
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
    fun exportedSchemaNineContainsSelectedAudioInputDeviceMetadata() {
        val schema = Path
            .of("schemas", "com.dbcheck.app.data.local.db.DbCheckDatabase", "9.json")
            .readText()

        assertTrue(schema.contains("\"version\": 9"))
        assertTrue(schema.contains("\"fieldPath\": \"selectedAudioInputDeviceId\""))
        assertTrue(schema.contains("\"columnName\": \"selectedAudioInputDeviceId\""))
        assertTrue(schema.contains("\"fieldPath\": \"selectedAudioInputDeviceName\""))
        assertTrue(schema.contains("\"columnName\": \"selectedAudioInputDeviceName\""))
        assertTrue(schema.contains("\"fieldPath\": \"routedAudioInputDeviceName\""))
        assertTrue(schema.contains("\"columnName\": \"routedAudioInputDeviceName\""))
    }

    @Test
    fun sleepSessionEntityStoresMetadataOutsideOrdinarySessions() {
        val source = mainSource("data/local/db/entity/SleepSessionEntity.kt").readText()

        assertTrue(source.contains("""tableName = "sleep_sessions""""))
        assertTrue(source.contains("entity = SessionEntity::class"))
        assertTrue(source.contains("onDelete = ForeignKey.CASCADE"))
        assertTrue(source.contains("@PrimaryKey val sessionId: Long"))
        assertTrue(source.contains("val targetDurationMinutes: Int"))
        assertTrue(source.contains("val keepAwakeEnabled: Boolean"))
        assertTrue(source.contains("val createdAt: Long"))
    }

    @Test
    fun sleepNotableEventEntityStoresEventsOnlyForSleepSessions() {
        val source = mainSource("data/local/db/entity/SleepNotableEventEntity.kt").readText()

        assertTrue(source.contains("""tableName = "sleep_notable_events""""))
        assertTrue(source.contains("entity = SleepSessionEntity::class"))
        assertTrue(source.contains("parentColumns = [\"sessionId\"]"))
        assertTrue(source.contains("childColumns = [\"sessionId\"]"))
        assertTrue(source.contains("onDelete = ForeignKey.CASCADE"))
        assertTrue(source.contains("val sessionId: Long"))
        assertTrue(source.contains("val timestamp: Long"))
        assertTrue(source.contains("val eventType: String"))
        assertTrue(source.contains("val levelDb: Float? = null"))
        assertTrue(source.contains("val durationMs: Long? = null"))
        assertTrue(source.contains("""value = ["sessionId", "timestamp"]"""))
        assertFalse(source.contains("rawAudio"))
        assertFalse(source.contains("FloatArray"))
    }

    @Test
    fun exportedSchemaTenContainsSleepTablesWithoutChangingOrdinarySessionColumns() {
        val schema = Path
            .of("schemas", "com.dbcheck.app.data.local.db.DbCheckDatabase", "10.json")
            .readText()

        assertTrue(schema.contains("\"version\": 10"))
        assertTrue(schema.contains("\"tableName\": \"sleep_sessions\""))
        assertTrue(schema.contains("\"tableName\": \"sleep_notable_events\""))
        assertTrue(schema.contains("\"fieldPath\": \"targetDurationMinutes\""))
        assertTrue(schema.contains("\"fieldPath\": \"keepAwakeEnabled\""))
        assertTrue(schema.contains("\"fieldPath\": \"eventType\""))
        assertTrue(schema.contains("\"fieldPath\": \"levelDb\""))
        assertTrue(schema.contains("\"fieldPath\": \"durationMs\""))
        assertFalse(schema.contains("`sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sleep"))
        assertFalse(schema.contains("\"fieldPath\": \"sleep"))
    }

    @Test
    fun passiveMonitoringEntityStoresOnlyAggregateColumns() {
        val source = mainSource("data/local/db/entity/PassiveMonitoringSampleEntity.kt").readText()

        assertTrue(source.contains("""tableName = "passive_monitoring_samples""""))
        assertTrue(source.contains("val startedAtMs: Long"))
        assertTrue(source.contains("val endedAtMs: Long"))
        assertTrue(source.contains("val readingCount: Int"))
        assertTrue(source.contains("val minDb: Float"))
        assertTrue(source.contains("val averageDb: Float"))
        assertTrue(source.contains("val maxDb: Float"))
        assertTrue(source.contains("val peakDb: Float"))
        assertTrue(source.contains("val totalEnergy: Double"))
        assertTrue(source.contains("""value = ["startedAtMs"]"""))
        assertTrue(source.contains("""value = ["endedAtMs"]"""))
        assertFalse(source.contains("rawAudio"))
        assertFalse(source.contains("pcm"))
        assertFalse(source.contains("FloatArray"))
        assertFalse(source.contains("sessionId"))
    }

    @Test
    fun exportedSchemaElevenContainsPassiveMonitoringAggregateTable() {
        val schema = Path
            .of("schemas", "com.dbcheck.app.data.local.db.DbCheckDatabase", "11.json")
            .readText()

        assertTrue(schema.contains("\"version\": 11"))
        assertTrue(schema.contains("\"tableName\": \"passive_monitoring_samples\""))
        assertTrue(schema.contains("\"fieldPath\": \"readingCount\""))
        assertTrue(schema.contains("\"fieldPath\": \"totalEnergy\""))
        assertFalse(schema.contains("rawAudio"))
        assertFalse(schema.contains("pcm"))
        assertFalse(schema.contains("FloatArray"))
    }

    @Test
    fun hearingRecoveryResultEntityStoresOnlyThresholdDeltas() {
        val source = mainSource("data/local/db/entity/HearingRecoveryResultEntity.kt").readText()

        assertTrue(source.contains("""tableName = "hearing_recovery_results""""))
        assertTrue(source.contains("entity = HearingTestResultEntity::class"))
        assertTrue(source.contains("onDelete = ForeignKey.CASCADE"))
        assertTrue(source.contains("val baselineTestId: Long"))
        assertTrue(source.contains("val averageShiftDb: Float"))
        assertTrue(source.contains("val maxShiftDb: Float"))
        assertTrue(source.contains("val leftEarShiftData: String"))
        assertTrue(source.contains("val rightEarShiftData: String"))
        assertTrue(source.contains("""value = ["timestamp"]"""))
        assertFalse(source.contains("rawAudio"))
        assertFalse(source.contains("pcm"))
        assertFalse(source.contains("FloatArray"))
    }

    @Test
    fun exportedSchemaTwelveContainsHearingRecoveryResults() {
        val schema = Path
            .of("schemas", "com.dbcheck.app.data.local.db.DbCheckDatabase", "12.json")
            .readText()

        assertTrue(schema.contains("\"version\": 12"))
        assertTrue(schema.contains("\"tableName\": \"hearing_recovery_results\""))
        assertTrue(schema.contains("\"fieldPath\": \"baselineTestId\""))
        assertTrue(schema.contains("\"fieldPath\": \"averageShiftDb\""))
        assertTrue(schema.contains("\"fieldPath\": \"maxShiftDb\""))
        assertTrue(schema.contains("\"fieldPath\": \"leftEarShiftData\""))
        assertTrue(schema.contains("\"fieldPath\": \"rightEarShiftData\""))
        assertFalse(schema.contains("rawAudio"))
        assertFalse(schema.contains("pcm"))
        assertFalse(schema.contains("FloatArray"))
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
    fun migrationEightToNineAddsNullableSelectedAudioInputDeviceMetadata() {
        val migration = migrationEightToNine()
        val database = mockk<SupportSQLiteDatabase>(relaxed = true)

        assertEquals(8, migration.startVersion)
        assertEquals(9, migration.endVersion)

        migration.migrate(database)

        verify {
            database.execSQL("ALTER TABLE `sessions` ADD COLUMN `selectedAudioInputDeviceId` INTEGER")
            database.execSQL("ALTER TABLE `sessions` ADD COLUMN `selectedAudioInputDeviceName` TEXT")
            database.execSQL("ALTER TABLE `sessions` ADD COLUMN `routedAudioInputDeviceName` TEXT")
        }
    }

    @Test
    fun migrationNineToTenCreatesSleepMetadataAndNotableEventTables() {
        val migration = migrationNineToTen()
        val database = mockk<SupportSQLiteDatabase>(relaxed = true)

        assertEquals(9, migration.startVersion)
        assertEquals(10, migration.endVersion)

        migration.migrate(database)

        verify {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `sleep_sessions` " +
                    "(`sessionId` INTEGER NOT NULL, " +
                    "`targetDurationMinutes` INTEGER NOT NULL, " +
                    "`keepAwakeEnabled` INTEGER NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`sessionId`), " +
                    "FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `sleep_notable_events` " +
                    "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`sessionId` INTEGER NOT NULL, " +
                    "`timestamp` INTEGER NOT NULL, " +
                    "`eventType` TEXT NOT NULL, " +
                    "`levelDb` REAL, " +
                    "`durationMs` INTEGER, " +
                    "FOREIGN KEY(`sessionId`) REFERENCES `sleep_sessions`(`sessionId`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_sleep_notable_events_sessionId_timestamp` " +
                    "ON `sleep_notable_events` (`sessionId`, `timestamp`)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_sleep_notable_events_timestamp` " +
                    "ON `sleep_notable_events` (`timestamp`)",
            )
        }
    }

    @Test
    fun migrationTenToElevenCreatesPassiveMonitoringAggregateTable() {
        val migration = migrationTenToEleven()
        val database = mockk<SupportSQLiteDatabase>(relaxed = true)

        assertEquals(10, migration.startVersion)
        assertEquals(11, migration.endVersion)

        migration.migrate(database)

        verify {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `passive_monitoring_samples` " +
                    "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`startedAtMs` INTEGER NOT NULL, " +
                    "`endedAtMs` INTEGER NOT NULL, " +
                    "`readingCount` INTEGER NOT NULL, " +
                    "`minDb` REAL NOT NULL, " +
                    "`averageDb` REAL NOT NULL, " +
                    "`maxDb` REAL NOT NULL, " +
                    "`peakDb` REAL NOT NULL, " +
                    "`totalEnergy` REAL NOT NULL)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_passive_monitoring_samples_startedAtMs` " +
                    "ON `passive_monitoring_samples` (`startedAtMs`)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_passive_monitoring_samples_endedAtMs` " +
                    "ON `passive_monitoring_samples` (`endedAtMs`)",
            )
        }
    }

    @Test
    fun migrationElevenToTwelveCreatesHearingRecoveryResultTable() {
        val migration = migrationElevenToTwelve()
        val database = mockk<SupportSQLiteDatabase>(relaxed = true)

        assertEquals(11, migration.startVersion)
        assertEquals(12, migration.endVersion)

        migration.migrate(database)

        verify {
            database.execSQL(DbCheckMigrations.CREATE_HEARING_RECOVERY_RESULTS_TABLE_SQL)
            database.execSQL(DbCheckMigrations.CREATE_HEARING_RECOVERY_RESULTS_TIMESTAMP_INDEX_SQL)
            database.execSQL(DbCheckMigrations.CREATE_HEARING_RECOVERY_RESULTS_BASELINE_TEST_ID_INDEX_SQL)
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
        assertTrue(source.contains("DbCheckMigrations.MIGRATION_8_9"))
        assertTrue(source.contains("DbCheckMigrations.MIGRATION_9_10"))
        assertTrue(source.contains("DbCheckMigrations.MIGRATION_10_11"))
        assertTrue(source.contains("DbCheckMigrations.MIGRATION_11_12"))
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

private fun migrationEightToNine(): Migration {
    val migrationsClass = Class.forName("com.dbcheck.app.data.local.db.DbCheckMigrations")
    return migrationsClass.getField("MIGRATION_8_9").get(null) as Migration
}

private fun migrationNineToTen(): Migration {
    val migrationsClass = Class.forName("com.dbcheck.app.data.local.db.DbCheckMigrations")
    return migrationsClass.getField("MIGRATION_9_10").get(null) as Migration
}

private fun migrationTenToEleven(): Migration {
    val migrationsClass = Class.forName("com.dbcheck.app.data.local.db.DbCheckMigrations")
    return migrationsClass.getField("MIGRATION_10_11").get(null) as Migration
}

private fun migrationElevenToTwelve(): Migration {
    val migrationsClass = Class.forName("com.dbcheck.app.data.local.db.DbCheckMigrations")
    return migrationsClass.getField("MIGRATION_11_12").get(null) as Migration
}
