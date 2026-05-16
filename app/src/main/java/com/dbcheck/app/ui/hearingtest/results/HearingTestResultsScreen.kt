package com.dbcheck.app.ui.hearingtest.results

import android.content.Intent
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.DbCheckErrorMessage
import com.dbcheck.app.ui.theme.DbCheckTheme
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun HearingTestResultsScreen(
    onSave: () -> Unit,
    viewModel: ResultsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.shareErrorMessage) {
        if (state.shareErrorMessage != null) {
            delay(3_000L)
            viewModel.clearShareError()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.shareIntents.collect { intent ->
            runCatching {
                context.startActivity(
                    Intent.createChooser(intent, "Share hearing test results"),
                )
            }.onFailure {
                viewModel.onShareUnavailable()
            }
        }
    }

    HearingTestResultsContent(
        state = state,
        onSave = onSave,
        onShare = viewModel::createShareIntent,
    )
}

@Composable
private fun HearingTestResultsContent(
    state: ResultsUiState,
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.material.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.space5),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(spacing.space10))
        ResultsHeader(state = state)
        Spacer(Modifier.height(spacing.space8))
        AudiogramCard(state = state)
        Spacer(Modifier.height(spacing.space4))
        KeyMetricsCard(state = state)
        Spacer(Modifier.height(spacing.space4))
        ResultsDisclaimer()
        Spacer(Modifier.height(spacing.space8))
        ShareErrorMessage(message = state.shareErrorMessage)
        ResultsActions(onSave = onSave, onShare = onShare)
        Spacer(Modifier.height(spacing.space8))
    }
}

@Composable
private fun ResultsHeader(state: ResultsUiState) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val spacing = DbCheckTheme.spacing

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

    val ratingColor =
        when (state.rating) {
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
}

@Composable
private fun AudiogramCard(state: ResultsUiState) {
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing
    val typography = DbCheckTheme.typography

    DbCheckCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("HEARING PROFILE", style = typography.labelMd, color = colors.material.onSurfaceVariant)
            Spacer(Modifier.height(spacing.space3))
            AudiogramLegend()
            Spacer(Modifier.height(spacing.space2))
            AudiogramChart(
                leftData = state.leftEarThresholds,
                rightData = state.rightEarThresholds,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp),
            )
        }
    }
}

@Composable
private fun AudiogramLegend() {
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing
    val typography = DbCheckTheme.typography

    Row(horizontalArrangement = Arrangement.spacedBy(spacing.space4)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.space1),
        ) {
            Canvas(Modifier.size(12.dp)) { drawCircle(color = colors.material.primary) }
            Text("LEFT", style = typography.labelSm, color = colors.material.onSurfaceVariant)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.space1),
        ) {
            Canvas(Modifier.size(12.dp)) { drawCircle(color = colors.material.secondary) }
            Text("RIGHT", style = typography.labelSm, color = colors.material.onSurfaceVariant)
        }
    }
}

@Composable
private fun KeyMetricsCard(state: ResultsUiState) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    DbCheckCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space3),
        ) {
            Text("KEY METRICS", style = typography.labelMd, color = colors.material.onSurfaceVariant)
            MetricRow(
                "Avg. tone level",
                "${String.format(Locale.getDefault(), "%.0f", state.avgThreshold)} relative dB",
            )
            MetricRow(
                "Speech clarity estimate",
                "${String.format(Locale.getDefault(), "%.0f", state.speechClarity)}%",
            )
            MetricRow(
                "Highest detected freq.",
                formatHighFrequencyLimit(state.highFreqLimit),
            )
            Text(
                text = "Relative estimates from this app's tone test, not clinical audiometry.",
                style = typography.labelSm,
                color = colors.material.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ResultsDisclaimer() {
    Text(
        text =
            "This test provides relative hearing thresholds for personal tracking. " +
                "For clinical diagnosis, consult an audiologist.",
        style = DbCheckTheme.typography.bodyMd,
        color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = DbCheckTheme.spacing.space3),
    )
}

@Composable
private fun ShareErrorMessage(message: String?) {
    val spacing = DbCheckTheme.spacing

    message?.let { error ->
        DbCheckErrorMessage(
            text = error,
            horizontalPadding = spacing.space3,
        )
        Spacer(Modifier.height(spacing.space3))
    }
}

@Composable
private fun ResultsActions(
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    val spacing = DbCheckTheme.spacing

    DbCheckButton(
        text = "Save to Profile",
        onClick = onSave,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(spacing.space3))
    DbCheckButton(
        text = "Share Results",
        onClick = onShare,
        modifier = Modifier.fillMaxWidth(),
        style = DbCheckButtonStyle.Secondary,
    )
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
) {
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
        if (!hasAudiogramData(leftData, rightData)) return@Canvas

        val maxFreq = 8000f
        val minThreshold = -60f

        fun drawLine(
            data: List<Pair<Float, Float>>,
            color: androidx.compose.ui.graphics.Color,
        ) {
            if (data.isEmpty()) return
            val path = Path()
            data.forEachIndexed { index, (freq, threshold) ->
                val point = audiogramPoint(freq, threshold, maxFreq, minThreshold, size.width, size.height)
                if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            }
            drawPath(path, color, style = Stroke(width = 3f, cap = StrokeCap.Round))

            // Draw dots
            data.forEach { (freq, threshold) ->
                val point = audiogramPoint(freq, threshold, maxFreq, minThreshold, size.width, size.height)
                drawCircle(color, radius = 6f, center = point)
            }
        }

        drawLine(leftData, leftColor)
        drawLine(rightData, rightColor)
    }
}

internal fun hasAudiogramData(leftData: List<Pair<Float, Float>>, rightData: List<Pair<Float, Float>>): Boolean =
    leftData.isNotEmpty() || rightData.isNotEmpty()

internal fun audiogramPoint(
    frequency: Float,
    threshold: Float,
    maxFrequency: Float,
    minThreshold: Float,
    width: Float,
    height: Float,
): Offset {
    val x = (kotlin.math.log2(frequency / 250f) / kotlin.math.log2(maxFrequency / 250f)) * width
    val y = ((threshold - minThreshold) / (0f - minThreshold)) * height
    return Offset(x, y)
}

internal fun formatHighFrequencyLimit(highFreqLimitHz: Float): String = if (highFreqLimitHz > 0f) {
        "${String.format(Locale.getDefault(), "%.1f", highFreqLimitHz / 1000f)} kHz"
    } else {
        "N/A"
    }
