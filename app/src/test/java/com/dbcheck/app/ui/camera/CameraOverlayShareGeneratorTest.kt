package com.dbcheck.app.ui.camera

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import com.dbcheck.app.R
import com.dbcheck.app.data.export.ExportFileCache
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
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
    fun silentVideoFileUsesMp4ExportCacheName() {
        val cacheDir = temporaryFolder.newFolder("cache")
        val context = cameraShareContext(cacheDir)
        val generator = CameraOverlayShareGenerator(context, UnconfinedTestDispatcher())

        val outputFile = generator.createSilentVideoFile(nowMs = 1_700_000_000_000L)

        assertEquals(ExportFileCache.exportDirectory(cacheDir), outputFile.parentFile)
        assertTrue(outputFile.name.startsWith("dBcheck_camera_silent_video_"))
        assertTrue(outputFile.name.endsWith(".mp4"))
    }

    private fun cameraShareContext(cacheDir: File): Context {
        val contentResolver = mockk<ContentResolver>(relaxed = true)
        return mockk {
            every { this@mockk.cacheDir } returns cacheDir
            every { packageName } returns "com.dbcheck.app"
            every { this@mockk.contentResolver } returns contentResolver
            every { getString(R.string.camera_overlay_share_clip_label) } returns "dBcheck camera overlay"
            every { getString(R.string.camera_overlay_share_text) } returns "dBcheck camera overlay"
            every { getString(R.string.camera_overlay_status_live) } returns "LIVE"
            every { getString(R.string.camera_overlay_status_ready) } returns "READY"
            every { getString(R.string.camera_overlay_db_value, 74) } returns "74 dB"
            every { getString(R.string.camera_overlay_db_unavailable) } returns "-- dB"
            every { getString(R.string.camera_overlay_timestamp_value, any<String>()) } answers {
                val formatArgs = secondArg<Array<Any>>()
                "Updated ${formatArgs.single()}"
            }
            every { getString(R.string.camera_overlay_timestamp_unavailable) } returns "No live reading"
        }
    }

    private fun whiteBitmap(width: Int, height: Int): Bitmap = createBitmap(width, height).apply {
            eraseColor(Color.WHITE)
        }
}
