package com.dbcheck.app.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.dbcheck.app.domain.noise.NoiseLevel
import com.dbcheck.app.domain.report.ReportHeartRateSample
import com.dbcheck.app.domain.report.ReportPoint

data class PdfChartPoint(
    val x: Float,
    val y: Float,
)

data class PdfHeartRateSeries(
    val samples: List<ReportHeartRateSample>,
    val startTime: Long,
    val endTime: Long,
    val minBpm: Long,
    val maxBpm: Long,
)

private data class PdfSeriesMapping(
    val startTime: Long,
    val endTime: Long,
    val width: Float,
    val height: Float,
    val minValue: Float,
    val maxValue: Float,
)

object PdfChartRenderer {
    fun drawTimeSeries(
        canvas: Canvas,
        points: List<ReportPoint>,
        rect: RectF,
        minDb: Float,
        maxDb: Float,
        style: PdfChartStyle = PdfChartStyle(),
    ) {
        val shape =
            buildTimeSeriesShape(
                points = points,
                width = rect.width(),
                height = rect.height(),
                minDb = minDb,
                maxDb = maxDb,
            )
        drawGrid(canvas, rect, style)
        drawThreshold(canvas, rect, minDb, maxDb, style)

        when (shape) {
            PdfTimeSeriesShape.Empty -> return

            is PdfTimeSeriesShape.SinglePoint ->
                canvas.drawCircle(
                    rect.left + shape.point.x,
                    rect.top + shape.point.y,
                    POINT_RADIUS,
                    style.pointPaint,
                )

            is PdfTimeSeriesShape.Line -> drawLine(canvas, rect, shape.points, style.linePaint)
        }
    }

    fun drawHeartRateSeries(
        canvas: Canvas,
        series: PdfHeartRateSeries,
        rect: RectF,
        style: PdfChartStyle = PdfChartStyle(),
    ) {
        val shape =
            buildHeartRateShape(
                samples = series.samples,
                startTime = series.startTime,
                endTime = series.endTime,
                width = rect.width(),
                height = rect.height(),
                minBpm = series.minBpm,
                maxBpm = series.maxBpm,
            )
        drawGrid(canvas, rect, style)

        when (shape) {
            PdfTimeSeriesShape.Empty -> return

            is PdfTimeSeriesShape.SinglePoint ->
                canvas.drawCircle(
                    rect.left + shape.point.x,
                    rect.top + shape.point.y,
                    POINT_RADIUS,
                    style.heartRatePointPaint,
                )

            is PdfTimeSeriesShape.Line -> drawLine(canvas, rect, shape.points, style.heartRateLinePaint)
        }
    }

    internal fun buildTimeSeriesShape(
        points: List<ReportPoint>,
        width: Float,
        height: Float,
        minDb: Float,
        maxDb: Float,
    ): PdfTimeSeriesShape {
        val mapped =
            mapTimeSeries(
                points = points,
                width = width,
                height = height,
                minDb = minDb,
                maxDb = maxDb,
            )
        return when (mapped.size) {
            0 -> PdfTimeSeriesShape.Empty
            1 -> PdfTimeSeriesShape.SinglePoint(mapped.single())
            else -> PdfTimeSeriesShape.Line(mapped)
        }
    }

    internal fun buildHeartRateShape(
        samples: List<ReportHeartRateSample>,
        startTime: Long,
        endTime: Long,
        width: Float,
        height: Float,
        minBpm: Long,
        maxBpm: Long,
    ): PdfTimeSeriesShape {
        val mapped =
            mapHeartRateSeries(
                samples = samples,
                startTime = startTime,
                endTime = endTime,
                width = width,
                height = height,
                minBpm = minBpm,
                maxBpm = maxBpm,
            )
        return when (mapped.size) {
            0 -> PdfTimeSeriesShape.Empty
            1 -> PdfTimeSeriesShape.SinglePoint(mapped.single())
            else -> PdfTimeSeriesShape.Line(mapped)
        }
    }

    private fun drawLine(canvas: Canvas, rect: RectF, mapped: List<PdfChartPoint>, paint: Paint) {
        val path =
            Path().apply {
                mapped.forEachIndexed { index, point ->
                    val x = rect.left + point.x
                    val y = rect.top + point.y
                    if (index == 0) {
                        moveTo(x, y)
                    } else {
                        lineTo(x, y)
                    }
                }
            }
        canvas.drawPath(path, paint)
    }

    fun mapTimeSeries(
        points: List<ReportPoint>,
        width: Float,
        height: Float,
        minDb: Float,
        maxDb: Float,
    ): List<PdfChartPoint> {
        if (points.isEmpty()) return emptyList()

        val sortedPoints = points.sortedBy { it.timestamp }
        return mapSeries(
            items = sortedPoints,
            mapping =
                PdfSeriesMapping(
                    startTime = sortedPoints.first().timestamp,
                    endTime = sortedPoints.last().timestamp,
                    width = width,
                    height = height,
                    minValue = minDb,
                    maxValue = maxDb,
                ),
            timestampOf = { it.timestamp },
            valueOf = { it.db },
        )
    }

    fun mapHeartRateSeries(
        samples: List<ReportHeartRateSample>,
        startTime: Long,
        endTime: Long,
        width: Float,
        height: Float,
        minBpm: Long,
        maxBpm: Long,
    ): List<PdfChartPoint> = mapSeries(
        items = samples.sortedBy { it.timestamp },
        mapping =
            PdfSeriesMapping(
                startTime = startTime,
                endTime = endTime,
                width = width,
                height = height,
                minValue = minBpm.toFloat(),
                maxValue = maxBpm.toFloat(),
            ),
        timestampOf = { it.timestamp },
        valueOf = { it.beatsPerMinute.toFloat() },
    )

    private fun <T> mapSeries(
        items: List<T>,
        mapping: PdfSeriesMapping,
        timestampOf: (T) -> Long,
        valueOf: (T) -> Float,
    ): List<PdfChartPoint> {
        if (items.isEmpty()) return emptyList()

        val timeSpan = (mapping.endTime - mapping.startTime).takeIf { it > 0L }?.toFloat()
        val valueSpan = (mapping.maxValue - mapping.minValue).takeIf { it > 0f } ?: 1f

        return items.map { item ->
            val x =
                if (timeSpan == null) {
                    mapping.width / 2f
                } else {
                    ((timestampOf(item) - mapping.startTime) / timeSpan * mapping.width)
                        .coerceIn(0f, mapping.width)
                }
            val normalizedValue = ((valueOf(item) - mapping.minValue) / valueSpan).coerceIn(0f, 1f)
            PdfChartPoint(
                x = x,
                y = mapping.height - normalizedValue * mapping.height,
            )
        }
    }

    private fun drawGrid(
        canvas: Canvas,
        rect: RectF,
        style: PdfChartStyle,
    ) {
        repeat(GRID_LINE_COUNT) { index ->
            val y = rect.top + rect.height() * index / (GRID_LINE_COUNT - 1)
            canvas.drawLine(rect.left, y, rect.right, y, style.gridPaint)
        }
        canvas.drawRect(rect, style.borderPaint)
    }

    private fun drawThreshold(
        canvas: Canvas,
        rect: RectF,
        minDb: Float,
        maxDb: Float,
        style: PdfChartStyle,
    ) {
        val thresholdDb = NoiseLevel.ELEVATED.maxDb
        if (thresholdDb !in minDb..maxDb) return

        val y =
            rect.bottom -
                ((thresholdDb - minDb) / (maxDb - minDb).coerceAtLeast(1f)) * rect.height()
        canvas.drawLine(rect.left, y, rect.right, y, style.thresholdPaint)
        canvas.drawText("85 dBA", rect.right - 44f, y - 6f, style.axisLabelPaint)
    }

    private const val GRID_LINE_COUNT = 5
    private const val POINT_RADIUS = 4f
}

data class PdfChartStyle(
    val linePaint: Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(70, 105, 6)
            strokeWidth = 3f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        },
    val pointPaint: Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(70, 105, 6)
            style = Paint.Style.FILL
        },
    val heartRateLinePaint: Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(204, 119, 0)
            strokeWidth = 3f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        },
    val heartRatePointPaint: Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(204, 119, 0)
            style = Paint.Style.FILL
        },
    val gridPaint: Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(220, 224, 224)
            strokeWidth = 1f
        },
    val borderPaint: Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(175, 178, 179)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        },
    val thresholdPaint: Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(204, 119, 0)
            strokeWidth = 2f
            pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
        },
    val axisLabelPaint: Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(92, 96, 96)
            textSize = 10f
        },
)
