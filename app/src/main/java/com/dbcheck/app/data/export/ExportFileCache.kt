package com.dbcheck.app.data.export

import java.io.File

object ExportFileCache {
    const val MAX_RETENTION_MS = 24L * 60L * 60L * 1000L
    private const val EXPORT_DIRECTORY = "exports"

    fun exportDirectory(cacheDir: File): File = File(cacheDir, EXPORT_DIRECTORY).apply { mkdirs() }

    fun exportFile(cacheDir: File, fileName: String): File = File(exportDirectory(cacheDir), fileName)

    fun cleanupStaleFiles(
        cacheDir: File,
        nowMs: Long = System.currentTimeMillis(),
        maxRetentionMs: Long = MAX_RETENTION_MS,
    ) {
        val cutoffMs = nowMs - maxRetentionMs
        exportDirectory(cacheDir)
            .listFiles()
            .orEmpty()
            .filter { file -> file.isFile && file.lastModified() < cutoffMs }
            .forEach(::deleteStaleFile)
    }

    private fun deleteStaleFile(file: File) {
        if (!file.delete() && file.exists()) {
            file.deleteOnExit()
        }
    }
}
