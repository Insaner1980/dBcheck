package com.dbcheck.app.util

import android.graphics.Bitmap
import androidx.core.content.FileProvider
import com.dbcheck.app.data.export.ExportFileCache
import com.dbcheck.app.testStringContext
import com.dbcheck.app.ui.camera.CameraOverlayShareGenerator
import com.dbcheck.app.ui.camera.CameraOverlayUiState
import com.dbcheck.app.ui.camera.burnCameraOverlayIntoBitmap
import com.dbcheck.app.ui.camera.decodeCameraPhoto
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.lang.reflect.InvocationTargetException

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PngShareFailureTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @After
    fun tearDown() {
        unmockkStatic(FileProvider::class)
        unmockkStatic("com.dbcheck.app.ui.camera.CameraOverlayShareGeneratorKt")
    }

    @Test
    fun cacheFailureStillReleasesOwnedBitmap() {
        val context = testStringContext()
        val error = IOException("Cache unavailable")
        every { context.cacheDir } throws error
        val bitmap = mockk<Bitmap>(relaxed = true)

        assertSame(error, publishFailure(ShareResultsGenerator(context, UnconfinedTestDispatcher()), bitmap))
        verify(exactly = 1) { bitmap.recycle() }
    }

    @Test
    fun failedCompressionNeverPublishesEmptyPng() {
        val cacheDir = temporaryFolder.newFolder("cache")
        val context = testStringContext()
        every { context.cacheDir } returns cacheDir
        val bitmap = mockk<Bitmap>(relaxed = true)
        stubFailedCompression(bitmap)

        assertTrue(publishFailure(ShareResultsGenerator(context, UnconfinedTestDispatcher()), bitmap) is IOException)
        verify(exactly = 0) { FileProvider.getUriForFile(any(), any(), any()) }
        verify(exactly = 1) { bitmap.recycle() }
        assertTrue(ExportFileCache.exportDirectory(cacheDir).listFiles().orEmpty().isEmpty())
    }

    @Test
    fun failedCameraCompressionReleasesBitmapsAndFiles() = runTest {
        val cacheDir = temporaryFolder.newFolder("camera-cache")
        val context = testStringContext()
        every { context.cacheDir } returns cacheDir
        val sourceFile = temporaryFolder.newFile("capture.jpg")
        val source = mockk<Bitmap>(relaxed = true)
        val output = mockk<Bitmap>(relaxed = true)
        mockkStatic("com.dbcheck.app.ui.camera.CameraOverlayShareGeneratorKt")
        every { decodeCameraPhoto(sourceFile) } returns source
        every { burnCameraOverlayIntoBitmap(source, any(), context) } returns output
        stubFailedCompression(output)

        val error = runCatching {
            CameraOverlayShareGenerator(context, UnconfinedTestDispatcher())
                .createPhotoShareIntent(sourceFile, CameraOverlayUiState())
        }.exceptionOrNull()

        assertTrue(error is IOException)
        verify(exactly = 0) { FileProvider.getUriForFile(any(), any(), any()) }
        verify(exactly = 1) { source.recycle(); output.recycle() }
        assertTrue(!sourceFile.exists())
        assertTrue(ExportFileCache.exportDirectory(cacheDir).listFiles().orEmpty().isEmpty())
    }

    private fun stubFailedCompression(bitmap: Bitmap) {
        every { bitmap.compress(Bitmap.CompressFormat.PNG, 100, any()) } returns false
        mockkStatic(FileProvider::class)
        every { FileProvider.getUriForFile(any(), any(), any()) } throws
            IllegalArgumentException("Published failed PNG")
    }

    private fun publishFailure(generator: ShareResultsGenerator, bitmap: Bitmap): Throwable? {
        val method = ShareResultsGenerator::class.java.getDeclaredMethod(
            "createImageShareIntent", Bitmap::class.java, String::class.java, String::class.java, String::class.java,
        ).apply { isAccessible = true }
        return try {
            method.invoke(generator, bitmap, "fixture.png", "Fixture", "Fixture")
            null
        } catch (error: InvocationTargetException) {
            error.cause
        }
    }
}
