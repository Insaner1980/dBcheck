package com.dbcheck.app.ui.hearingtest.results

import android.content.Context
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dbcheck.app.R
import com.dbcheck.app.domain.hearingtest.HearingRating
import com.dbcheck.app.domain.hearingtest.HearingTestPolicy
import com.dbcheck.app.ui.common.currentLocale
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.theme.DbCheckTheme
import com.dbcheck.app.util.hearingTestRatingStringRes
import kotlinx.coroutines.delay

@Composable
fun HearingTestResultsScreen(onSave: () -> Unit, viewModel: ResultsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val shareChooserTitle = stringResource(R.string.hearing_results_share_chooser)

    LaunchedEffect(state.shareErrorMessage) {
        if (state.shareErrorMessage != null) {
            delay(3_000L)
            viewModel.clearShareError()
        }
    }

    LaunchedEffect(shareChooserTitle) {
        viewModel.shareIntents.collect { intent ->
            runCatching {
                context.startActivity(
                    Intent.createChooser(intent, shareChooserTitle),
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
private fun HearingTestResultsContent(state: ResultsUiState, onSave: () -> Unit, onShare: () -> Unit) {
    when (resultsContentMode(state)) {
        ResultsContentMode.LOADING -> LoadingResultContent()
        ResultsContentMode.ERROR -> ErrorResultContent(message = state.loadErrorMessage, onBack = onSave)
        ResultsContentMode.LOCKED -> LockedResultContent(onBack = onSave)
        ResultsContentMode.MISSING -> MissingResultContent(onBack = onSave)
        ResultsContentMode.CONTENT -> LoadedResultContent(state = state, onSave = onSave, onShare = onShare)
    }
}

@Composable
private fun LoadedResultContent(state: ResultsUiState, onSave: () -> Unit, onShare: () -> Unit) {
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing

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

internal enum class ResultsContentMode {
    LOADING,
    ERROR,
    LOCKED,
    MISSING,
    CONTENT,
}

internal fun resultsContentMode(state: ResultsUiState): ResultsContentMode = when {
        state.isLoading -> ResultsContentMode.LOADING
        state.loadErrorMessage != null -> ResultsContentMode.ERROR
        !state.isProUser -> ResultsContentMode.LOCKED
        state.isResultMissing -> ResultsContentMode.MISSING
        else -> ResultsContentMode.CONTENT
    }

@Composable
private fun LoadingResultContent() {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

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
            text = stringResource(R.string.hearing_results_loading),
            style = typography.bodyLg,
            color = colors.material.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LockedResultContent(onBack: () -> Unit) {
    UnavailableResultContent(
        title = stringResource(R.string.hearing_results_pro_title),
        message = stringResource(R.string.hearing_results_pro_message),
        onBack = onBack,
    )
}

@Composable
private fun MissingResultContent(onBack: () -> Unit) {
    UnavailableResultContent(
        title = stringResource(R.string.hearing_results_missing_title),
        message = stringResource(R.string.hearing_results_missing_message),
        onBack = onBack,
    )
}

@Composable
private fun ErrorResultContent(message: String?, onBack: () -> Unit) {
    UnavailableResultContent(
        title = message ?: stringResource(R.string.hearing_error_load_failed),
        message = null,
        onBack = onBack,
    )
}

@Composable
private fun UnavailableResultContent(title: String, message: String?, onBack: () -> Unit) {
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
            text = title,
            style = typography.headlineLg,
            color = colors.material.onSurface,
            textAlign = TextAlign.Center,
        )
        message?.let {
            Spacer(Modifier.height(spacing.space3))
            Text(
                text = it,
                style = typography.bodyLg,
                color = colors.material.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(spacing.space8))
        DbCheckButton(
            text = stringResource(R.string.hearing_results_back_to_analytics),
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
    val localizedRating = stringResource(hearingTestRatingStringRes(state.rating))

    Icon(
        imageVector = Icons.Filled.CheckCircle,
        contentDescription = null,
        tint = colors.success,
        modifier = Modifier.size(48.dp),
    )

    Spacer(Modifier.height(spacing.space4))

    Text(
        text = stringResource(R.string.hearing_results_analysis_complete),
        style = typography.labelMd,
        color = colors.material.onSurfaceVariant,
    )

    Spacer(Modifier.height(spacing.space2))

    val ratingColor =
        when (HearingRating.fromCode(state.rating)) {
            HearingRating.EXCELLENT -> colors.success
            HearingRating.GOOD -> colors.material.primary
            HearingRating.FAIR -> colors.warning
            HearingRating.POOR -> colors.material.error
        }

    Text(
        text = localizedRating,
        style = typography.headlineLg,
        color = ratingColor,
    )

    Text(
        text = stringResource(R.string.hearing_results_summary_range, localizedRating.uppercase()),
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
            Text(
                stringResource(R.string.hearing_results_hearing_profile),
                style = typography.labelMd,
                color = colors.material.onSurfaceVariant,
            )
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
            Text(
                stringResource(R.string.hearing_left_caps),
                style = typography.labelSm,
                color = colors.material.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Canvas(Modifier.size(12.dp)) { drawCircle(color = colors.material.secondary) }
            Text(
                stringResource(R.string.hearing_right_caps),
                style = typography.labelSm,
                color = colors.material.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun KeyMetricsCard(state: ResultsUiState) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val locale = currentLocale()

    DbCheckCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.hearing_results_key_metrics),
                style = typography.labelMd,
                color = colors.material.onSurfaceVariant,
            )
            MetricRow(
                stringResource(R.string.hearing_results_avg_threshold),
                "${String.format(locale, "%.0f", state.avgThreshold)} dB relative",
            )
            MetricRow(
                stringResource(R.string.hearing_results_tested_range),
                stringResource(
                    R.string.hearing_results_tested_range_value,
                    HearingTestPolicy.MIN_FREQUENCY_HZ.toInt(),
                    (HearingTestPolicy.MAX_FREQUENCY_HZ / 1_000f).toInt(),
                ),
            )
            Text(
                text = stringResource(R.string.hearing_results_estimated_note),
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
            stringResource(R.string.hearing_results_disclaimer),
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
private fun ResultsActions(onSave: () -> Unit, onShare: () -> Unit) {
    val spacing = DbCheckTheme.spacing

    DbCheckButton(
        text = stringResource(R.string.action_save_to_profile),
        onClick = onSave,
        modifier = Modifier.fillMaxWidth(),
        height = 56.dp,
    )
    Spacer(Modifier.height(spacing.space3))
    DbCheckButton(
        text = stringResource(R.string.action_share_results),
        onClick = onShare,
        modifier = Modifier.fillMaxWidth(),
        style = DbCheckButtonStyle.Secondary,
        height = 56.dp,
    )
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
    val context = LocalContext.current
    val chartDescription = audiogramChartContentDescription(context, leftData, rightData)

    Canvas(
        modifier =
            modifier.semantics {
                contentDescription = chartDescription
            },
    ) {
        if (leftData.isEmpty()) return@Canvas

        val maxFreq = HearingTestPolicy.MAX_FREQUENCY_HZ
        val minThreshold = -60f

        fun drawLine(data: List<Pair<Float, Float>>, color: androidx.compose.ui.graphics.Color) {
            fun pointFor(freq: Float, threshold: Float): Offset {
                val x =
                    (
                        kotlin.math.log2(freq / HearingTestPolicy.MIN_FREQUENCY_HZ) /
                            kotlin.math.log2(maxFreq / HearingTestPolicy.MIN_FREQUENCY_HZ)
                    ) * size.width
                val y = ((threshold - minThreshold) / (0f - minThreshold)) * size.height
                return Offset(x, y)
            }

            val points = data.map { (freq, threshold) -> pointFor(freq, threshold) }
            val path = Path()
            points.forEachIndexed { index, point ->
                if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            }
            drawPath(path, color, style = Stroke(width = 3f, cap = StrokeCap.Round))

            points.forEach { point ->
                drawCircle(color, radius = 6f, center = point)
            }
        }

        drawLine(leftData, leftColor)
        drawLine(rightData, rightColor)
    }
}

private fun audiogramChartContentDescription(
    context: Context,
    leftData: List<Pair<Float, Float>>,
    rightData: List<Pair<Float, Float>>,
): String {
    if (leftData.isEmpty() && rightData.isEmpty()) return context.getString(R.string.a11y_audiogram_chart_empty)

    fun earDescription(label: String, data: List<Pair<Float, Float>>): String {
        if (data.isEmpty()) return context.getString(R.string.a11y_audiogram_ear_empty, label)
        val thresholds =
            data.joinToString(separator = ", ") { (frequency, threshold) ->
                "${frequency.toInt()} Hz ${threshold.toInt()} dB relative"
            }
        return context.getString(R.string.a11y_audiogram_ear_thresholds, label, thresholds)
    }

    return context.getString(
        R.string.a11y_audiogram_chart_with_data,
        earDescription(context.getString(R.string.hearing_left), leftData),
        earDescription(context.getString(R.string.hearing_right), rightData),
    )
}
