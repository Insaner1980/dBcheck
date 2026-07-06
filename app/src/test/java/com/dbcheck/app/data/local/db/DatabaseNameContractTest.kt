package com.dbcheck.app.data.local.db

import com.dbcheck.app.projectFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseNameContractTest {
    @Test
    fun databaseModuleUsesDatabaseNameConstant() {
        val source = projectFile("src/main/java/com/dbcheck/app/di/DatabaseModule.kt").readText()

        assertTrue(source.contains("DbCheckDatabase.DATABASE_NAME"))
        assertFalse(source.contains("\"dbcheck.db\""))
    }
}
