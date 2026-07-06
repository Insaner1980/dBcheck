package com.dbcheck.app

import android.content.ContentResolver
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import java.io.File

internal fun testExportCacheContext(cacheDir: File): Context {
    val contentResolver = mockk<ContentResolver>(relaxed = true)
    return mockk {
        every { this@mockk.cacheDir } returns cacheDir
        every { packageName } returns "com.dbcheck.app"
        every { this@mockk.contentResolver } returns contentResolver
    }
}
