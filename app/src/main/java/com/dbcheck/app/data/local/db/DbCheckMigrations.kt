package com.dbcheck.app.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dbcheck.app.domain.audio.ResponseTime

object DbCheckMigrations {
    private const val TABLE_ID_PRIMARY_KEY_PREFIX_SQL = "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
    private const val TIMESTAMP_COLUMN_SQL = "`timestamp` INTEGER NOT NULL, "
    private const val CASCADE_FOREIGN_KEY_SUFFIX_SQL = "ON UPDATE NO ACTION ON DELETE CASCADE )"

    internal const val CREATE_HEARING_RECOVERY_RESULTS_TABLE_SQL =
        "CREATE TABLE IF NOT EXISTS `hearing_recovery_results` " +
            TABLE_ID_PRIMARY_KEY_PREFIX_SQL +
            "`baselineTestId` INTEGER NOT NULL, " +
            TIMESTAMP_COLUMN_SQL +
            "`testedFrequencyCount` INTEGER NOT NULL, " +
            "`averageShiftDb` REAL NOT NULL, " +
            "`maxShiftDb` REAL NOT NULL, " +
            "`status` TEXT NOT NULL, " +
            "`leftEarShiftData` TEXT NOT NULL, " +
            "`rightEarShiftData` TEXT NOT NULL, " +
            "FOREIGN KEY(`baselineTestId`) REFERENCES `hearing_test_results`(`id`) " +
            CASCADE_FOREIGN_KEY_SUFFIX_SQL

    internal const val CREATE_HEARING_RECOVERY_RESULTS_TIMESTAMP_INDEX_SQL =
        "CREATE INDEX IF NOT EXISTS `${DbCheckSchema.INDEX_HEARING_RECOVERY_RESULTS_TIMESTAMP}` " +
            "ON `hearing_recovery_results` (`timestamp`)"

    internal const val CREATE_HEARING_RECOVERY_RESULTS_BASELINE_TEST_ID_INDEX_SQL =
        "CREATE INDEX IF NOT EXISTS " +
            "`${DbCheckSchema.INDEX_HEARING_RECOVERY_RESULTS_BASELINE_TEST_ID}` " +
            "ON `hearing_recovery_results` (`baselineTestId`)"

    @JvmField
    val MIGRATION_1_2 =
        object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `activeSlot` INTEGER")
                db.execSQL(
                    "UPDATE `sessions` SET `activeSlot` = ${DbCheckSchema.ACTIVE_SESSION_SLOT} WHERE `id` = " +
                        "(SELECT `id` FROM `sessions` WHERE `isActive` = 1 " +
                        "ORDER BY `startTime` DESC, `id` DESC LIMIT 1)",
                )
                db.execSQL(
                    "UPDATE `sessions` SET `isActive` = 0, `endTime` = COALESCE(`endTime`, `startTime`) " +
                        "WHERE `isActive` = 1 AND `activeSlot` IS NULL",
                )
                db.execSQL("DROP INDEX IF EXISTS `${DbCheckSchema.INDEX_MEASUREMENTS_SESSION_ID}`")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `${DbCheckSchema.INDEX_SESSIONS_ACTIVE_SLOT}` " +
                        "ON `sessions` (`activeSlot`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `${DbCheckSchema.INDEX_SESSIONS_IS_ACTIVE_START_TIME}` " +
                        "ON `sessions` (`isActive`, `startTime`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `${DbCheckSchema.INDEX_SESSIONS_START_TIME}` " +
                        "ON `sessions` (`startTime`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `${DbCheckSchema.INDEX_MEASUREMENTS_SESSION_ID_TIMESTAMP}` " +
                        "ON `measurements` (`sessionId`, `timestamp`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `${DbCheckSchema.INDEX_HEARING_TEST_RESULTS_TIMESTAMP}` " +
                        "ON `hearing_test_results` (`timestamp`)",
                )
            }
        }

    @JvmField
    val MIGRATION_2_3 =
        object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `measurements` ADD COLUMN `peakDb` REAL NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `measurements` SET `peakDb` = `dbWeighted`")
            }
        }

    @JvmField
    val MIGRATION_3_4 =
        object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `measurements` ADD COLUMN `aWeightedDb` REAL NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `measurements` SET `aWeightedDb` = `dbWeighted`")
                db.execSQL(
                    "ALTER TABLE `measurements` ADD COLUMN `responseTime` TEXT NOT NULL DEFAULT " +
                        "'${ResponseTime.FAST.name}'",
                )
            }
        }

    @JvmField
    val MIGRATION_4_5 =
        object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sound_detection_events` " +
                        TABLE_ID_PRIMARY_KEY_PREFIX_SQL +
                        "`sessionId` INTEGER NOT NULL, " +
                        TIMESTAMP_COLUMN_SQL +
                        "`label` TEXT NOT NULL, " +
                        "`confidence` REAL NOT NULL, " +
                        "FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) " +
                        CASCADE_FOREIGN_KEY_SUFFIX_SQL,
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `${DbCheckSchema.INDEX_SOUND_DETECTION_EVENTS_SESSION_ID_TIMESTAMP}` " +
                        "ON `sound_detection_events` (`sessionId`, `timestamp`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `${DbCheckSchema.INDEX_SOUND_DETECTION_EVENTS_TIMESTAMP}` " +
                        "ON `sound_detection_events` (`timestamp`)",
                )
            }
        }

    @JvmField
    val MIGRATION_5_6 =
        object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `locationLatitude` REAL")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `locationLongitude` REAL")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `locationAccuracyMeters` REAL")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `locationCapturedAt` INTEGER")
            }
        }

    @JvmField
    val MIGRATION_6_7 =
        object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `calibration_profiles` " +
                        TABLE_ID_PRIMARY_KEY_PREFIX_SQL +
                        "`name` TEXT NOT NULL, " +
                        "`micSensitivityOffset` REAL NOT NULL, " +
                        "`isDefault` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `${DbCheckSchema.INDEX_CALIBRATION_PROFILES_NAME}` " +
                        "ON `calibration_profiles` (`name`)",
                )
            }
        }

    @JvmField
    val MIGRATION_7_8 =
        object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `calibration_profiles` " +
                        "ADD COLUMN `octaveBandOffsets` TEXT NOT NULL DEFAULT ''",
                )
            }
        }

    @JvmField
    val MIGRATION_8_9 =
        object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `selectedAudioInputDeviceId` INTEGER")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `selectedAudioInputDeviceName` TEXT")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `routedAudioInputDeviceName` TEXT")
            }
        }

    @JvmField
    val MIGRATION_9_10 =
        object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sleep_sessions` " +
                        "(`sessionId` INTEGER NOT NULL, " +
                        "`targetDurationMinutes` INTEGER NOT NULL, " +
                        "`keepAwakeEnabled` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`sessionId`), " +
                        "FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) " +
                        CASCADE_FOREIGN_KEY_SUFFIX_SQL,
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sleep_notable_events` " +
                        TABLE_ID_PRIMARY_KEY_PREFIX_SQL +
                        "`sessionId` INTEGER NOT NULL, " +
                        TIMESTAMP_COLUMN_SQL +
                        "`eventType` TEXT NOT NULL, " +
                        "`levelDb` REAL, " +
                        "`durationMs` INTEGER, " +
                        "FOREIGN KEY(`sessionId`) REFERENCES `sleep_sessions`(`sessionId`) " +
                        CASCADE_FOREIGN_KEY_SUFFIX_SQL,
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `${DbCheckSchema.INDEX_SLEEP_NOTABLE_EVENTS_SESSION_ID_TIMESTAMP}` " +
                        "ON `sleep_notable_events` (`sessionId`, `timestamp`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `${DbCheckSchema.INDEX_SLEEP_NOTABLE_EVENTS_TIMESTAMP}` " +
                        "ON `sleep_notable_events` (`timestamp`)",
                )
            }
        }

    @JvmField
    val MIGRATION_10_11 =
        object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `passive_monitoring_samples` " +
                        TABLE_ID_PRIMARY_KEY_PREFIX_SQL +
                        "`startedAtMs` INTEGER NOT NULL, " +
                        "`endedAtMs` INTEGER NOT NULL, " +
                        "`readingCount` INTEGER NOT NULL, " +
                        "`minDb` REAL NOT NULL, " +
                        "`averageDb` REAL NOT NULL, " +
                        "`maxDb` REAL NOT NULL, " +
                        "`peakDb` REAL NOT NULL, " +
                        "`totalEnergy` REAL NOT NULL)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `${DbCheckSchema.INDEX_PASSIVE_MONITORING_SAMPLES_STARTED_AT_MS}` " +
                        "ON `passive_monitoring_samples` (`startedAtMs`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `${DbCheckSchema.INDEX_PASSIVE_MONITORING_SAMPLES_ENDED_AT_MS}` " +
                        "ON `passive_monitoring_samples` (`endedAtMs`)",
                )
            }
        }

    @JvmField
    val MIGRATION_11_12 =
        object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(CREATE_HEARING_RECOVERY_RESULTS_TABLE_SQL)
                db.execSQL(CREATE_HEARING_RECOVERY_RESULTS_TIMESTAMP_INDEX_SQL)
                db.execSQL(CREATE_HEARING_RECOVERY_RESULTS_BASELINE_TEST_ID_INDEX_SQL)
            }
        }

    @JvmField
    val MIGRATION_12_13 =
        object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `startUtcOffsetSeconds` INTEGER")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `endUtcOffsetSeconds` INTEGER")
            }
        }
}
