package com.dbcheck.app.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.dbcheck.app.domain.noise.NoiseLevel
import com.dbcheck.app.domain.report.ReportPoint

data class PdfChartPoint(
    val x: Float,
    val y: Float,
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
        val mapped =
            mapTimeSeries(
                points = points,
                width = rect.width(),
                height = rect.height(),
                minDb = minDb,
                maxDb = maxDb,
            )
        drawGrid(canvas, rect, style)
        drawThreshold(canvas, rect, minDb, maxDb, style)

        if (mapped.size < 2) return

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
        canvas.drawPath(path, style.linePaint)
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
        val firstTime = sortedPoints.first().timestamp
        val lastTime = sortedPoints.last().timestamp
        val timeSpan = (lastTime - firstTime).takeIf { it > 0L }?.toFloat()
        val dbSpan = (maxDb - minDb).takeIf { it > 0f } ?: 1f

        return sortedPoints.map { point ->
            val x =
                if (timeSpan == null) {
                    width / 2f
                } else {
                    ((point.timestamp - firstTime) / timeSpan * width).coerceIn(0f, width)
                }
            val normalizedDb = ((point.db - minDb) / dbSpan).coerceIn(0f, 1f)
            PdfChartPoint(
                x = x,
                y = height - normalizedDb * height,
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
