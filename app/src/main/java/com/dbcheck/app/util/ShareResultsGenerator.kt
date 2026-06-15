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
import com.dbcheck.app.R
import com.dbcheck.app.data.export.ExportFileCache
import com.dbcheck.app.di.IoDispatcher
import com.dbcheck.app.domain.report.SessionReportData
import com.dbcheck.app.domain.session.SessionMetadata
import com.dbcheck.app.util.ProductIdentity.FILE_NAME_PREFIX
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
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
            equivalentLevelLabel: String,
        ): Intent = withContext(ioDispatcher) {
                val duration = DurationFormatter.formatClockDuration(durationMs)
                val content =
                    buildSessionStatsShareContent(
                        text = context.getString(
                            R.string.share_meter_results_text,
                            ReportTextFormatter.oneDecimal(avgDb),
                            equivalentLevelLabel,
                            ReportTextFormatter.oneDecimal(peakDb),
                            duration,
                        ),
                    )

                Intent(content.action).apply {
                    type = content.type
                    putExtra(Intent.EXTRA_TEXT, content.text)
                }
            }

        suspend fun shareHearingTestResults(score: Int, rating: String): Intent = withContext(ioDispatcher) {
                val localizedRating = context.getString(hearingTestRatingStringRes(rating))
                val bitmap = generateShareCard(score, localizedRating)
                createImageShareIntent(
                    bitmap = bitmap,
                    fileName = "${FILE_NAME_PREFIX}_hearing_test_share.png",
                    title = context.getString(R.string.share_hearing_title),
                    text = context.getString(R.string.share_hearing_results_text, localizedRating, score),
                )
            }

        suspend fun shareSessionReportCard(report: SessionReportData): Intent = withContext(ioDispatcher) {
                val bitmap = generateSessionShareCard(report)
                createImageShareIntent(
                    bitmap = bitmap,
                    fileName = buildSessionReportShareFileName(report),
                    title = context.getString(R.string.share_session_report_title),
                    text =
                        buildSessionReportShareText(
                            context.getString(R.string.share_session_report_text),
                            report,
                        ),
                )
            }

        private fun generateShareCard(score: Int, rating: String): Bitmap {
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
            canvas.drawText(context.getString(R.string.share_hearing_card_title), 80f, 200f, titlePaint)

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

            val disclaimerPaint =
                Paint().apply {
                    color = 0xFFADAAAA.toInt()
                    textSize = 30f
                    isAntiAlias = true
                }
            drawShareText(
                canvas,
                context.getString(R.string.hearing_results_disclaimer),
                80f,
                760f,
                1000f,
                disclaimerPaint,
            )

            // Subtitle
            val subtitlePaint =
                Paint().apply {
                    color = 0xFF5F5F5F.toInt()
                    textSize = 36f
                    isAntiAlias = true
                }
            canvas.drawText(context.getString(R.string.share_hearing_card_subtitle), 80f, 900f, subtitlePaint)

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

            canvas.drawText(context.getString(R.string.share_session_report_card_title), 80f, 130f, titlePaint)
            drawShareText(canvas, report.shareTitle(), 80f, 182f, 1000f, labelPaint)
            if (report.sessionTags.isNotEmpty()) {
                drawShareText(canvas, report.tagLabel(), 80f, 224f, 1000f, labelPaint)
                canvas.drawText(report.dateLabel(), 80f, 266f, labelPaint)
            } else {
                canvas.drawText(report.dateLabel(), 80f, 224f, labelPaint)
            }

            canvas.drawText("${ReportTextFormatter.oneDecimal(report.laeqDb)} dB", 80f, 420f, valuePaint)
            canvas.drawText(report.equivalentLevelLabel, 86f, 470f, labelPaint)

            drawShareMetric(
                canvas,
                RectF(80f, 560f, 500f, 740f),
                context.getString(R.string.report_metric_lcpeak),
                "${ReportTextFormatter.oneDecimal(report.lcPeakDb)} dB",
                cardPaint,
                labelPaint,
                metricPaint,
            )
            drawShareMetric(
                canvas,
                RectF(580f, 560f, 1000f, 740f),
                context.getString(R.string.report_metric_twa),
                ReportTextFormatter.oneDecimalOrUnavailable(
                    report.twaDb,
                    " dB",
                    context.getString(R.string.value_unavailable),
                ),
                cardPaint,
                labelPaint,
                metricPaint,
            )
            drawShareMetric(
                canvas,
                RectF(80f, 780f, 500f, 960f),
                context.getString(R.string.report_metric_dose),
                ReportTextFormatter.oneDecimalOrUnavailable(
                    report.dosePercent,
                    "%",
                    context.getString(R.string.value_unavailable),
                ),
                cardPaint,
                labelPaint,
                metricPaint,
            )
            drawShareMetric(
                canvas,
                RectF(580f, 780f, 1000f, 960f),
                context.getString(R.string.report_metric_duration),
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
            drawShareText(
                canvas = canvas,
                text = value,
                x = rect.left + 32f,
                y = rect.top + 128f,
                maxRight = rect.right - 32f,
                paint = valuePaint,
            )
        }

        private fun drawShareText(canvas: Canvas, text: String, x: Float, y: Float, maxRight: Float, paint: Paint) {
            val maxWidth = (maxRight - x).coerceAtLeast(0f)
            canvas.drawText(
                ellipsizeShareText(text, maxWidth, paint::measureText),
                x,
                y,
                paint,
            )
        }

        private fun createImageShareIntent(bitmap: Bitmap, fileName: String, title: String, text: String): Intent {
            ExportFileCache.cleanupStaleFiles(context.cacheDir)
            val file = ExportFileCache.exportFile(context.cacheDir, fileName)
            FileOutputStream(file).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }

            val uri =
                FileProvider.getUriForFile(
                    context,
                    ExportFileCache.fileProviderAuthority(context),
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

        private fun sharePaint(color: Int, textSize: Float, bold: Boolean): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                this.textSize = textSize
                typeface =
                    Typeface.create(
                        "sans-serif",
                        if (bold) Typeface.BOLD else Typeface.NORMAL,
                    )
            }

        private fun SessionReportData.dateLabel(): String =
            ReportTextFormatter.dateTime(startTime, SESSION_REPORT_SHARE_DATE_PATTERN)

        private fun SessionReportData.shareTitle(): String = listOfNotNull(sessionEmoji, sessionName)
                .joinToString(separator = " ")

        private fun SessionReportData.tagLabel(): String = sessionTags.joinToString(separator = "  ") { "#$it" }

        private fun SessionReportData.durationLabel(): String = ReportTextFormatter.duration(durationMs)
    }

internal data class ShareTextContent(val action: String, val type: String, val text: String)

internal fun buildSessionStatsShareContent(text: String): ShareTextContent = ShareTextContent(
        action = Intent.ACTION_SEND,
        type = "text/plain",
        text = text,
    )

internal fun buildSessionReportShareText(template: String, report: SessionReportData): String = String.format(
        java.util.Locale.getDefault(),
        template,
        report.sessionName,
        ReportTextFormatter.oneDecimal(report.laeqDb),
        report.equivalentLevelLabel,
    )

internal fun buildSessionReportShareFileName(report: SessionReportData): String {
    val shortSlug =
        SessionMetadata
            .slugify(report.sessionName)
            .take(MAX_SESSION_REPORT_SLUG_LENGTH)
            .trim('-')
            .ifBlank { "session" }
    return "${FILE_NAME_PREFIX}_session_report_${report.sessionId}_$shortSlug.png"
}

internal fun ellipsizeShareText(text: String, maxWidth: Float, measureText: (String) -> Float): String =
    ellipsizeMeasuredText(text, maxWidth, measureText)

private const val MAX_SESSION_REPORT_SLUG_LENGTH = 48
private const val SESSION_REPORT_SHARE_DATE_PATTERN = "yyyy-MM-dd HH:mm"
