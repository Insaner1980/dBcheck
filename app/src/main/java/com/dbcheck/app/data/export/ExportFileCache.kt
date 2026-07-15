package com.dbcheck.app.data.export

import android.content.Context
import java.io.File

object ExportFileCache {
    const val MAX_RETENTION_MS = 24L * 60L * 60L * 1000L
    const val EXPORT_DIRECTORY_NAME = "exports"
    const val EXPORT_DIRECTORY_PATH = "$EXPORT_DIRECTORY_NAME/"
    const val FILE_PROVIDER_AUTHORITY_SUFFIX = "fileprovider"

    fun fileProviderAuthority(context: Context): String = "${context.packageName}.$FILE_PROVIDER_AUTHORITY_SUFFIX"

    fun exportDirectory(cacheDir: File): File = File(cacheDir, EXPORT_DIRECTORY_NAME).apply { mkdirs() }

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
            .forEach(::deleteExportFile)
    }

    fun deleteExportFile(file: File) {
        if (!file.delete() && file.exists()) {
            file.deleteOnExit()
        }
    }
}
