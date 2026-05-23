package com.dbcheck.app.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import com.dbcheck.app.BuildConfig
import com.dbcheck.app.R
import com.dbcheck.app.di.IoDispatcher
import com.dbcheck.app.domain.noise.NoiseLevel
import com.dbcheck.app.domain.report.PeakEvent
import com.dbcheck.app.domain.report.ReportHeartRateSection
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
            heartRate: ReportHeartRateSection = ReportHeartRateSection(),
        ) = withContext(ioDispatcher) {
            val document = PdfDocument()
            try {
                val style = PdfReportStyle(context)
                val pageCount = if (heartRate.enabled) HEART_RATE_PAGE_COUNT else BASE_PAGE_COUNT
                drawSummaryPage(document, report, style, pageCount)
                drawMetricsPage(document, report, style, pageCount)
                drawTimeSeriesPage(document, report, style, pageCount)
                drawPeakEventsPage(document, report, style, pageCount)
                if (heartRate.enabled) {
                    drawHeartRatePage(document, report, heartRate, style, pageCount)
                }

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
            pageCount: Int,
        ) = drawPage(
            document,
            style,
            pageNumber = 1,
            pageCount = pageCount,
            title = string(R.string.report_section_session_summary),
        ) { canvas ->
            var y = PAGE_TOP
            y = drawTitleBlock(canvas, report, style, y)
            y += 18f
            drawKpiGrid(canvas, report, style, y)
            drawNote(
                canvas = canvas,
                text = report.nioshNote(),
                style = style,
                top = 650f,
            )
        }

        private fun drawMetricsPage(
            document: PdfDocument,
            report: SessionReportData,
            style: PdfReportStyle,
            pageCount: Int,
        ) = drawPage(
            document,
            style,
            pageNumber = 2,
            pageCount = pageCount,
            title = string(R.string.report_section_scientific_metrics),
        ) { canvas ->
            val rows =
                listOf(
                    report.equivalentLevelLabel to "${ReportTextFormatter.oneDecimal(report.laeqDb)} dB",
                    string(R.string.report_metric_lcpeak) to "${ReportTextFormatter.oneDecimal(report.lcPeakDb)} dB",
                    string(R.string.report_8_hour_twa) to
                        ReportTextFormatter.oneDecimalOrUnavailable(
                            report.twaDb,
                            " dB",
                            string(R.string.value_unavailable),
                        ),
                    string(R.string.report_niosh_dose) to
                        ReportTextFormatter.oneDecimalOrUnavailable(
                            report.dosePercent,
                            "%",
                            string(R.string.value_unavailable),
                        ),
                    string(R.string.report_metric_weighting) to report.weighting,
                    string(R.string.report_metric_samples) to report.measurementCount.toString(),
                    string(R.string.report_metric_duration) to report.durationLabel(),
                )
            drawMetricTable(canvas, rows, style, PAGE_TOP)
        }

        private fun drawTimeSeriesPage(
            document: PdfDocument,
            report: SessionReportData,
            style: PdfReportStyle,
            pageCount: Int,
        ) = drawPage(
            document,
            style,
            pageNumber = 3,
            pageCount = pageCount,
            title = string(R.string.report_section_time_series),
        ) { canvas ->
            canvas.drawText(string(R.string.report_time_series_title), PAGE_LEFT, PAGE_TOP, style.sectionPaint)
            val chartRect = RectF(PAGE_LEFT, PAGE_TOP + 36f, PAGE_RIGHT, 540f)
            PdfChartRenderer.drawTimeSeries(
                canvas = canvas,
                points = report.timeSeries,
                rect = chartRect,
                minDb = report.minDb.coerceAtMost(NoiseLevel.QUIET.maxDb),
                maxDb = report.maxDb.coerceAtLeast(100f),
                drawAWeightedThreshold = report.aWeightedExposureMetricsAvailable,
            )
            drawTimeRangeLabels(canvas, report, chartRect, style)
        }

        private fun drawPeakEventsPage(
            document: PdfDocument,
            report: SessionReportData,
            style: PdfReportStyle,
            pageCount: Int,
        ) = drawPage(
            document,
            style,
            pageNumber = 4,
            pageCount = pageCount,
            title = string(R.string.report_section_peak_events),
        ) { canvas ->
            if (report.peakEvents.isEmpty()) {
                drawNote(canvas, report.peakEventsNote(), style, PAGE_TOP)
            } else {
                drawPeakEvents(canvas, report.peakEvents.take(MAX_PEAK_EVENTS), style)
            }
        }

        private fun drawHeartRatePage(
            document: PdfDocument,
            report: SessionReportData,
            heartRate: ReportHeartRateSection,
            style: PdfReportStyle,
            pageCount: Int,
        ) = drawPage(
            document,
            style,
            pageNumber = 5,
            pageCount = pageCount,
            title = string(R.string.report_section_heart_rate),
        ) { canvas ->
            canvas.drawText(string(R.string.report_heart_rate_section), PAGE_LEFT, PAGE_TOP, style.sectionPaint)
            val samples = heartRate.samples.sortedBy { it.timestamp }
            if (samples.isEmpty()) {
                drawNote(canvas, string(R.string.report_heart_rate_empty), style, PAGE_TOP + 36f)
            } else {
                val minBpm = samples.minOf { it.beatsPerMinute }.coerceAtMost(60L)
                val maxBpm = samples.maxOf { it.beatsPerMinute }.coerceAtLeast(120L)
                val latestBpm = samples.maxBy { it.timestamp }.beatsPerMinute
                drawMetricTable(
                    canvas = canvas,
                    rows =
                        listOf(
                            string(R.string.report_heart_rate_latest) to "$latestBpm BPM",
                            string(R.string.report_heart_rate_range) to "$minBpm-$maxBpm BPM",
                            string(R.string.report_metric_samples) to samples.size.toString(),
                        ),
                    style = style,
                    top = PAGE_TOP + 48f,
                )
                val chartRect = RectF(PAGE_LEFT, PAGE_TOP + 190f, PAGE_RIGHT, 640f)
                PdfChartRenderer.drawHeartRateSeries(
                    canvas = canvas,
                    rect = chartRect,
                    series =
                        PdfHeartRateSeries(
                            samples = samples,
                            startTime = report.startTime,
                            endTime = report.endTime,
                            minBpm = minBpm,
                            maxBpm = maxBpm,
                        ),
                )
                drawTimeRangeLabels(canvas, report, chartRect, style)
            }
        }

        private fun drawPage(
            document: PdfDocument,
            style: PdfReportStyle,
            pageNumber: Int,
            pageCount: Int,
            title: String,
            content: (Canvas) -> Unit,
        ) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)
            drawHeader(canvas, title, style)
            content(canvas)
            drawFooter(canvas, pageNumber, pageCount, style)
            document.finishPage(page)
        }

        private fun drawHeader(canvas: Canvas, title: String, style: PdfReportStyle) {
            canvas.drawText(string(R.string.app_name), PAGE_LEFT, 46f, style.brandPaint)
            canvas.drawText(title, PAGE_RIGHT, 46f, style.headerPaint)
            canvas.drawLine(PAGE_LEFT, 62f, PAGE_RIGHT, 62f, style.dividerPaint)
        }

        private fun drawFooter(canvas: Canvas, pageNumber: Int, pageCount: Int, style: PdfReportStyle) {
            val footer =
                string(R.string.report_generated_footer, BuildConfig.VERSION_NAME)
            canvas.drawLine(PAGE_LEFT, 780f, PAGE_RIGHT, 780f, style.dividerPaint)
            canvas.drawText(footer, PAGE_LEFT, 804f, style.footerPaint)
            canvas.drawText(
                string(R.string.report_page_number, pageNumber, pageCount),
                PAGE_RIGHT,
                804f,
                style.footerRightPaint,
            )
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
            canvas.drawText(
                string(R.string.report_duration_label, report.durationLabel()),
                PAGE_LEFT,
                y + 22f,
                style.bodyPaint,
            )
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
                    Kpi(
                        report.equivalentLevelLabel,
                        "${ReportTextFormatter.oneDecimal(report.laeqDb)} dB",
                    ),
                    Kpi(
                        string(R.string.report_metric_lcpeak),
                        "${ReportTextFormatter.oneDecimal(report.lcPeakDb)} dB",
                    ),
                    Kpi(
                        string(R.string.report_metric_twa),
                        ReportTextFormatter.oneDecimalOrUnavailable(
                            report.twaDb,
                            " dB",
                            string(R.string.value_unavailable),
                        ),
                    ),
                    Kpi(
                        string(R.string.report_metric_dose),
                        ReportTextFormatter.oneDecimalOrUnavailable(
                            report.dosePercent,
                            "%",
                            string(R.string.value_unavailable),
                        ),
                    ),
                )
            cards.forEachIndexed { index, kpi ->
                val row = index / 2
                val col = index % 2
                val left = PAGE_LEFT + col * (KPI_WIDTH + 20f)
                val rect = RectF(left, top + row * 130f, left + KPI_WIDTH, top + row * 130f + 104f)
                drawKpiCard(canvas, rect, kpi, style)
            }
        }

        private fun drawKpiCard(canvas: Canvas, rect: RectF, kpi: Kpi, style: PdfReportStyle) {
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

        private fun drawPeakEvents(canvas: Canvas, events: List<PeakEvent>, style: PdfReportStyle) {
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            var y = PAGE_TOP
            events.forEachIndexed { index, event ->
                canvas.drawText(
                    "${index + 1}. ${ReportTextFormatter.oneDecimal(event.maxDb)} dB",
                    PAGE_LEFT,
                    y,
                    style.sectionPaint,
                )
                canvas.drawText(
                    "${timeFormat.format(Date(event.startTime))} - ${timeFormat.format(Date(event.endTime))}",
                    PAGE_LEFT,
                    y + 24f,
                    style.bodyPaint,
                )
                canvas.drawText(
                    string(R.string.report_peak_at, timeFormat.format(Date(event.peakTime))),
                    PAGE_LEFT,
                    y + 46f,
                    style.bodyPaint,
                )
                y += 78f
            }
        }

        private fun drawNote(canvas: Canvas, text: String, style: PdfReportStyle, top: Float) {
            canvas.drawRoundRect(RectF(PAGE_LEFT, top, PAGE_RIGHT, top + 76f), 14f, 14f, style.notePaint)
            canvas.drawText(text, PAGE_LEFT + 18f, top + 44f, style.bodyPaint)
        }

        private fun SessionReportData.dateRangeLabel(): String =
            ReportTextFormatter.dateRange(startTime, endTime, PDF_REPORT_DATE_PATTERN)

        private fun SessionReportData.durationLabel(): String = ReportTextFormatter.duration(durationMs)

        private fun SessionReportData.nioshNote(): String = if (aWeightedExposureMetricsAvailable) {
            string(R.string.report_niosh_reference)
        } else {
            string(R.string.report_niosh_metrics_unavailable)
        }

        private fun SessionReportData.peakEventsNote(): String = if (aWeightedExposureMetricsAvailable) {
            string(R.string.report_no_peak_events)
        } else {
            string(R.string.report_a_weighted_peak_unavailable)
        }

        private fun string(@StringRes id: Int, vararg formatArgs: Any): String = context.getString(id, *formatArgs)

        private data class Kpi(val label: String, val value: String)

        private companion object {
            const val PAGE_WIDTH = 595
            const val PAGE_HEIGHT = 842
            const val PAGE_LEFT = 48f
            const val PAGE_RIGHT = 547f
            const val PAGE_TOP = 112f
            const val KPI_WIDTH = 239.5f
            const val MAX_PEAK_EVENTS = 8
            const val BASE_PAGE_COUNT = 4
            const val HEART_RATE_PAGE_COUNT = 5
            const val PDF_REPORT_DATE_PATTERN = "yyyy-MM-dd HH:mm"
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

    private fun font(context: Context, id: Int, fallbackStyle: Int): Typeface =
        ResourcesCompat.getFont(context, id) ?: Typeface.create(Typeface.SANS_SERIF, fallbackStyle)

    private fun paint(
        size: Float,
        color: Int,
        typeface: Typeface,
        align: Paint.Align = Paint.Align.LEFT,
        style: Paint.Style = Paint.Style.FILL,
    ): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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
