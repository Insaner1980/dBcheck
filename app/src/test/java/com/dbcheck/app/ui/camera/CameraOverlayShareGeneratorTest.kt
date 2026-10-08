package com.dbcheck.app.ui.camera

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.content.IntentCompat
import androidx.core.graphics.createBitmap
import androidx.exifinterface.media.ExifInterface
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
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
    @Config(sdk = [26])
    fun photoDecodeSamplesLargeJpegAndAppliesExifRotationBeforeBurnIn() {
        val sourceFile = temporaryFolder.newFile("rotated-large.jpg")
        val source = whiteBitmap(width = 4_096, height = 2_048)
        try {
            FileOutputStream(sourceFile).use { source.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        } finally {
            source.recycle()
        }
        ExifInterface(sourceFile.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }

        val decoded = decodeCameraPhoto(sourceFile)
        try {
            assertEquals(1_024, decoded.width)
            assertEquals(2_048, decoded.height)
        } finally {
            decoded.recycle()
        }
    }

    @Test
    @Config(sdk = [28])
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun photoOrientationSupportsEveryExifRotationAndMirror() {
        val source = createBitmap(2, 3)
        val pixels = intArrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.CYAN, Color.MAGENTA, Color.YELLOW)
        source.setPixels(pixels, 0, 2, 0, 0, 2, 3)
        val expected = mapOf(
            ExifInterface.ORIENTATION_NORMAL to listOf(0, 1, 2, 3, 4, 5),
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL to listOf(1, 0, 3, 2, 5, 4),
            ExifInterface.ORIENTATION_ROTATE_180 to listOf(5, 4, 3, 2, 1, 0),
            ExifInterface.ORIENTATION_FLIP_VERTICAL to listOf(4, 5, 2, 3, 0, 1),
            ExifInterface.ORIENTATION_TRANSPOSE to listOf(0, 2, 4, 1, 3, 5),
            ExifInterface.ORIENTATION_ROTATE_90 to listOf(4, 2, 0, 5, 3, 1),
            ExifInterface.ORIENTATION_TRANSVERSE to listOf(5, 3, 1, 4, 2, 0),
            ExifInterface.ORIENTATION_ROTATE_270 to listOf(1, 3, 5, 0, 2, 4),
        )
        try {
            expected.forEach { (orientation, indices) ->
                val transformed = orientCameraPhoto(source, orientation)
                try {
                    val actual = IntArray(6)
                    transformed.getPixels(actual, 0, transformed.width, 0, 0, transformed.width, transformed.height)
                    assertEquals("Orientation $orientation", indices.map { pixels[it] }, actual.toList())
                } finally {
                    if (transformed !== source) transformed.recycle()
                }
            }
        } finally {
            source.recycle()
        }
    }

    @Test
    fun burnInOverlayKeepsRoundedCornerOutsidePanelUntouched() {
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
        assertEquals(Color.WHITE, burnedIn.getPixel(20, 160))
    }

    @Test
    fun burnedInOverlayUsesSharedPanelRadiusAndWordmark() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/camera/CameraOverlayShareGenerator.kt").readText()

        assertTrue(source.contains("ExternalBrand.SHARE_CARD_PANEL_RADIUS_PX * scale"))
        assertTrue(source.contains("canvas.drawText(ExternalBrand.WORDMARK"))
        assertTrue(source.contains("canvas.drawRoundRect(rect, panelRadius, panelRadius, panelPaint)"))
        assertFalse(source.contains("for (y in top until bottom)"))
        assertFalse(source.contains("bitmap[x, y]"))
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
        assertEquals(
            shareUri,
            IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java),
        )
        assertTrue((intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
        assertEquals(shareUri, intent.clipData?.getItemAt(0)?.uri)
        assertEquals(480, decodedPng.width)
        assertEquals(320, decodedPng.height)
    }

    @Test
    fun photoShareIntentCleansCaptureAndPngWhenFileProviderFails() = runTest {
        val cacheDir = temporaryFolder.newFolder("provider-failure-cache")
        val context = cameraShareContext(cacheDir)
        val sourceFile = temporaryFolder.newFile("provider-failure-captured.jpg")
        FileOutputStream(sourceFile).use { output ->
            whiteBitmap(width = 480, height = 320).compress(Bitmap.CompressFormat.JPEG, 95, output)
        }
        mockkStatic(FileProvider::class)
        every {
            FileProvider.getUriForFile(context, "com.dbcheck.app.fileprovider", any())
        } throws IllegalArgumentException("Provider path unavailable")
        val generator = CameraOverlayShareGenerator(context, UnconfinedTestDispatcher())

        val error =
            runCatching {
                generator.createPhotoShareIntent(
                    sourcePhotoFile = sourceFile,
                    readout = CameraOverlayUiState(),
                )
            }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertFalse(sourceFile.exists())
        assertTrue(ExportFileCache.exportDirectory(cacheDir).listFiles().orEmpty().isEmpty())
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
