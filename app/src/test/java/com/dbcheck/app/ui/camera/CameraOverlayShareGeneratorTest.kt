package com.dbcheck.app.ui.camera

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import com.dbcheck.app.R
import com.dbcheck.app.data.export.ExportFileCache
import com.dbcheck.app.projectFile
import com.dbcheck.app.testExportCacheContext
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CameraOverlayShareGeneratorTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @After
    fun tearDown() {
        unmockkStatic(FileProvider::class)
    }

    @Test
    fun burnInOverlayChangesPixelsWithoutChangingDimensions() {
        val source = whiteBitmap(width = 400, height = 300)
        val readout =
            CameraOverlayBurnInReadout(
                status = "LIVE",
                dbText = "74 dB",
                levelLabel = "LCeq",
                timestampText = "Updated 12:00:00",
            )

        val burnedIn = burnCameraOverlayIntoBitmap(source, readout)

        assertEquals(400, burnedIn.width)
        assertEquals(300, burnedIn.height)
        assertNotEquals(Color.WHITE, burnedIn.getPixel(80, 220))
    }

    @Test
    fun burnedInOverlayUsesSharedPanelRadiusAndWordmark() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/camera/CameraOverlayShareGenerator.kt").readText()

        assertTrue(source.contains("canvas.drawRoundRect(rect, 24f * scale, 24f * scale, panelPaint)"))
        assertTrue(source.contains("canvas.drawText(\"dBcheck\""))
        assertFalse(source.contains("canvas.drawRoundRect(rect, 18f * scale, 18f * scale, panelPaint)"))
    }

    @Test
    fun photoShareIntentWritesBurnedInPngToExportCacheAndGrantsReadAccess() = runTest {
        val cacheDir = temporaryFolder.newFolder("cache")
        val context = cameraShareContext(cacheDir)
        val sourceFile = temporaryFolder.newFile("captured.jpg")
        FileOutputStream(sourceFile).use { output ->
            whiteBitmap(width = 480, height = 320).compress(Bitmap.CompressFormat.JPEG, 95, output)
        }
        val shareUri = Uri.parse("content://com.dbcheck.app.fileprovider/exports/camera.png")
        mockkStatic(FileProvider::class)
        every { FileProvider.getUriForFile(context, "com.dbcheck.app.fileprovider", any()) } returns shareUri
        val generator = CameraOverlayShareGenerator(context, UnconfinedTestDispatcher())

        val intent =
            generator.createPhotoShareIntent(
                sourcePhotoFile = sourceFile,
                readout =
                    CameraOverlayUiState(
                        currentDb = 73.6f,
                        status = CameraOverlayReadoutStatus.LIVE,
                        levelLabel = "LCeq",
                        timestampMs = 1_700_000_000_000L,
                    ),
            )

        val exportedFiles = ExportFileCache.exportDirectory(cacheDir).listFiles().orEmpty()
        val exportedPng = exportedFiles.single { it.name.startsWith("dBcheck_camera_overlay_") }
        val decodedPng = BitmapFactory.decodeFile(exportedPng.absolutePath)
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("image/png", intent.type)
        assertEquals(shareUri, intent.getParcelableExtra(Intent.EXTRA_STREAM))
        assertTrue((intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
        assertEquals(shareUri, intent.clipData?.getItemAt(0)?.uri)
        assertNotEquals(Color.WHITE, decodedPng.getPixel(80, decodedPng.height - 80))
    }

    @Test
    fun silentVideoFileUsesMp4ExportCacheName() = runTest {
        val cacheDir = temporaryFolder.newFolder("cache")
        val context = cameraShareContext(cacheDir)
        val generator = CameraOverlayShareGenerator(context, UnconfinedTestDispatcher())

        val outputFile = generator.createSilentVideoFile(nowMs = 1_700_000_000_000L)

        assertEquals(ExportFileCache.exportDirectory(cacheDir), outputFile.parentFile)
        assertTrue(outputFile.name.startsWith("dBcheck_camera_silent_video_"))
        assertTrue(outputFile.name.endsWith(".mp4"))
    }

    @Test
    fun captureFileCreationRunsOnIoDispatcher() = runTest {
        val cacheDir = temporaryFolder.newFolder("cache")
        val context = cameraShareContext(cacheDir)
        var cacheDirThreadName: String? = null
        every { context.cacheDir } answers {
            cacheDirThreadName = Thread.currentThread().name
            cacheDir
        }
        val executor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "camera-cache-io") }
        val dispatcher = executor.asCoroutineDispatcher()
        val generator = CameraOverlayShareGenerator(context, dispatcher)

        try {
            generator.createRawCaptureFile(nowMs = 1_700_000_000_000L)

            assertEquals("camera-cache-io", cacheDirThreadName)
        } finally {
            dispatcher.close()
            executor.shutdown()
        }
    }

    private fun cameraShareContext(cacheDir: File) = testExportCacheContext(cacheDir).also { context ->
            every { context.getString(R.string.camera_overlay_share_clip_label) } returns "dBcheck camera overlay"
            every { context.getString(R.string.camera_overlay_share_text) } returns "dBcheck camera overlay"
            every { context.getString(R.string.camera_overlay_status_live) } returns "LIVE"
            every { context.getString(R.string.camera_overlay_status_ready) } returns "READY"
            every { context.getString(R.string.camera_overlay_db_value, 74) } returns "74 dB"
            every { context.getString(R.string.camera_overlay_db_unavailable) } returns "-- dB"
            every { context.getString(R.string.camera_overlay_timestamp_value, any<String>()) } answers {
                val formatArgs = secondArg<Array<Any>>()
                "Updated ${formatArgs.single()}"
            }
            every { context.getString(R.string.camera_overlay_timestamp_unavailable) } returns "No live reading"
        }

    private fun whiteBitmap(width: Int, height: Int): Bitmap = createBitmap(width, height).apply {
            eraseColor(Color.WHITE)
        }
}
