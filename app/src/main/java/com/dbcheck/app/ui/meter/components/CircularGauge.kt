package com.dbcheck.app.ui.meter.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dbcheck.app.R
import com.dbcheck.app.domain.noise.NoiseLevel
import com.dbcheck.app.domain.noise.SoundLevelDisplayScale
import com.dbcheck.app.ui.common.UiNumberFormatter
import com.dbcheck.app.ui.theme.DbCheckMotion
import com.dbcheck.app.ui.theme.DbCheckTheme
import com.dbcheck.app.ui.theme.animatedThemeColor

@Suppress("LongMethod")
@Composable
fun CircularGauge(
    currentDb: Float,
    noiseLevel: NoiseLevel,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    gaugeSize: Dp = 288.dp,
    animationsEnabled: Boolean = true,
) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val spacing = DbCheckTheme.spacing
    val textMeasurer = rememberTextMeasurer()

    // Recording state is the source of truth: 0 dB can also be a valid live value.
    val targetSweep =
        if (isRecording) {
            SoundLevelDisplayScale.positionForDb(currentDb) * GAUGE_SWEEP_ANGLE
        } else {
            0f
        }
    val sweepAngle = gaugeSweepAngle(targetSweep = targetSweep, animationsEnabled = animationsEnabled)
    val breathingScale = breathingScale(animationsEnabled && isRecording)

    // Active measurement color is independent from interaction accent.
    val arcColor =
        animatedThemeColor(
            targetValue = colors.noiseLevels.colorFor(noiseLevel),
            animationsEnabled = animationsEnabled,
            label = "gaugeLevelColor",
        )
    val arcBrush =
        remember(arcColor) {
            Brush.sweepGradient(listOf(arcColor, arcColor))
        }

    val glassColor = colors.material.surface.copy(alpha = 0.6f)

    Box(
        modifier = modifier.size(gaugeSize),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(gaugeSize)) {
            val canvasSize = size.minDimension
            val strokeWidth = spacing.meterGaugeStrokeWidth.toPx()
            val scaleInset = spacing.meterGaugeScaleInset.toPx()
            val radius = (canvasSize - strokeWidth) / 2 - scaleInset
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2, radius * 2)

            // Glassmorphic background circle
            drawCircle(
                color = glassColor,
                radius = radius * 0.9f,
            )

            // Track (background arc)
            drawArc(
                brush = colors.signatureGradient,
                startAngle = GAUGE_START_ANGLE,
                sweepAngle = GAUGE_SWEEP_ANGLE,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            // Active arc with breathing pulse
            scale(breathingScale) {
                drawArc(
                    brush = arcBrush,
                    startAngle = GAUGE_START_ANGLE,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }

            // Tick marks
            val tickCount = 27
            for (i in 0..tickCount) {
                val angle =
                    Math.toRadians(
                        (GAUGE_START_ANGLE + (GAUGE_SWEEP_ANGLE * i / tickCount)).toDouble(),
                    )
                val outerRadius = radius + strokeWidth / 2 + 4.dp.toPx()
                val innerRadius = outerRadius + 6.dp.toPx()
                val startX = center.x + (outerRadius * kotlin.math.cos(angle)).toFloat()
                val startY = center.y + (outerRadius * kotlin.math.sin(angle)).toFloat()
                val endX = center.x + (innerRadius * kotlin.math.cos(angle)).toFloat()
                val endY = center.y + (innerRadius * kotlin.math.sin(angle)).toFloat()

                drawLine(
                    color = colors.material.surfaceContainerHigh,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 1.5f,
                )
            }

            GAUGE_SCALE_LABELS.forEach { db ->
                val label = UiNumberFormatter.integer(db)
                val layout = textMeasurer.measure(text = label, style = typography.labelSm)
                val labelRadius =
                    radius +
                        strokeWidth / 2 +
                        spacing.meterGaugeLabelOffset.toPx()
                val labelCenter =
                    gaugeLabelPosition(
                        db = db,
                        center = center,
                        radius = labelRadius,
                    )
                drawText(
                    textLayoutResult = layout,
                    color = colors.material.onSurfaceVariant,
                    topLeft =
                        Offset(
                            x = labelCenter.x - layout.size.width / 2f,
                            y = labelCenter.y - layout.size.height / 2f,
                        ),
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (isRecording) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.space1),
                ) {
                    Text(
                        text = UiNumberFormatter.integer(currentDb),
                        style = typography.displayLg,
                        color = colors.material.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.unit_db),
                        style = typography.dataMd,
                        color = colors.material.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(spacing.space2))
                NoiseLevelPill(noiseLevel = noiseLevel, animationsEnabled = animationsEnabled)
            } else {
                Text(
                    text = stringResource(R.string.meter_idle_instruction),
                    style = typography.labelLg,
                    color = colors.material.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier.widthIn(
                            max =
                                gaugeInnerContentDiameter(
                                    gaugeSize = gaugeSize,
                                    scaleInset = spacing.meterGaugeScaleInset,
                                    strokeWidth = spacing.meterGaugeStrokeWidth,
                                ),
                        ),
                )
            }
        }
    }
}

internal fun gaugeLabelPosition(db: Float, center: Offset, radius: Float): Offset {
    val angleRadians = Math.toRadians(gaugeAngleForDb(db).toDouble())
    return Offset(
        x = center.x + radius * kotlin.math.cos(angleRadians).toFloat(),
        y = center.y + radius * kotlin.math.sin(angleRadians).toFloat(),
    )
}

internal fun gaugeAngleForDb(db: Float): Float =
    GAUGE_START_ANGLE + SoundLevelDisplayScale.positionForDb(db) * GAUGE_SWEEP_ANGLE

internal fun gaugeInnerContentDiameter(gaugeSize: Dp, scaleInset: Dp, strokeWidth: Dp): Dp =
    (gaugeSize - (scaleInset + strokeWidth) * 2).coerceAtLeast(0.dp)

@Composable
private fun gaugeSweepAngle(targetSweep: Float, animationsEnabled: Boolean): Float {
    if (!animationsEnabled) {
        return targetSweep
    }

    val animatedSweep by animateFloatAsState(
        targetValue = targetSweep,
        animationSpec =
            tween(
                durationMillis = DbCheckMotion.GaugeSweep,
                easing = androidx.compose.animation.core.EaseOut,
            ),
        label = "gaugeSweep",
    )
    return animatedSweep
}

@Composable
private fun breathingScale(animationsEnabled: Boolean): Float {
    if (!animationsEnabled) {
        return 1f
    }

    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(DbCheckMotion.Breathing, easing = androidx.compose.animation.core.EaseInOut),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
            ),
        label = "breathingPulse",
    )
    return breathingScale
}

private val GAUGE_SCALE_LABELS = listOf(0f, 40f, 80f, 120f)
private const val GAUGE_START_ANGLE = 135f
private const val GAUGE_SWEEP_ANGLE = 270f
