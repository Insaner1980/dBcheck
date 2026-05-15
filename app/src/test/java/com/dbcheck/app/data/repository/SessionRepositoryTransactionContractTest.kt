package com.dbcheck.app.data.repository

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path
import kotlin.io.path.readText

class SessionRepositoryTransactionContractTest {
    @Test
    fun sessionCompletionPersistsMeasurementsAndCompletionInOneRoomTransaction() {
        val source = sessionRepositorySource().functionBlock("completeSessionWithMeasurements")

        assertTrue(source.contains("suspend fun completeSessionWithMeasurements"))
        assertTrue(source.contains("database.withTransaction"))
        assertTrue(
            source.indexOf("measurementDao.insertMeasurements") <
                source.indexOf("sessionDao.completeSession"),
        )
    }

    @Test
    fun activeSessionFlushPersistsMeasurementsAndRuntimeSummaryInOneRoomTransaction() {
        val source = sessionRepositorySource().functionBlock("recordActiveSessionMeasurements")

        assertTrue(source.contains("suspend fun recordActiveSessionMeasurements"))
        assertTrue(source.contains("database.withTransaction"))
        assertTrue(source.contains("measurementDao.insertMeasurements"))
        assertTrue(source.contains("sessionDao.updateSessionRuntimeSummary"))
    }
}

private fun sessionRepositorySource(): String = Path
        .of(
            "src",
            "main",
            "java",
            "com",
            "dbcheck",
            "app",
            "data",
            "repository",
            "SessionRepository.kt",
        ).readText()

private fun String.functionBlock(name: String): String = substringAfter("suspend fun $name")
        .substringBefore("\n\n        fun")
        .let { "suspend fun $name$it" }
