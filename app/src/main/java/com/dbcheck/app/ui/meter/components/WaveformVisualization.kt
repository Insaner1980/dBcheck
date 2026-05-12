package com.dbcheck.app.ui.meter.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.dbcheck.app.data.local.preferences.model.WaveformStyle
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun WaveformVisualization(
    data: List<Float>,
    style: WaveformStyle,
    modifier: Modifier = Modifier,
) {
    val colors = DbCheckTheme.colorScheme
    val waveColor = colors.material.tertiary.copy(alpha = 0.2f)

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(48.dp),
    ) {
        if (data.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val midY = height / 2
        val stepX = width / (data.size - 1).coerceAtLeast(1)

        if (style == WaveformStyle.BARS) {
            data.forEachIndexed { index, amplitude ->
                val x = index * stepX
                val barHalfHeight = amplitude * midY * 0.8f
                drawLine(
                    color = waveColor,
                    start = Offset(x, midY - barHalfHeight),
                    end = Offset(x, midY + barHalfHeight),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            return@Canvas
        }

        val path =
            Path().apply {
                moveTo(0f, midY)
                data.forEachIndexed { index, amplitude ->
                    val x = index * stepX
                    val y = midY - (amplitude * midY * 0.8f)
                    if (index == 0) {
                        moveTo(x, y)
                    } else {
                        lineTo(x, y)
                    }
                }
            }

        if (style == WaveformStyle.FILLED) {
            val fillPath =
                Path().apply {
                    addPath(path)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }
            drawPath(
                path = fillPath,
                color = waveColor.copy(alpha = 0.08f),
            )
        }

        drawPath(
            path = path,
            color = waveColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}
