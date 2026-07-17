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

class SessionTimeZoneSchemaContractTest {
    @Test
    fun sessionEntityAndCompletionStoreHistoricalTimeZoneOffsets() {
        val entity = mainSource("data/local/db/entity/SessionEntity.kt").readText()
        val dao = mainSource("data/local/db/dao/SessionDao.kt").readText()

        assertTrue(entity.contains("val startUtcOffsetSeconds: Int? = null"))
        assertTrue(entity.contains("val endUtcOffsetSeconds: Int? = null"))
        assertTrue(dao.contains("data class SessionCompletionUpdate("))
        assertTrue(dao.contains("val endUtcOffsetSeconds: Int?,"))
        assertTrue(dao.contains("@Update(entity = SessionEntity::class)"))
    }

    @Test
    fun exportedSchemaThirteenContainsNullableSessionTimeZoneOffsets() {
        val schema = Path
            .of("schemas", "com.dbcheck.app.data.local.db.DbCheckDatabase", "13.json")
            .readText()

        assertTrue(schema.contains("\"version\": 13"))
        assertTrue(schema.contains("\"fieldPath\": \"startUtcOffsetSeconds\""))
        assertTrue(schema.contains("\"fieldPath\": \"endUtcOffsetSeconds\""))
        assertFalse(schema.contains("`startUtcOffsetSeconds` INTEGER NOT NULL"))
        assertFalse(schema.contains("`endUtcOffsetSeconds` INTEGER NOT NULL"))
    }

    @Test
    fun migrationTwelveToThirteenAddsNullableSessionTimeZoneOffsets() {
        val migration = migrationTwelveToThirteen()
        val database = mockk<SupportSQLiteDatabase>(relaxed = true)

        assertEquals(12, migration.startVersion)
        assertEquals(13, migration.endVersion)

        migration.migrate(database)

        verify {
            database.execSQL("ALTER TABLE `sessions` ADD COLUMN `startUtcOffsetSeconds` INTEGER")
            database.execSQL("ALTER TABLE `sessions` ADD COLUMN `endUtcOffsetSeconds` INTEGER")
        }
    }

    @Test
    fun databaseModuleRegistersTimeZoneOffsetMigration() {
        val source = mainSource("di/DatabaseModule.kt").readText()

        assertTrue(source.contains("DbCheckMigrations.MIGRATION_12_13"))
    }
}

private fun migrationTwelveToThirteen(): Migration {
    val migrationsClass = Class.forName("com.dbcheck.app.data.local.db.DbCheckMigrations")
    return migrationsClass.getField("MIGRATION_12_13").get(null) as Migration
}
