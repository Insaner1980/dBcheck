package com.dbcheck.app.ui.analytics.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.dbcheck.app.ui.history.detail.HeartRateSampleUiState
import com.dbcheck.app.ui.theme.DbCheckTheme
import java.util.Locale

@Composable
fun HeartRateOverlay(
    samples: List<HeartRateSampleUiState>,
    startTimeMs: Long,
    endTimeMs: Long,
    modifier: Modifier = Modifier,
) {
    if (samples.isEmpty() || endTimeMs <= startTimeMs) return

    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing
    val stats = remember(samples) { samples.toHeartRateOverlayStats() }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
        Text(
            text = "HEART RATE ${stats.latestBpm} BPM",
            style = typography.labelMd,
            color = colors.material.onSurfaceVariant,
        )
        Spacer(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(spacing.space16)
                    .drawWithCache {
                        val timeSpan = (endTimeMs - startTimeMs).toFloat().coerceAtLeast(1f)
                        val bpmSpan = (stats.maxBpm - stats.minBpm).toFloat().coerceAtLeast(1f)
                        val path = Path()

                        samples.forEachIndexed { index, sample ->
                            val normalizedX =
                                ((sample.time.toEpochMilli() - startTimeMs) / timeSpan)
                                    .coerceIn(0f, 1f)
                            val y =
                                size.height -
                                    ((sample.beatsPerMinute - stats.minBpm) / bpmSpan)
                                        .coerceIn(0f, 1f) * size.height
                            val x = normalizedX * size.width
                            if (index == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                        }
                        val stroke =
                            Stroke(
                                width = spacing.space1.toPx(),
                                cap = StrokeCap.Round,
                            )

                        onDrawBehind {
                            drawLine(
                                color = colors.ghostBorder,
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                            )
                            drawPath(
                                path = path,
                                color = colors.warning,
                                style = stroke,
                            )
                        }
                    },
        )
        Text(
            text = "${stats.minBpm.formatBpm()}-${stats.maxBpm.formatBpm()} BPM",
            style = typography.labelSm,
            color = colors.material.onSurfaceVariant,
        )
    }
}

private data class HeartRateOverlayStats(val minBpm: Long, val maxBpm: Long, val latestBpm: Long)

private fun List<HeartRateSampleUiState>.toHeartRateOverlayStats(): HeartRateOverlayStats = HeartRateOverlayStats(
        minBpm = minOf { it.beatsPerMinute }.coerceAtMost(60L),
        maxBpm = maxOf { it.beatsPerMinute }.coerceAtLeast(120L),
        latestBpm = maxBy { it.time }.beatsPerMinute,
    )

private fun Long.formatBpm(): String = "%d".format(Locale.US, this)
