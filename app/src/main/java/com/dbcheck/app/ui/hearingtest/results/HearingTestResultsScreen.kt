package com.dbcheck.app.ui.hearingtest.results

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun HearingTestResultsScreen(
    onSave: () -> Unit,
    onShare: () -> Unit,
    viewModel: ResultsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val spacing = DbCheckTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.material.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(spacing.space10))

        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = colors.success,
            modifier = Modifier.size(48.dp),
        )

        Spacer(Modifier.height(spacing.space4))

        Text(
            text = "ANALYSIS COMPLETE",
            style = typography.labelMd,
            color = colors.material.onSurfaceVariant,
        )

        Spacer(Modifier.height(spacing.space2))

        val ratingColor = when (state.rating) {
            "Excellent" -> colors.success
            "Good" -> colors.material.primary
            "Fair" -> colors.warning
            else -> colors.material.error
        }

        Text(
            text = state.rating,
            style = typography.headlineLg,
            color = ratingColor,
        )

        Text(
            text = "YOUR HEARING IS WITHIN ${state.rating.uppercase()} RANGE",
            style = typography.labelMd,
            color = colors.material.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(spacing.space8))

        // Audiogram chart
        DbCheckCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("HEARING PROFILE", style = typography.labelMd, color = colors.material.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Canvas(Modifier.size(12.dp)) { drawCircle(color = colors.material.primary) }
                        Text("LEFT", style = typography.labelSm, color = colors.material.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Canvas(Modifier.size(12.dp)) { drawCircle(color = colors.material.secondary) }
                        Text("RIGHT", style = typography.labelSm, color = colors.material.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Simple audiogram
                AudiogramChart(
                    leftData = state.leftEarThresholds,
                    rightData = state.rightEarThresholds,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                )
            }
        }

        Spacer(Modifier.height(spacing.space4))

        // Key metrics
        DbCheckCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("KEY METRICS", style = typography.labelMd, color = colors.material.onSurfaceVariant)
                MetricRow("Avg. Threshold", "${String.format("%.0f", state.avgThreshold)} dB")
                MetricRow("Speech Clarity*", "${String.format("%.0f", state.speechClarity)}%")
                MetricRow("High Freq. Limit*", "${String.format("%.1f", state.highFreqLimit / 1000f)} kHz")
                Text(
                    text = "*Estimated based on test data",
                    style = typography.labelSm,
                    color = colors.material.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(spacing.space4))

        Text(
            text = "This test provides relative hearing thresholds for personal tracking. For clinical diagnosis, consult an audiologist.",
            style = typography.bodyMd,
            color = colors.material.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )

        Spacer(Modifier.height(spacing.space8))

        DbCheckButton(
            text = "Save to Profile",
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            height = 56.dp,
        )

        Spacer(Modifier.height(spacing.space3))

        DbCheckButton(
            text = "Share Results",
            onClick = onShare,
            modifier = Modifier.fillMaxWidth(),
            style = DbCheckButtonStyle.Secondary,
            height = 56.dp,
        )

        Spacer(Modifier.height(spacing.space8))
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = typography.bodyMd, color = colors.material.onSurfaceVariant)
        Text(value, style = typography.dataMd, color = colors.material.onSurface)
    }
}

@Composable
private fun AudiogramChart(
    leftData: List<Pair<Float, Float>>,
    rightData: List<Pair<Float, Float>>,
    modifier: Modifier = Modifier,
) {
    val colors = DbCheckTheme.colorScheme
    val leftColor = colors.material.primary
    val rightColor = colors.material.secondary

    Canvas(modifier = modifier) {
        if (leftData.isEmpty()) return@Canvas

        val maxFreq = 8000f
        val minThreshold = -60f

        fun drawLine(data: List<Pair<Float, Float>>, color: androidx.compose.ui.graphics.Color) {
            val path = Path()
            data.forEachIndexed { index, (freq, threshold) ->
                val x = (kotlin.math.log2(freq / 250f) / kotlin.math.log2(maxFreq / 250f)) * size.width
                val y = ((threshold - minThreshold) / (0f - minThreshold)) * size.height
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color, style = Stroke(width = 3f, cap = StrokeCap.Round))

            // Draw dots
            data.forEach { (freq, threshold) ->
                val x = (kotlin.math.log2(freq / 250f) / kotlin.math.log2(maxFreq / 250f)) * size.width
                val y = ((threshold - minThreshold) / (0f - minThreshold)) * size.height
                drawCircle(color, radius = 6f, center = Offset(x, y))
            }
        }

        drawLine(leftData, leftColor)
        drawLine(rightData, rightColor)
    }
}
