package com.dbcheck.app.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import com.dbcheck.app.data.export.ExportFileCache
import com.dbcheck.app.di.IoDispatcher
import com.dbcheck.app.domain.report.SessionReportData
import com.dbcheck.app.domain.session.SessionMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class ShareResultsGenerator
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        suspend fun shareSessionStats(
            avgDb: Float,
            peakDb: Float,
            durationMs: Long,
        ): Intent =
            withContext(ioDispatcher) {
                val content = buildSessionStatsShareContent(avgDb, peakDb, durationMs)

                Intent(content.action).apply {
                    type = content.type
                    putExtra(Intent.EXTRA_TEXT, content.text)
                }
            }

        suspend fun shareHearingTestResults(
            score: Int,
            rating: String,
        ): Intent =
            withContext(ioDispatcher) {
                val bitmap = generateShareCard(score, rating)
                createImageShareIntent(
                    bitmap = bitmap,
                    fileName = "hearing_test_share.png",
                    title = "dBcheck hearing test",
                    text = "My hearing test result: $rating ($score/100) - tested with dBcheck \uD83C\uDFA7",
                )
            }

        suspend fun shareSessionReportCard(report: SessionReportData): Intent =
            withContext(ioDispatcher) {
                val bitmap = generateSessionShareCard(report)
                createImageShareIntent(
                    bitmap = bitmap,
                    fileName = "session_report_${SessionMetadata.slugify(report.sessionName)}.png",
                    title = "dBcheck session report",
                    text = "dBcheck session report for ${report.sessionName}: ${report.laeqDb.formatOne()} dB LAeq",
                )
            }

        private fun generateShareCard(
            score: Int,
            rating: String,
        ): Bitmap {
            val width = 1080
            val height = 1080
            val bitmap = createBitmap(width, height)
            val canvas = Canvas(bitmap)

            // Background
            val bgPaint = Paint().apply { color = 0xFF0E0E0E.toInt() }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // Title
            val titlePaint =
                Paint().apply {
                    color = 0xFFE8E4E0.toInt()
                    textSize = 48f
                    isAntiAlias = true
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                }
            canvas.drawText("dBcheck Hearing Test", 80f, 200f, titlePaint)

            // Score
            val scorePaint =
                Paint().apply {
                    color = 0xFFC5FE00.toInt()
                    textSize = 180f
                    isAntiAlias = true
                    typeface = Typeface.create("sans-serif", Typeface.BOLD)
                }
            canvas.drawText("$score", 80f, 500f, scorePaint)

            // Rating
            val ratingPaint =
                Paint().apply {
                    color = 0xFFADAAAA.toInt()
                    textSize = 72f
                    isAntiAlias = true
                }
            canvas.drawText(rating, 80f, 600f, ratingPaint)

            // Subtitle
            val subtitlePaint =
                Paint().apply {
                    color = 0xFF5F5F5F.toInt()
                    textSize = 36f
                    isAntiAlias = true
                }
            canvas.drawText("Tested with dBcheck", 80f, 900f, subtitlePaint)

            return bitmap
        }

        private fun generateSessionShareCard(report: SessionReportData): Bitmap {
            val width = 1080
            val height = 1080
            val bitmap = createBitmap(width, height)
            val canvas = Canvas(bitmap)

            val backgroundPaint = Paint().apply { color = 0xFFF9F9F9.toInt() }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

            val titlePaint = sharePaint(color = 0xFF2F3334.toInt(), textSize = 52f, bold = true)
            val labelPaint = sharePaint(color = 0xFF5C6060.toInt(), textSize = 28f, bold = false)
            val valuePaint = sharePaint(color = 0xFF466906.toInt(), textSize = 132f, bold = true)
            val metricPaint = sharePaint(color = 0xFF2F3334.toInt(), textSize = 44f, bold = true)
            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFECEEEE.toInt() }

            canvas.drawText("dBcheck Session Report", 80f, 130f, titlePaint)
            canvas.drawText(report.shareTitle(), 80f, 182f, labelPaint)
            if (report.sessionTags.isNotEmpty()) {
                canvas.drawText(report.tagLabel(), 80f, 224f, labelPaint)
                canvas.drawText(report.dateLabel(), 80f, 266f, labelPaint)
            } else {
                canvas.drawText(report.dateLabel(), 80f, 224f, labelPaint)
            }

            canvas.drawText("${report.laeqDb.formatOne()} dB", 80f, 420f, valuePaint)
            canvas.drawText("LAeq", 86f, 470f, labelPaint)

            drawShareMetric(
                canvas,
                RectF(80f, 560f, 500f, 740f),
                "LCpeak",
                "${report.lcPeakDb.formatOne()} dB",
                cardPaint,
                labelPaint,
                metricPaint,
            )
            drawShareMetric(
                canvas,
                RectF(580f, 560f, 1000f, 740f),
                "TWA",
                "${report.twaDb.formatOne()} dB",
                cardPaint,
                labelPaint,
                metricPaint,
            )
            drawShareMetric(
                canvas,
                RectF(80f, 780f, 500f, 960f),
                "Dose",
                "${report.dosePercent.formatOne()}%",
                cardPaint,
                labelPaint,
                metricPaint,
            )
            drawShareMetric(
                canvas,
                RectF(580f, 780f, 1000f, 960f),
                "Duration",
                report.durationLabel(),
                cardPaint,
                labelPaint,
                metricPaint,
            )

            return bitmap
        }

        private fun drawShareMetric(
            canvas: Canvas,
            rect: RectF,
            label: String,
            value: String,
            cardPaint: Paint,
            labelPaint: Paint,
            valuePaint: Paint,
        ) {
            canvas.drawRoundRect(rect, 28f, 28f, cardPaint)
            canvas.drawText(label, rect.left + 32f, rect.top + 54f, labelPaint)
            canvas.drawText(value, rect.left + 32f, rect.top + 128f, valuePaint)
        }

        private fun createImageShareIntent(
            bitmap: Bitmap,
            fileName: String,
            title: String,
            text: String,
        ): Intent {
            ExportFileCache.cleanupStaleFiles(context.cacheDir)
            val file = ExportFileCache.exportFile(context.cacheDir, fileName)
            FileOutputStream(file).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }

            val uri =
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )

            return Intent(Intent.ACTION_SEND).apply {
                setDataAndType(uri, "image/png")
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, text)
                clipData = ClipData.newUri(context.contentResolver, title, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        private fun sharePaint(
            color: Int,
            textSize: Float,
            bold: Boolean,
        ): Paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                this.textSize = textSize
                typeface =
                    Typeface.create(
                        "sans-serif",
                        if (bold) Typeface.BOLD else Typeface.NORMAL,
                    )
            }

        private fun SessionReportData.dateLabel(): String =
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(startTime))

        private fun SessionReportData.shareTitle(): String =
            listOfNotNull(sessionEmoji, sessionName)
                .joinToString(separator = " ")

        private fun SessionReportData.tagLabel(): String =
            sessionTags.joinToString(separator = "  ") { "#$it" }

        private fun SessionReportData.durationLabel(): String = DurationFormatter.formatClockDuration(durationMs)

        private fun Float.formatOne(): String = "%.1f".format(Locale.US, this)
    }

internal data class ShareTextContent(
    val action: String,
    val type: String,
    val text: String,
)

internal fun buildSessionStatsShareContent(
    avgDb: Float,
    peakDb: Float,
    durationMs: Long,
): ShareTextContent {
    val duration = DurationFormatter.formatClockDuration(durationMs)
    val text =
        "I measured ${avgDb.toInt()} dB avg " +
            "(peak: ${peakDb.toInt()} dB) " +
            "in my $duration session with dBcheck \uD83D\uDD0A"

    return ShareTextContent(
        action = Intent.ACTION_SEND,
        type = "text/plain",
        text = text,
    )
}
