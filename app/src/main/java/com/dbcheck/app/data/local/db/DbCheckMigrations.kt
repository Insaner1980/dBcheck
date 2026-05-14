package com.dbcheck.app.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
}
