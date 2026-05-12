package com.dbcheck.app.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.res.ResourcesCompat
import com.dbcheck.app.BuildConfig
import com.dbcheck.app.R
import com.dbcheck.app.di.IoDispatcher
import com.dbcheck.app.domain.noise.NoiseLevel
import com.dbcheck.app.domain.report.PeakEvent
import com.dbcheck.app.domain.report.SessionReportData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class ExportPdfReportUseCase
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        suspend fun export(
            report: SessionReportData,
            outputUri: Uri,
        ) = withContext(ioDispatcher) {
            val document = PdfDocument()
            try {
                val style = PdfReportStyle(context)
                drawSummaryPage(document, report, style)
                drawMetricsPage(document, report, style)
                drawTimeSeriesPage(document, report, style)
                drawPeakEventsPage(document, report, style)

                val outputStream =
                    context.contentResolver.openOutputStream(outputUri)
                        ?: throw IOException("Unable to open PDF output stream")
                outputStream.use { document.writeTo(it) }
            } finally {
                document.close()
            }
        }

        private fun drawSummaryPage(
            document: PdfDocument,
            report: SessionReportData,
            style: PdfReportStyle,
        ) = drawPage(document, style, pageNumber = 1, title = "Session Summary") { canvas ->
            var y = PAGE_TOP
            y = drawTitleBlock(canvas, report, style, y)
            y += 18f
            drawKpiGrid(canvas, report, style, y)
            drawNote(
                canvas = canvas,
                text = "NIOSH reference: 85 dBA as an 8-hour TWA with a 3 dB exchange rate.",
                style = style,
                top = 650f,
            )
        }

        private fun drawMetricsPage(
            document: PdfDocument,
            report: SessionReportData,
            style: PdfReportStyle,
        ) = drawPage(document, style, pageNumber = 2, title = "Scientific Metrics") { canvas ->
            val rows =
                listOf(
                    "LAeq" to "${report.laeqDb.formatDb()} dB",
                    "LCpeak" to "${report.lcPeakDb.formatDb()} dB",
                    "8-hour TWA" to "${report.twaDb.formatDb()} dB",
                    "NIOSH dose" to "${report.dosePercent.formatDb()}%",
                    "Weighting" to report.weighting,
                    "Samples" to report.measurementCount.toString(),
                    "Duration" to report.durationLabel(),
                )
            drawMetricTable(canvas, rows, style, PAGE_TOP)
        }

        private fun drawTimeSeriesPage(
            document: PdfDocument,
            report: SessionReportData,
            style: PdfReportStyle,
        ) = drawPage(document, style, pageNumber = 3, title = "Time Series") { canvas ->
            canvas.drawText("Weighted sound level over session", PAGE_LEFT, PAGE_TOP, style.sectionPaint)
            val chartRect = RectF(PAGE_LEFT, PAGE_TOP + 36f, PAGE_RIGHT, 540f)
            PdfChartRenderer.drawTimeSeries(
                canvas = canvas,
                points = report.timeSeries,
                rect = chartRect,
                minDb = report.minDb.coerceAtMost(NoiseLevel.QUIET.maxDb),
                maxDb = report.maxDb.coerceAtLeast(100f),
            )
            drawTimeRangeLabels(canvas, report, chartRect, style)
        }

        private fun drawPeakEventsPage(
            document: PdfDocument,
            report: SessionReportData,
            style: PdfReportStyle,
        ) = drawPage(document, style, pageNumber = 4, title = "Peak Events") { canvas ->
            if (report.peakEvents.isEmpty()) {
                drawNote(canvas, "No events at or above 85 dBA were detected.", style, PAGE_TOP)
            } else {
                drawPeakEvents(canvas, report.peakEvents.take(MAX_PEAK_EVENTS), style)
            }
        }

        private fun drawPage(
            document: PdfDocument,
            style: PdfReportStyle,
            pageNumber: Int,
            title: String,
            content: (Canvas) -> Unit,
        ) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)
            drawHeader(canvas, title, style)
            content(canvas)
            drawFooter(canvas, pageNumber, style)
            document.finishPage(page)
        }

        private fun drawHeader(
            canvas: Canvas,
            title: String,
            style: PdfReportStyle,
        ) {
            canvas.drawText("dBcheck", PAGE_LEFT, 46f, style.brandPaint)
            canvas.drawText(title, PAGE_RIGHT, 46f, style.headerPaint)
            canvas.drawLine(PAGE_LEFT, 62f, PAGE_RIGHT, 62f, style.dividerPaint)
        }

        private fun drawFooter(
            canvas: Canvas,
            pageNumber: Int,
            style: PdfReportStyle,
        ) {
            val footer =
                "Generated by dBcheck v${BuildConfig.VERSION_NAME} - " +
                    "Not a calibrated Class 1/2 instrument unless used with verified external microphone"
            canvas.drawLine(PAGE_LEFT, 780f, PAGE_RIGHT, 780f, style.dividerPaint)
            canvas.drawText(footer, PAGE_LEFT, 804f, style.footerPaint)
            canvas.drawText("Page $pageNumber / 4", PAGE_RIGHT, 804f, style.footerRightPaint)
        }

        private fun drawTitleBlock(
            canvas: Canvas,
            report: SessionReportData,
            style: PdfReportStyle,
            top: Float,
        ): Float {
            val title =
                listOfNotNull(report.sessionEmoji, report.sessionName)
                    .joinToString(separator = " ")
            canvas.drawText(title, PAGE_LEFT, top, style.titlePaint)

            var y = top + 28f
            if (report.sessionTags.isNotEmpty()) {
                val tags = report.sessionTags.joinToString(separator = "  ") { "#$it" }
                canvas.drawText(tags, PAGE_LEFT, y, style.bodyPaint)
                y += 22f
            }
            canvas.drawText(report.dateRangeLabel(), PAGE_LEFT, y, style.bodyPaint)
            canvas.drawText("Duration ${report.durationLabel()}", PAGE_LEFT, y + 22f, style.bodyPaint)
            return y + 36f
        }

        private fun drawKpiGrid(
            canvas: Canvas,
            report: SessionReportData,
            style: PdfReportStyle,
            top: Float,
        ) {
            val cards =
                listOf(
                    Kpi("LAeq", "${report.laeqDb.formatDb()} dB"),
                    Kpi("LCpeak", "${report.lcPeakDb.formatDb()} dB"),
                    Kpi("TWA", "${report.twaDb.formatDb()} dB"),
                    Kpi("Dose", "${report.dosePercent.formatDb()}%"),
                )
            cards.forEachIndexed { index, kpi ->
                val row = index / 2
                val col = index % 2
                val left = PAGE_LEFT + col * (KPI_WIDTH + 20f)
                val rect = RectF(left, top + row * 130f, left + KPI_WIDTH, top + row * 130f + 104f)
                drawKpiCard(canvas, rect, kpi, style)
            }
        }

        private fun drawKpiCard(
            canvas: Canvas,
            rect: RectF,
            kpi: Kpi,
            style: PdfReportStyle,
        ) {
            canvas.drawRoundRect(rect, 14f, 14f, style.cardPaint)
            canvas.drawText(kpi.label, rect.left + 18f, rect.top + 32f, style.kpiLabelPaint)
            canvas.drawText(kpi.value, rect.left + 18f, rect.top + 78f, style.kpiValuePaint)
        }

        private fun drawMetricTable(
            canvas: Canvas,
            rows: List<Pair<String, String>>,
            style: PdfReportStyle,
            top: Float,
        ) {
            var y = top
            rows.forEach { (label, value) ->
                canvas.drawText(label, PAGE_LEFT, y, style.tableLabelPaint)
                canvas.drawText(value, PAGE_RIGHT, y, style.tableValuePaint)
                y += 42f
            }
        }

        private fun drawTimeRangeLabels(
            canvas: Canvas,
            report: SessionReportData,
            chartRect: RectF,
            style: PdfReportStyle,
        ) {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            canvas.drawText(
                timeFormat.format(Date(report.startTime)),
                chartRect.left,
                chartRect.bottom + 24f,
                style.axisPaint,
            )
            canvas.drawText(
                timeFormat.format(Date(report.endTime)),
                chartRect.right,
                chartRect.bottom + 24f,
                style.axisRightPaint,
            )
        }

        private fun drawPeakEvents(
            canvas: Canvas,
            events: List<PeakEvent>,
            style: PdfReportStyle,
        ) {
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            var y = PAGE_TOP
            events.forEachIndexed { index, event ->
                canvas.drawText("${index + 1}. ${event.maxDb.formatDb()} dB", PAGE_LEFT, y, style.sectionPaint)
                canvas.drawText(
                    "${timeFormat.format(Date(event.startTime))} - ${timeFormat.format(Date(event.endTime))}",
                    PAGE_LEFT,
                    y + 24f,
                    style.bodyPaint,
                )
                canvas.drawText(
                    "Peak at ${timeFormat.format(Date(event.peakTime))}",
                    PAGE_LEFT,
                    y + 46f,
                    style.bodyPaint,
                )
                y += 78f
            }
        }

        private fun drawNote(
            canvas: Canvas,
            text: String,
            style: PdfReportStyle,
            top: Float,
        ) {
            canvas.drawRoundRect(RectF(PAGE_LEFT, top, PAGE_RIGHT, top + 76f), 14f, 14f, style.notePaint)
            canvas.drawText(text, PAGE_LEFT + 18f, top + 44f, style.bodyPaint)
        }

        private fun SessionReportData.dateRangeLabel(): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            return "${dateFormat.format(Date(startTime))} - ${dateFormat.format(Date(endTime))}"
        }

        private fun SessionReportData.durationLabel(): String = DurationFormatter.formatClockDuration(durationMs)

        private fun Float.formatDb(): String = "%.1f".format(Locale.US, this)

        private data class Kpi(
            val label: String,
            val value: String,
        )

        private companion object {
            const val PAGE_WIDTH = 595
            const val PAGE_HEIGHT = 842
            const val PAGE_LEFT = 48f
            const val PAGE_RIGHT = 547f
            const val PAGE_TOP = 112f
            const val KPI_WIDTH = 239.5f
            const val MAX_PEAK_EVENTS = 8
        }
    }

private class PdfReportStyle(context: Context) {
    private val manropeRegular = font(context, R.font.manrope_regular, Typeface.NORMAL)
    private val manropeSemiBold = font(context, R.font.manrope_semibold, Typeface.BOLD)
    private val spaceBold = font(context, R.font.space_grotesk_bold, Typeface.BOLD)
    private val spaceSemiBold = font(context, R.font.space_grotesk_semibold, Typeface.BOLD)

    val brandPaint = paint(size = 18f, color = PdfPalette.Primary, typeface = spaceBold)
    val headerPaint =
        paint(size = 12f, color = PdfPalette.TextMuted, typeface = manropeSemiBold, align = Paint.Align.RIGHT)
    val titlePaint = paint(size = 28f, color = PdfPalette.Text, typeface = manropeSemiBold)
    val sectionPaint = paint(size = 18f, color = PdfPalette.Text, typeface = manropeSemiBold)
    val bodyPaint = paint(size = 12f, color = PdfPalette.TextMuted, typeface = manropeRegular)
    val footerPaint = paint(size = 8f, color = PdfPalette.TextMuted, typeface = manropeRegular)
    val footerRightPaint =
        paint(size = 8f, color = PdfPalette.TextMuted, typeface = manropeRegular, align = Paint.Align.RIGHT)
    val dividerPaint = paint(size = 1f, color = PdfPalette.Divider, typeface = manropeRegular)
    val cardPaint = paint(size = 1f, color = PdfPalette.Card, typeface = manropeRegular, style = Paint.Style.FILL)
    val notePaint = paint(size = 1f, color = PdfPalette.Note, typeface = manropeRegular, style = Paint.Style.FILL)
    val kpiLabelPaint = paint(size = 11f, color = PdfPalette.TextMuted, typeface = spaceSemiBold)
    val kpiValuePaint = paint(size = 30f, color = PdfPalette.Text, typeface = spaceBold)
    val tableLabelPaint = paint(size = 13f, color = PdfPalette.TextMuted, typeface = manropeRegular)
    val tableValuePaint =
        paint(size = 15f, color = PdfPalette.Text, typeface = spaceSemiBold, align = Paint.Align.RIGHT)
    val axisPaint = paint(size = 10f, color = PdfPalette.TextMuted, typeface = manropeRegular)
    val axisRightPaint =
        paint(size = 10f, color = PdfPalette.TextMuted, typeface = manropeRegular, align = Paint.Align.RIGHT)

    private fun font(
        context: Context,
        id: Int,
        fallbackStyle: Int,
    ): Typeface = ResourcesCompat.getFont(context, id) ?: Typeface.create(Typeface.SANS_SERIF, fallbackStyle)

    private fun paint(
        size: Float,
        color: Int,
        typeface: Typeface,
        align: Paint.Align = Paint.Align.LEFT,
        style: Paint.Style = Paint.Style.FILL,
    ): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            this.typeface = typeface
            textAlign = align
            this.style = style
        }
}

private object PdfPalette {
    val Primary: Int = Color.rgb(70, 105, 6)
    val Text: Int = Color.rgb(47, 51, 52)
    val TextMuted: Int = Color.rgb(92, 96, 96)
    val Card: Int = Color.rgb(236, 238, 238)
    val Note: Int = Color.rgb(243, 244, 244)
    val Divider: Int = Color.rgb(216, 219, 220)
}
