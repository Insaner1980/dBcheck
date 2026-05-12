package com.dbcheck.app.data.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ExportFileCacheTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun exportDirectoryUsesDedicatedCacheSubdirectory() {
        val cacheDir = temporaryFolder.newFolder("cache")

        val exportDir = ExportFileCache.exportDirectory(cacheDir)

        assertEquals(File(cacheDir, "exports").canonicalFile, exportDir.canonicalFile)
        assertTrue(exportDir.isDirectory)
    }

    @Test
    fun cleanupStaleFilesDeletesOnlyOldFilesInsideExportDirectory() {
        val cacheDir = temporaryFolder.newFolder("cache")
        val exportDir = ExportFileCache.exportDirectory(cacheDir)
        val stale = File(exportDir, "old.csv").apply { writeText("old") }
        val fresh = File(exportDir, "fresh.csv").apply { writeText("fresh") }
        val unrelated = File(cacheDir, "outside.csv").apply { writeText("outside") }
        stale.setLastModified(NOW_MS - ExportFileCache.MAX_RETENTION_MS - 1)
        fresh.setLastModified(NOW_MS)
        unrelated.setLastModified(NOW_MS - ExportFileCache.MAX_RETENTION_MS - 1)

        ExportFileCache.cleanupStaleFiles(cacheDir, nowMs = NOW_MS)

        assertFalse(stale.exists())
        assertTrue(fresh.exists())
        assertTrue(unrelated.exists())
    }

    private companion object {
        const val NOW_MS = 1_700_000_000_000L
    }
}
