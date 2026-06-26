package com.dbcheck.app.ui.camera

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.graphics.set
import com.dbcheck.app.R
import com.dbcheck.app.data.export.ExportFileCache
import com.dbcheck.app.di.IoDispatcher
import com.dbcheck.app.util.ProductIdentity.FILE_NAME_PREFIX
import com.dbcheck.app.util.sansSerifPaint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.roundToInt

class CameraOverlayShareGenerator
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        fun createRawCaptureFile(nowMs: Long = System.currentTimeMillis()): File {
            ExportFileCache.cleanupStaleFiles(context.cacheDir, nowMs = nowMs)
            return ExportFileCache.exportFile(
                context.cacheDir,
                "${FILE_NAME_PREFIX}_camera_capture_raw_${cameraOverlayFileTimestamp(nowMs)}.jpg",
            )
        }

        fun createSilentVideoFile(nowMs: Long = System.currentTimeMillis()): File {
            ExportFileCache.cleanupStaleFiles(context.cacheDir, nowMs = nowMs)
            return ExportFileCache.exportFile(
                context.cacheDir,
                "${FILE_NAME_PREFIX}_camera_silent_video_${cameraOverlayFileTimestamp(nowMs)}.mp4",
            )
        }

        suspend fun createPhotoShareIntent(sourcePhotoFile: File, readout: CameraOverlayUiState): Intent =
            withContext(ioDispatcher) {
                val sourceBitmap =
                    BitmapFactory.decodeFile(sourcePhotoFile.absolutePath)
                        ?: error("Unable to decode captured camera overlay photo")
                val outputBitmap = burnCameraOverlayIntoBitmap(sourceBitmap, readout.toBurnInReadout(context))
                val outputFile =
                    ExportFileCache.exportFile(
                        context.cacheDir,
                        "${FILE_NAME_PREFIX}_camera_overlay_${cameraOverlayFileTimestamp()}.png",
                    )
                FileOutputStream(outputFile).use { output ->
                    outputBitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                }
                deleteRawCaptureFile(sourcePhotoFile)
                outputFile.toShareIntent()
            }

        private fun File.toShareIntent(): Intent {
            val uri =
                FileProvider.getUriForFile(
                    context,
                    ExportFileCache.fileProviderAuthority(context),
                    this,
                )
            val title = context.getString(R.string.camera_overlay_share_clip_label)
            return Intent(Intent.ACTION_SEND).apply {
                setDataAndType(uri, "image/png")
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, context.getString(R.string.camera_overlay_share_text))
                clipData = ClipData.newUri(context.contentResolver, title, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        private fun deleteRawCaptureFile(file: File) {
            if (!file.delete() && file.exists()) {
                file.deleteOnExit()
            }
        }
    }

internal data class CameraOverlayBurnInReadout(
    val status: String,
    val dbText: String,
    val levelLabel: String,
    val timestampText: String,
)

internal fun burnCameraOverlayIntoBitmap(source: Bitmap, readout: CameraOverlayBurnInReadout): Bitmap {
    val output = createBitmap(source.width, source.height)
    val canvas = Canvas(output)
    canvas.drawBitmap(source, 0f, 0f, null)

    val minDimension = min(source.width, source.height).toFloat()
    val scale = (minDimension / 360f).coerceAtLeast(0.8f)
    val margin = 24f * scale
    val padding = 20f * scale
    val panelWidth = min(source.width - margin * 2f, 320f * scale)
    val panelHeight = 144f * scale
    val rect =
        RectF(
            margin,
            source.height - margin - panelHeight,
            margin + panelWidth,
            source.height - margin,
        )
    applyCameraOverlayPanelScrim(output, rect)
    val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xB0000000.toInt() }
    canvas.drawRoundRect(rect, 18f * scale, 18f * scale, panelPaint)

    val statusPaint = sansSerifPaint(color = 0xFFF7F7F7.toInt(), textSize = 18f * scale, bold = true)
    val dbPaint = sansSerifPaint(color = 0xFFFFFFFF.toInt(), textSize = 46f * scale, bold = true)
    val labelPaint = sansSerifPaint(color = 0xFFE2E5E1.toInt(), textSize = 20f * scale, bold = false)
    val timestampPaint = sansSerifPaint(color = 0xFFC8D0CA.toInt(), textSize = 16f * scale, bold = false)
    val textX = rect.left + padding

    canvas.drawText(readout.status, textX, rect.top + 34f * scale, statusPaint)
    canvas.drawText(readout.dbText, textX, rect.top + 86f * scale, dbPaint)
    canvas.drawText(readout.levelLabel, textX, rect.top + 114f * scale, labelPaint)
    canvas.drawText(readout.timestampText, textX, rect.top + 136f * scale, timestampPaint)

    return output
}

private fun applyCameraOverlayPanelScrim(bitmap: Bitmap, rect: RectF) {
    val left = rect.left.toInt().coerceIn(0, bitmap.width)
    val right = rect.right.toInt().coerceIn(left, bitmap.width)
    val top = rect.top.toInt().coerceIn(0, bitmap.height)
    val bottom = rect.bottom.toInt().coerceIn(top, bitmap.height)
    for (y in top until bottom) {
        for (x in left until right) {
            bitmap[x, y] = darkenForCameraOverlay(bitmap[x, y])
        }
    }
}

private fun darkenForCameraOverlay(color: Int): Int {
    val retained = 0.28f
    return Color.argb(
        Color.alpha(color).coerceAtLeast(255),
        (Color.red(color) * retained).roundToInt(),
        (Color.green(color) * retained).roundToInt(),
        (Color.blue(color) * retained).roundToInt(),
    )
}

internal fun CameraOverlayUiState.toBurnInReadout(context: Context): CameraOverlayBurnInReadout {
    val status =
        context.getString(
            when (status) {
                CameraOverlayReadoutStatus.READY -> R.string.camera_overlay_status_ready
                CameraOverlayReadoutStatus.LIVE -> R.string.camera_overlay_status_live
            },
        )
    val dbText =
        currentDb?.roundToInt()?.let {
            context.getString(R.string.camera_overlay_db_value, it)
        } ?: context.getString(R.string.camera_overlay_db_unavailable)
    val timestampText =
        timestampMs?.let {
            context.getString(R.string.camera_overlay_timestamp_value, formatCameraOverlayTimestamp(it))
        } ?: context.getString(R.string.camera_overlay_timestamp_unavailable)
    return CameraOverlayBurnInReadout(
        status = status,
        dbText = dbText,
        levelLabel = levelLabel,
        timestampText = timestampText,
    )
}

internal fun formatCameraOverlayTimestamp(timestampMs: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestampMs))

private fun cameraOverlayFileTimestamp(nowMs: Long = System.currentTimeMillis()): String =
    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(nowMs))
