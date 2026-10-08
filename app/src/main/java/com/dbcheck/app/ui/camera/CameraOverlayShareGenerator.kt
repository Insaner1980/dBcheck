package com.dbcheck.app.ui.camera

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Matrix
import android.graphics.RectF
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.exifinterface.media.ExifInterface
import com.dbcheck.app.R
import com.dbcheck.app.data.export.ExportFileCache
import com.dbcheck.app.di.IoDispatcher
import com.dbcheck.app.util.ExternalBrand
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
        suspend fun createRawCaptureFile(nowMs: Long = System.currentTimeMillis()): File = withContext(ioDispatcher) {
            ExportFileCache.cleanupStaleFiles(context.cacheDir, nowMs = nowMs)
            ExportFileCache.exportFile(
                context.cacheDir,
                "${FILE_NAME_PREFIX}_camera_capture_raw_${cameraOverlayFileTimestamp(nowMs)}.jpg",
            )
        }

        suspend fun createSilentVideoFile(nowMs: Long = System.currentTimeMillis()): File = withContext(ioDispatcher) {
            ExportFileCache.cleanupStaleFiles(context.cacheDir, nowMs = nowMs)
            ExportFileCache.exportFile(
                context.cacheDir,
                "${FILE_NAME_PREFIX}_camera_silent_video_${cameraOverlayFileTimestamp(nowMs)}.mp4",
            )
        }

        suspend fun createPhotoShareIntent(sourcePhotoFile: File, readout: CameraOverlayUiState): Intent =
            withContext(ioDispatcher) {
                var outputFile: File? = null
                var published = false
                try {
                    val sourceBitmap =
                        decodeCameraPhoto(sourcePhotoFile)
                    try {
                        val outputBitmap =
                            burnCameraOverlayIntoBitmap(sourceBitmap, readout.toBurnInReadout(context), context)
                        try {
                            val createdOutputFile =
                                ExportFileCache.exportFile(
                                    context.cacheDir,
                                    "${FILE_NAME_PREFIX}_camera_overlay_${cameraOverlayFileTimestamp()}.png",
                                )
                            outputFile = createdOutputFile
                            FileOutputStream(createdOutputFile).use { output ->
                                outputBitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                            }
                            val shareIntent = createdOutputFile.toShareIntent()
                            deleteRawCaptureFile(sourcePhotoFile)
                            published = true
                            shareIntent
                        } finally {
                            outputBitmap.recycle()
                        }
                    } finally {
                        sourceBitmap.recycle()
                    }
                } finally {
                    if (!published) {
                        outputFile?.let(ExportFileCache::deleteExportFile)
                        deleteRawCaptureFile(sourcePhotoFile)
                    }
                }
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

internal fun decodeCameraPhoto(file: File): Bitmap {
    val orientation =
        ExifInterface(file.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, options)
    require(options.outWidth > 0 && options.outHeight > 0) { "Invalid captured photo dimensions" }
    options.inJustDecodeBounds = false
    options.inSampleSize = 1
    val longestSide = maxOf(options.outWidth, options.outHeight).toLong()
    while (longestSide > 2_048L * options.inSampleSize) {
        options.inSampleSize *= 2
    }
    val decoded = BitmapFactory.decodeFile(file.absolutePath, options)
        ?: error("Unable to decode captured camera overlay photo")
    var keepDecoded = false
    try {
        val oriented = orientCameraPhoto(decoded, orientation)
        keepDecoded = oriented === decoded
        return oriented
    } finally {
        if (!keepDecoded) decoded.recycle()
    }
}

internal fun orientCameraPhoto(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                setRotate(90f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                setRotate(270f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
            else -> return bitmap
        }
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

internal fun burnCameraOverlayIntoBitmap(
    source: Bitmap,
    readout: CameraOverlayBurnInReadout,
    context: Context? = null,
): Bitmap {
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
    val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xE9000000.toInt() }
    val panelRadius = ExternalBrand.SHARE_CARD_PANEL_RADIUS_PX * scale
    canvas.drawRoundRect(rect, panelRadius, panelRadius, panelPaint)

    val statusPaint =
        cameraBurnInTextPaint(
            context = context,
            color = 0xFFF7F7F7.toInt(),
            textSize = 18f * scale,
            semibold = true,
        )
    val dbPaint =
        cameraBurnInTextPaint(
            context = context,
            color = 0xFFFFFFFF.toInt(),
            textSize = 46f * scale,
            semibold = true,
            spaceGrotesk = true,
        )
    val labelPaint =
        cameraBurnInTextPaint(
            context = context,
            color = 0xFFE2E5E1.toInt(),
            textSize = 20f * scale,
        )
    val timestampPaint =
        cameraBurnInTextPaint(
            context = context,
            color = 0xFFC8D0CA.toInt(),
            textSize = 16f * scale,
        )
    val textX = rect.left + padding
    val wordmarkPaint =
        cameraBurnInTextPaint(
            context = context,
            color = 0xFFC8D0CA.toInt(),
            textSize = 16f * scale,
            semibold = true,
            align = Paint.Align.RIGHT,
        )

    canvas.drawText(readout.status, textX, rect.top + 34f * scale, statusPaint)
    canvas.drawText(ExternalBrand.WORDMARK, rect.right - padding, rect.top + 34f * scale, wordmarkPaint)
    canvas.drawText(readout.dbText, textX, rect.top + 86f * scale, dbPaint)
    canvas.drawText(readout.levelLabel, textX, rect.top + 114f * scale, labelPaint)
    canvas.drawText(readout.timestampText, textX, rect.top + 136f * scale, timestampPaint)

    return output
}

private fun cameraBurnInTextPaint(
    context: Context?,
    color: Int,
    textSize: Float,
    semibold: Boolean = false,
    spaceGrotesk: Boolean = false,
    align: Paint.Align = Paint.Align.LEFT,
): Paint = context?.let {
        if (spaceGrotesk) {
            ExternalBrand.spaceGroteskPaint(context = it, color = color, textSize = textSize, align = align)
        } else {
            ExternalBrand.manropePaint(
                context = it,
                color = color,
                textSize = textSize,
                semibold = semibold,
                align = align,
            )
        }
    } ?: sansSerifPaint(color = color, textSize = textSize, bold = semibold || spaceGrotesk).apply {
        textAlign = align
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
