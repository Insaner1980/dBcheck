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

    if (state.isResultMissing) {
        MissingResultContent(onBack = onSave)
        return
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.material.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
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
private fun MissingResultContent(onBack: () -> Unit) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val spacing = DbCheckTheme.spacing

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.material.background)
                .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Result not found",
            style = typography.headlineLg,
            color = colors.material.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.space3))
        Text(
            text = "This hearing test result is no longer available.",
            style = typography.bodyLg,
            color = colors.material.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.space8))
        DbCheckButton(
            text = "Back to Analytics",
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            height = 56.dp,
        )
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
    val typography = DbCheckTheme.typography

    DbCheckCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("HEARING PROFILE", style = typography.labelMd, color = colors.material.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            AudiogramLegend()
            Spacer(Modifier.height(8.dp))
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
    val typography = DbCheckTheme.typography

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
}

@Composable
private fun KeyMetricsCard(state: ResultsUiState) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    DbCheckCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("KEY METRICS", style = typography.labelMd, color = colors.material.onSurfaceVariant)
            MetricRow("Avg. Threshold", "${String.format(Locale.getDefault(), "%.0f", state.avgThreshold)} dB")
            MetricRow("Speech Clarity*", "${String.format(Locale.getDefault(), "%.0f", state.speechClarity)}%")
            MetricRow(
                "High Freq. Limit*",
                "${String.format(Locale.getDefault(), "%.1f", state.highFreqLimit / 1000f)} kHz",
            )
            Text(
                text = "*Estimated based on test data",
                style = typography.labelSm,
                color = colors.material.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ResultsDisclaimer() {
    Text(
        text = "This test provides relative hearing thresholds for personal tracking. For clinical diagnosis, consult an audiologist.",
        style = DbCheckTheme.typography.bodyMd,
        color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}

@Composable
private fun ShareErrorMessage(message: String?) {
    val spacing = DbCheckTheme.spacing

    message?.let { error ->
        Text(
            text = error,
            style = DbCheckTheme.typography.bodyMd,
            color = DbCheckTheme.colorScheme.material.error,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
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
        if (leftData.isEmpty()) return@Canvas

        val maxFreq = 8000f
        val minThreshold = -60f

        fun drawLine(
            data: List<Pair<Float, Float>>,
            color: androidx.compose.ui.graphics.Color,
        ) {
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
