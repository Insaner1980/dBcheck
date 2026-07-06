package com.dbcheck.app.data.repository

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path
import kotlin.io.path.readText

class SessionRepositoryTransactionContractTest {
    @Test
    fun sessionCompletionPersistsMeasurementsAndCompletionInOneRoomTransaction() {
        val source = sessionRepositorySource().functionBlock("completeSessionWithMeasurements")
        val insertHelper = sessionRepositorySource().functionBlock("insertSessionMeasurements")

        assertTrue(source.contains("suspend fun completeSessionWithMeasurements"))
        assertTrue(source.contains("database.withTransaction"))
        assertTrue(insertHelper.contains("measurementDao.insertMeasurements"))
        assertTrue(
            source.indexOf("insertSessionMeasurements") <
                source.indexOf("sessionDao.completeSession"),
        )
    }

    @Test
    fun activeSessionFlushPersistsMeasurementsAndRuntimeSummaryInOneRoomTransaction() {
        val source = sessionRepositorySource().functionBlock("recordActiveSessionMeasurements")

        assertTrue(source.contains("suspend fun recordActiveSessionMeasurements"))
        assertTrue(source.contains("database.withTransaction"))
        assertTrue(source.contains("insertSessionMeasurements"))
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

private fun String.functionBlock(name: String): String {
    val functionMarker =
        Regex("(private\\s+)?suspend fun $name|fun $name").find(this)?.value
            ?: error("Function $name not found")
    return substringAfter(functionMarker)
        .substringBefore("\n\n        fun")
        .substringBefore("\n\n        private")
        .let { "$functionMarker$it" }
}
