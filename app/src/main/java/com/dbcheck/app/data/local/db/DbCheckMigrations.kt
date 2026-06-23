package com.dbcheck.app.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dbcheck.app.domain.audio.ResponseTime

object DbCheckMigrations {
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
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`sessionId` INTEGER NOT NULL, " +
                        "`timestamp` INTEGER NOT NULL, " +
                        "`label` TEXT NOT NULL, " +
                        "`confidence` REAL NOT NULL, " +
                        "FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )",
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
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
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
}
