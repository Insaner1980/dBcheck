package com.dbcheck.app.sync

import android.database.sqlite.SQLiteDatabase
import com.dbcheck.app.data.local.db.DbCheckDatabase
import javax.inject.Inject

class BackupDatabaseValidator
    @Inject
    constructor() {
        fun isValidDbCheckDatabase(filePath: String): Boolean = runCatching {
            SQLiteDatabase.openDatabase(
                filePath,
                null,
                SQLiteDatabase.OPEN_READONLY,
            ).use { db ->
                db.hasSuccessfulQuickCheck() &&
                    db.hasRequiredTables() &&
                    db.hasSupportedSchemaIdentity()
            }
        }.getOrDefault(false)

        private fun SQLiteDatabase.hasSuccessfulQuickCheck(): Boolean =
            rawQuery("PRAGMA quick_check(1)", null).use { cursor ->
                cursor.moveToFirst() && cursor.getString(0).equals("ok", ignoreCase = true)
            }

        private fun SQLiteDatabase.hasRequiredTables(): Boolean = REQUIRED_TABLES.all { table ->
            rawQuery(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ? LIMIT 1",
                arrayOf(table),
            ).use { cursor -> cursor.moveToFirst() }
        }

        private fun SQLiteDatabase.hasSupportedSchemaIdentity(): Boolean {
            val version = rawQuery("PRAGMA user_version", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else null
            }
            if (version == null || version !in MIN_SUPPORTED_BACKUP_VERSION..DbCheckDatabase.SCHEMA_VERSION) {
                return false
            }
            return rawQuery(
                "SELECT identity_hash FROM room_master_table WHERE id = ? LIMIT 1",
                arrayOf(ROOM_MASTER_ID.toString()),
            ).use { cursor ->
                cursor.moveToFirst() && cursor.getString(0) == SUPPORTED_ROOM_IDENTITY_HASHES[version]
            }
        }

        private companion object {
            const val MIN_SUPPORTED_BACKUP_VERSION = 1
            const val ROOM_MASTER_ID = 42

            val SUPPORTED_ROOM_IDENTITY_HASHES =
                mapOf(
                    1 to "7e369d0b7c708c2370301558896f06a2",
                    2 to "4fa5483623ddc99a528580ea86c9ea72",
                    3 to "b1ad50c964bb5a4f1d99ab7ec30d8466",
                    4 to "8402c49af44a81e3a5655f2a6d98f931",
                    5 to "56d3e819afa7b7c40d364f3350dad74f",
                    6 to "36b8f638e83866237997dfff5967a6ab",
                    7 to "6a250805c37e3e9ea7099fe317afc873",
                    8 to "01ba54961f26e6fc079f94b5a4b70a99",
                    9 to "5b73e542adc2464266a32a6c3d216e15",
                    10 to "e4c97360fab833b6bc30549ab7e8075f",
                    11 to "716c7f0bf6a88b295970a3f5459e7cbf",
                    12 to "f73f218710d7988e02fb65939ff4fd56",
                    13 to "3b7807f7ad6982a9676bda9f07ed3a2d",
                )

            val REQUIRED_TABLES =
                listOf(
                    "room_master_table",
                    "sessions",
                    "measurements",
                    "hearing_test_results",
                )
        }
    }
