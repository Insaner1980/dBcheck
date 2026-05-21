package com.dbcheck.app.ui.analytics.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.dbcheck.app.R
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
    val minBpm = samples.minOf { it.beatsPerMinute }.coerceAtMost(60L)
    val maxBpm = samples.maxOf { it.beatsPerMinute }.coerceAtLeast(120L)
    val latestBpm = samples.maxBy { it.time }.beatsPerMinute
    val resources = LocalResources.current
    val chartDescription =
        resources.getQuantityString(
            R.plurals.heart_rate_chart_description,
            samples.size,
            samples.size,
            latestBpm,
            minBpm.formatBpm(),
            maxBpm.formatBpm(),
        )

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
        Text(
            text = stringResource(R.string.heart_rate_header, latestBpm),
            style = typography.labelMd,
            color = colors.material.onSurfaceVariant,
        )
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(spacing.space16)
                    .semantics {
                        contentDescription = chartDescription
                    },
        ) {
            val timeSpan = (endTimeMs - startTimeMs).toFloat().coerceAtLeast(1f)
            val bpmSpan = (maxBpm - minBpm).toFloat().coerceAtLeast(1f)
            val path = Path()

            samples.forEachIndexed { index, sample ->
                val normalizedX =
                    ((sample.time.toEpochMilli() - startTimeMs) / timeSpan)
                        .coerceIn(0f, 1f)
                val y =
                    size.height -
                        ((sample.beatsPerMinute - minBpm) / bpmSpan)
                            .coerceIn(0f, 1f) * size.height
                val x = normalizedX * size.width
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawLine(
                color = colors.ghostBorder,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
            )
            drawPath(
                path = path,
                color = colors.warning,
                style = Stroke(width = spacing.space1.toPx(), cap = StrokeCap.Round),
            )
        }
        Text(
            text = "${minBpm.formatBpm()}-${maxBpm.formatBpm()} BPM",
            style = typography.labelSm,
            color = colors.material.onSurfaceVariant,
        )
    }
}

private fun Long.formatBpm(): String = "%d".format(Locale.US, this)
