package com.dbcheck.app.ui.meter.components

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dbcheck.app.data.model.NoiseLevel
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun CircularGauge(
    currentDb: Float,
    noiseLevel: NoiseLevel,
    modifier: Modifier = Modifier,
) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    // Animate the arc sweep
    val targetSweep = (currentDb / 130f).coerceIn(0f, 1f) * 270f
    val animatedSweep by animateFloatAsState(
        targetValue = targetSweep,
        animationSpec = tween(durationMillis = 200, easing = androidx.compose.animation.core.EaseOut),
        label = "gaugeSweep",
    )

    // Breathing pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = androidx.compose.animation.core.EaseInOut),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "breathingPulse",
    )

    // Arc color based on noise level
    val arcBrush = remember(noiseLevel, colors) {
        when (noiseLevel) {
            NoiseLevel.QUIET -> Brush.sweepGradient(
                listOf(colors.success, colors.success.copy(alpha = 0.6f)),
            )
            NoiseLevel.NORMAL -> Brush.sweepGradient(
                listOf(colors.material.primary, colors.material.secondary),
            )
            NoiseLevel.ELEVATED -> Brush.sweepGradient(
                listOf(colors.warning, colors.warning.copy(alpha = 0.8f)),
            )
            NoiseLevel.DANGEROUS -> Brush.sweepGradient(
                listOf(colors.material.error, colors.material.error.copy(alpha = 0.8f)),
            )
        }
    }

    val trackColor = colors.material.surfaceContainerHigh
    val glassColor = colors.material.surface.copy(alpha = 0.6f)

    Box(
        modifier = modifier.size(288.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(288.dp)) {
            val canvasSize = size.minDimension
            val strokeWidth = 12.dp.toPx()
            val radius = (canvasSize - strokeWidth) / 2
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            val arcSize = Size(canvasSize - strokeWidth, canvasSize - strokeWidth)

            // Glassmorphic background circle
            drawCircle(
                color = glassColor,
                radius = radius * 0.85f,
            )

            // Track (background arc)
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            // Active arc with breathing pulse
            scale(breathingScale) {
                drawArc(
                    brush = arcBrush,
                    startAngle = 135f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }

            // Tick marks
            val tickCount = 27
            for (i in 0..tickCount) {
                val angle = Math.toRadians((135.0 + (270.0 * i / tickCount)))
                val outerRadius = radius + strokeWidth / 2 + 4.dp.toPx()
                val innerRadius = outerRadius + 6.dp.toPx()
                val startX = center.x + (outerRadius * kotlin.math.cos(angle)).toFloat()
                val startY = center.y + (outerRadius * kotlin.math.sin(angle)).toFloat()
                val endX = center.x + (innerRadius * kotlin.math.cos(angle)).toFloat()
                val endY = center.y + (innerRadius * kotlin.math.sin(angle)).toFloat()

                drawLine(
                    color = trackColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 1.5f,
                )
            }
        }

        // Center content: dB reading
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "DECIBELS",
                style = typography.labelMd,
                color = colors.material.onSurfaceVariant,
            )
            Text(
                text = currentDb.toInt().toString(),
                style = typography.displayLg,
                color = colors.material.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "dB",
                style = typography.dataLg,
                color = colors.material.onSurfaceVariant,
            )
        }
    }
}
