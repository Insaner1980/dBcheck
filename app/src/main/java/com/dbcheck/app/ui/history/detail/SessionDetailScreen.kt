@file:Suppress("TooManyFunctions")

package com.dbcheck.app.ui.history.detail

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dbcheck.app.R
import com.dbcheck.app.domain.noise.NoiseLevel
import com.dbcheck.app.domain.report.DbHistogramBucket
import com.dbcheck.app.domain.report.PeakEvent
import com.dbcheck.app.domain.report.SessionReportData
import com.dbcheck.app.ui.analytics.components.HeartRateOverlay
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.InlineStatusRow
import com.dbcheck.app.ui.components.InlineStatusTone
import com.dbcheck.app.ui.components.ProLockOverlay
import com.dbcheck.app.ui.history.components.SessionNamingSheet
import com.dbcheck.app.ui.theme.ChartTokens
import com.dbcheck.app.ui.theme.DbCheckTheme
import com.dbcheck.app.util.PdfChartRenderer
import com.dbcheck.app.util.ReportTextFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SessionDetailScreen(
    onBack: () -> Unit,
    onNavigateToUpgrade: () -> Unit,
    viewModel: SessionDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val shareChooserTitle = stringResource(R.string.report_share_chooser)
    var showNamingSheet by remember { mutableStateOf(false) }
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        uri?.let(viewModel::exportPdf)
    }

    LaunchedEffect(uiState.message, uiState.errorMessage) {
        if (uiState.message != null || uiState.errorMessage != null) {
            kotlinx.coroutines.delay(3_000L)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(shareChooserTitle) {
        viewModel.sharePngIntents.collect { intent ->
            runCatching {
                context.startActivity(Intent.createChooser(intent, shareChooserTitle))
            }.onFailure {
                viewModel.onSharePngUnavailable()
            }
        }
    }

    LaunchedEffect(shareChooserTitle) {
        viewModel.shareWavIntents.collect { intent ->
            runCatching {
                context.startActivity(Intent.createChooser(intent, shareChooserTitle))
            }.onFailure {
                viewModel.onShareWavUnavailable()
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshHeartRateState()
    }
    val contentActions =
        sessionDetailContentActions(
            state = uiState,
            viewModel = viewModel,
            onBack = onBack,
            onNavigateToUpgrade = onNavigateToUpgrade,
            onLaunchPdfExport = { pdfLauncher.launch(viewModel.suggestedPdfName()) },
            onShowNamingSheet = { showNamingSheet = true },
        )

    SessionDetailContent(
        state = uiState,
        actions = contentActions,
    )

    if (showNamingSheet) {
        uiState.report?.let { report ->
            SessionNamingSheet(
                currentName = report.sessionCustomName.orEmpty(),
                currentEmoji = report.sessionEmoji ?: "🎧",
                currentTags = report.sessionTags,
                onDismiss = { showNamingSheet = false },
                onSave = { name, emoji, tags ->
                    viewModel.saveSessionMetadata(name, emoji, tags)
                    showNamingSheet = false
                },
            )
        }
    }
}

@Composable
private fun SessionDetailContent(state: SessionDetailUiState, actions: SessionDetailContentActions) {
    Column(modifier = Modifier.fillMaxSize()) {
        SessionDetailTopBar(
            onBack = actions.onBack,
            title = state.report?.sessionName ?: stringResource(R.string.report_session_default_title),
            showMetadataAction = state.report != null,
            isMetadataLocked = !state.isProUser,
            onEditMetadata = actions.onEditMetadata,
        )

        when {
            state.isLoading -> LoadingDetail()

            state.isHistoryLocked -> LockedHistoryDetail(actions.onNavigateToUpgrade)

            state.isNotFound -> MissingDetail()

            state.report != null ->
                SessionDetailLoaded(
                    report = state.report,
                    state = state,
                    onNavigateToUpgrade = actions.onNavigateToUpgrade,
                    onExportPdf = actions.onExportPdf,
                    onSharePng = actions.onSharePng,
                    onShareWav = actions.onShareWav,
                    onDeleteWav = actions.onDeleteWav,
                )
        }
    }
}

@Composable
private fun SessionDetailTopBar(
    onBack: () -> Unit,
    title: String,
    showMetadataAction: Boolean,
    isMetadataLocked: Boolean,
    onEditMetadata: () -> Unit,
) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.a11y_back),
                tint = colors.material.onSurface,
            )
        }
        Text(
            text = title,
            style = typography.bodyLg,
            color = colors.material.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (showMetadataAction) {
            IconButton(onClick = onEditMetadata) {
                Icon(
                    imageVector = if (isMetadataLocked) Icons.Outlined.Lock else Icons.Outlined.Edit,
                    contentDescription =
                        if (isMetadataLocked) {
                            stringResource(R.string.session_unlock_naming_content_description)
                        } else {
                            stringResource(R.string.session_edit_content_description)
                        },
                    tint = colors.material.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LoadingDetail() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            stringResource(R.string.report_loading_session),
            color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
        )
    }
}

@Composable
private fun LockedHistoryDetail(onNavigateToUpgrade: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = null,
                tint = DbCheckTheme.colorScheme.material.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
            Text(
                stringResource(R.string.report_unlimited_history_title),
                style = DbCheckTheme.typography.headlineMd,
                color = DbCheckTheme.colorScheme.material.onSurface,
            )
            Text(
                stringResource(R.string.report_session_requires_pro),
                style = DbCheckTheme.typography.bodyMd,
                color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
            )
            DbCheckButton(
                text = stringResource(R.string.action_upgrade),
                onClick = onNavigateToUpgrade,
                style = DbCheckButtonStyle.Primary,
            )
        }
    }
}

@Composable
private fun MissingDetail() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                Icons.Outlined.PictureAsPdf,
                contentDescription = null,
                tint = DbCheckTheme.colorScheme.material.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
            Text(
                stringResource(R.string.report_session_not_found_title),
                style = DbCheckTheme.typography.headlineMd,
                color = DbCheckTheme.colorScheme.material.onSurface,
            )
            Text(
                stringResource(R.string.report_session_not_found_message),
                style = DbCheckTheme.typography.bodyMd,
                color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SessionDetailLoaded(
    report: SessionReportData,
    state: SessionDetailUiState,
    onNavigateToUpgrade: () -> Unit,
    onExportPdf: () -> Unit,
    onSharePng: () -> Unit,
    onShareWav: () -> Unit,
    onDeleteWav: () -> Unit,
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space4),
    ) {
        item { SessionSummary(report) }
        item { KpiGrid(report) }
        state.sleepResults?.let { sleepResults ->
            item { SleepResultsCard(sleepResults) }
        }
        state.sleepInsights?.let { sleepInsights ->
            item { SleepInsightsCard(sleepInsights) }
        }
        item {
            TimeSeriesCard(
                report = report,
                showHeartRateOverlay = state.isProUser && state.heartRateOverlayEnabled,
                heartRateSamples = state.heartRateSamples,
                heartRateUnavailableMessage = state.heartRateUnavailableMessage,
            )
        }
        item {
            DbHistogramCard(
                buckets = report.dbHistogramBuckets,
                isLocked = !state.isProUser,
                onUpgradeClick = onNavigateToUpgrade,
            )
        }
        item { PeakEventsCard(report) }
        item {
            ReportActions(
                state = state,
                onNavigateToUpgrade = onNavigateToUpgrade,
                onExportPdf = onExportPdf,
                onSharePng = onSharePng,
                onShareWav = onShareWav,
                onDeleteWav = onDeleteWav,
            )
        }
        item { Spacer(Modifier.height(DbCheckTheme.spacing.space4)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SessionSummary(report: SessionReportData) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(R.string.report_session_detail),
            style = typography.labelMd,
            color = colors.material.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            report.sessionEmoji?.let { emoji ->
                Text(emoji, style = typography.headlineLg, color = colors.material.onSurface)
            }
            Text(
                text = report.sessionName,
                style = typography.headlineLg,
                color = colors.material.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        if (report.sessionTags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                report.sessionTags.forEach { tag ->
                    Text("#$tag", style = typography.labelMd, color = colors.material.primary)
                }
            }
        }
        Text(report.dateRangeLabel(), style = typography.bodyMd, color = colors.material.onSurfaceVariant)
        Text(
            stringResource(R.string.report_duration_label, report.durationLabel()),
            style = typography.bodyMd,
            color = colors.material.onSurfaceVariant,
        )
    }
}

@Composable
private fun KpiGrid(report: SessionReportData) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KpiCard(
                report.equivalentLevelLabel,
                "${ReportTextFormatter.oneDecimal(report.laeqDb)} dB",
                Modifier.weight(1f),
            )
            KpiCard(
                stringResource(R.string.report_metric_lcpeak),
                "${ReportTextFormatter.oneDecimal(report.lcPeakDb)} dB",
                Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KpiCard(
                stringResource(R.string.report_metric_twa),
                ReportTextFormatter.oneDecimalOrUnavailable(
                    report.twaDb,
                    " dB",
                    stringResource(R.string.value_unavailable),
                ),
                Modifier.weight(1f),
            )
            KpiCard(
                stringResource(R.string.report_metric_dose),
                ReportTextFormatter.oneDecimalOrUnavailable(
                    report.dosePercent,
                    "%",
                    stringResource(R.string.value_unavailable),
                ),
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun KpiCard(label: String, value: String, modifier: Modifier) {
    DbCheckCard(modifier = modifier.heightIn(min = 112.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                label.uppercase(),
                style = DbCheckTheme.typography.labelMd,
                color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
            )
            Text(value, style = DbCheckTheme.typography.dataXl, color = DbCheckTheme.colorScheme.material.onSurface)
        }
    }
}

@Composable
internal fun SleepResultsCard(state: SleepResultsUiState, modifier: Modifier = Modifier) {
    val unavailable = stringResource(R.string.value_unavailable)
    DbCheckCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                stringResource(R.string.sleep_results_title).uppercase(),
                style = DbCheckTheme.typography.labelMd,
                color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SleepResultsMetric(
                    label = stringResource(R.string.sleep_results_target),
                    value = ReportTextFormatter.duration(state.targetDurationMinutes * 60_000L),
                    modifier = Modifier.weight(1f),
                )
                SleepResultsMetric(
                    label = stringResource(R.string.sleep_results_recorded),
                    value = ReportTextFormatter.duration(state.recordedDurationMs),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SleepResultsMetric(
                    label = state.equivalentLevelLabel,
                    value = "${ReportTextFormatter.oneDecimal(state.equivalentLevelDb)} dB",
                    modifier = Modifier.weight(1f),
                )
                SleepResultsMetric(
                    label = stringResource(R.string.report_metric_max),
                    value = "${ReportTextFormatter.oneDecimal(state.maxDb)} dB",
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SleepResultsMetric(
                    label = stringResource(R.string.report_metric_lcpeak),
                    value = "${ReportTextFormatter.oneDecimal(state.lcPeakDb)} dB",
                    modifier = Modifier.weight(1f),
                )
                SleepResultsMetric(
                    label = stringResource(R.string.sleep_results_peak_events),
                    value = state.peakEventCount?.toString() ?: unavailable,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SleepResultsMetric(
                    label = stringResource(R.string.sleep_results_loud_periods),
                    value = state.loudPeriodCount?.toString() ?: unavailable,
                    modifier = Modifier.weight(1f),
                )
                SleepResultsMetric(
                    label = stringResource(R.string.report_metric_samples),
                    value = state.sampleCount?.toString() ?: unavailable,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
internal fun SleepInsightsCard(state: SleepInsightsUiState, modifier: Modifier = Modifier) {
    DbCheckCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.sleep_insights_title).uppercase(),
                style = DbCheckTheme.typography.labelMd,
                color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
            )
            when {
                !state.isAvailable ->
                    Text(
                        text = stringResource(R.string.sleep_insights_missing_data),
                        style = DbCheckTheme.typography.bodyMd,
                        color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
                    )

                state.notableEventCount == 0 ->
                    Text(
                        text = stringResource(R.string.sleep_insights_quiet_summary),
                        style = DbCheckTheme.typography.bodyMd,
                        color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
                    )

                else ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SleepResultsMetric(
                            label = stringResource(R.string.sleep_insights_notable_events),
                            value = state.notableEventCount?.toString().orEmpty(),
                            modifier = Modifier.weight(1f),
                        )
                        SleepResultsMetric(
                            label = stringResource(R.string.sleep_insights_loudest_period),
                            value = state.loudestPeriod?.label().orEmpty(),
                            modifier = Modifier.weight(1f),
                        )
                    }
            }
        }
    }
}

private fun SleepInsightPeriodUiState.label(): String =
    "${ReportTextFormatter.duration(durationMs)} / ${ReportTextFormatter.oneDecimal(maxDb)} dB"

@Composable
private fun SleepResultsMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label.uppercase(),
            style = DbCheckTheme.typography.labelSm,
            color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
        )
        Text(
            value,
            style = DbCheckTheme.typography.dataMd,
            color = DbCheckTheme.colorScheme.material.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TimeSeriesCard(
    report: SessionReportData,
    showHeartRateOverlay: Boolean,
    heartRateSamples: List<HeartRateSampleUiState>,
    heartRateUnavailableMessage: String?,
) {
    DbCheckCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                stringResource(R.string.report_section_time_series).uppercase(),
                style = DbCheckTheme.typography.labelMd,
                color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
            )
            if (report.timeSeries.isEmpty()) {
                Text(
                    stringResource(R.string.last_24_hours_no_chart_samples),
                    style = DbCheckTheme.typography.bodyMd,
                    color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
                )
            } else {
                SessionTimeSeriesChart(report)
            }
            if (heartRateUnavailableMessage != null) {
                Text(
                    heartRateUnavailableMessage,
                    style = DbCheckTheme.typography.bodyMd,
                    color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
                )
            } else if (showHeartRateOverlay) {
                if (heartRateSamples.isEmpty()) {
                    Text(
                        stringResource(R.string.report_heart_rate_empty),
                        style = DbCheckTheme.typography.bodyMd,
                        color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
                    )
                } else {
                    HeartRateOverlay(
                        samples = heartRateSamples,
                        startTimeMs = report.startTime,
                        endTimeMs = report.endTime,
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionTimeSeriesChart(report: SessionReportData) {
    val colors = DbCheckTheme.colorScheme
    val resources = LocalResources.current
    val chartDescription =
        resources.getQuantityString(
            R.plurals.report_time_series_chart_description,
            report.measurementCount,
            report.measurementCount,
            report.durationLabel(),
            ReportTextFormatter.oneDecimal(report.laeqDb),
            ReportTextFormatter.oneDecimal(report.minDb),
            ReportTextFormatter.oneDecimal(report.maxDb),
            report.equivalentLevelLabel,
        )
    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(168.dp)
                .semantics {
                    contentDescription = chartDescription
                },
    ) {
        val mapped =
            PdfChartRenderer.mapTimeSeries(
                points = report.timeSeries,
                width = size.width,
                height = size.height,
                minDb = report.minDb.coerceAtMost(NoiseLevel.QUIET.maxDb),
                maxDb = report.maxDb.coerceAtLeast(100f),
            )
        val path =
            Path().apply {
                mapped.forEachIndexed { index, point ->
                    if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                }
            }
        repeat(4) { index ->
            val y = size.height * index / 3f
            drawLine(
                colors.ghostBorder,
                Offset(0f, y),
                Offset(size.width, y),
                strokeWidth = ChartTokens.GridLineWidth.toPx(),
            )
        }
        if (mapped.size == 1) {
            drawCircle(
                color = colors.material.primary,
                radius = ChartTokens.PointRadius.toPx(),
                center = Offset(mapped[0].x, mapped[0].y),
            )
        } else {
            drawPath(
                path,
                color = colors.material.primary,
                style = Stroke(width = ChartTokens.LineWidth.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DbHistogramCard(
    buckets: List<DbHistogramBucket>,
    isLocked: Boolean,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProLockOverlay(
        isLocked = isLocked,
        onUpgradeClick = onUpgradeClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        DbHistogramCardContent(
            buckets =
                if (isLocked) {
                    LOCKED_PREVIEW_HISTOGRAM_BUCKETS
                } else {
                    buckets
                },
            isLocked = isLocked,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DbHistogramCardContent(buckets: List<DbHistogramBucket>, isLocked: Boolean, modifier: Modifier) {
    val visibleBuckets = buckets.visibleHistogramBuckets()

    DbCheckCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.report_section_db_distribution).uppercase(),
                style = DbCheckTheme.typography.labelMd,
                color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
            )
            if (visibleBuckets.isEmpty()) {
                Text(
                    stringResource(R.string.report_histogram_empty),
                    style = DbCheckTheme.typography.bodyMd,
                    color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
                )
            } else {
                DbHistogramBars(
                    buckets = buckets,
                    contentDescription = dbHistogramContentDescription(buckets = buckets, isLocked = isLocked),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    visibleBuckets.forEach { bucket ->
                        HistogramBucketLegendRow(bucket)
                    }
                }
            }
        }
    }
}

@Composable
private fun DbHistogramBars(buckets: List<DbHistogramBucket>, contentDescription: String) {
    val colors = DbCheckTheme.colorScheme
    val barColors = buckets.map { it.histogramColor() }
    val gridColor = colors.ghostBorder

    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(132.dp)
                .semantics {
                    this.contentDescription = contentDescription
                },
    ) {
        repeat(4) { index ->
            val y = size.height * index / 3f
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = ChartTokens.GridLineWidth.toPx())
        }

        if (buckets.isEmpty()) return@Canvas

        val gap = 4.dp.toPx()
        val barWidth = ((size.width - gap * (buckets.size - 1)) / buckets.size).coerceAtLeast(0f)
        val maxPercent = buckets.maxOfOrNull { it.percent }?.coerceAtLeast(1) ?: 1
        buckets.forEachIndexed { index, bucket ->
            val normalizedHeight = bucket.percent.toFloat() / maxPercent.toFloat()
            val minVisibleHeight = if (bucket.sampleCount > 0) 2.dp.toPx() else 0f
            val barHeight = (size.height * normalizedHeight).coerceAtLeast(minVisibleHeight)
            if (barHeight > 0f) {
                drawRoundRect(
                    color = barColors[index],
                    topLeft = Offset(index * (barWidth + gap), size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(ChartTokens.BarRadius.toPx(), ChartTokens.BarRadius.toPx()),
                )
            }
        }
    }
}

@Composable
private fun HistogramBucketLegendRow(bucket: DbHistogramBucket) {
    val color = bucket.histogramColor()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color),
        )
        Text(
            text =
                stringResource(
                    R.string.report_histogram_bucket_percent,
                    bucket.minDb,
                    bucket.maxDb,
                    bucket.percent,
                ),
            style = DbCheckTheme.typography.labelSm,
            color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
        )
    }
}

@Composable
private fun DbHistogramBucket.histogramColor(): Color {
    val colors = DbCheckTheme.colorScheme
    return when (NoiseLevel.fromDb((minDb + maxDb) / 2f)) {
        NoiseLevel.QUIET -> colors.material.primary.copy(alpha = 0.62f)
        NoiseLevel.NORMAL -> colors.success.copy(alpha = 0.78f)
        NoiseLevel.ELEVATED -> colors.warning.copy(alpha = 0.88f)
        NoiseLevel.DANGEROUS -> colors.material.error.copy(alpha = 0.88f)
    }
}

@Composable
private fun dbHistogramContentDescription(buckets: List<DbHistogramBucket>, isLocked: Boolean): String {
    if (isLocked) return stringResource(R.string.a11y_report_histogram_locked)

    val summary = dbHistogramAccessibilitySummary(buckets)
    return if (summary.isBlank()) {
        stringResource(R.string.a11y_report_histogram_empty)
    } else {
        stringResource(R.string.a11y_report_histogram_with_data, summary)
    }
}

internal fun dbHistogramAccessibilitySummary(buckets: List<DbHistogramBucket>): String =
    buckets.visibleHistogramBuckets().joinToString(separator = ", ") { bucket ->
        "${bucket.minDb}-${bucket.maxDb} dB ${bucket.percent}%"
    }

private fun List<DbHistogramBucket>.visibleHistogramBuckets(): List<DbHistogramBucket> =
    filter { bucket -> bucket.sampleCount > 0 || bucket.percent > 0 }

private val LOCKED_PREVIEW_HISTOGRAM_BUCKETS =
    listOf(
        DbHistogramBucket(minDb = 0, maxDb = 10, sampleCount = 0, percent = 0),
        DbHistogramBucket(minDb = 10, maxDb = 20, sampleCount = 0, percent = 0),
        DbHistogramBucket(minDb = 20, maxDb = 30, sampleCount = 1, percent = 4),
        DbHistogramBucket(minDb = 30, maxDb = 40, sampleCount = 2, percent = 8),
        DbHistogramBucket(minDb = 40, maxDb = 50, sampleCount = 4, percent = 15),
        DbHistogramBucket(minDb = 50, maxDb = 60, sampleCount = 6, percent = 23),
        DbHistogramBucket(minDb = 60, maxDb = 70, sampleCount = 5, percent = 19),
        DbHistogramBucket(minDb = 70, maxDb = 80, sampleCount = 4, percent = 15),
        DbHistogramBucket(minDb = 80, maxDb = 90, sampleCount = 2, percent = 8),
        DbHistogramBucket(minDb = 90, maxDb = 100, sampleCount = 1, percent = 4),
        DbHistogramBucket(minDb = 100, maxDb = 110, sampleCount = 1, percent = 4),
        DbHistogramBucket(minDb = 110, maxDb = 120, sampleCount = 0, percent = 0),
        DbHistogramBucket(minDb = 120, maxDb = 130, sampleCount = 0, percent = 0),
    )

@Composable
private fun PeakEventsCard(report: SessionReportData) {
    val events = report.peakEvents
    DbCheckCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.report_section_peak_events).uppercase(),
                style = DbCheckTheme.typography.labelMd,
                color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
            )
            if (!report.aWeightedExposureMetricsAvailable) {
                Text(
                    stringResource(R.string.report_a_weighted_peak_unavailable),
                    style = DbCheckTheme.typography.bodyMd,
                    color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
                )
            } else if (events.isEmpty()) {
                Text(
                    stringResource(R.string.report_no_peak_events),
                    style = DbCheckTheme.typography.bodyMd,
                    color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
                )
            } else {
                events.take(5).forEach { event -> PeakEventRow(event) }
            }
        }
    }
}

@Composable
private fun PeakEventRow(event: PeakEvent) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            event.timeLabel(),
            style = DbCheckTheme.typography.bodyMd,
            color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
        )
        Text(
            "${ReportTextFormatter.oneDecimal(event.maxDb)} dB",
            style = DbCheckTheme.typography.dataMd,
            color = DbCheckTheme.colorScheme.warning,
        )
    }
}

@Composable
private fun ReportActions(
    state: SessionDetailUiState,
    onNavigateToUpgrade: () -> Unit,
    onExportPdf: () -> Unit,
    onSharePng: () -> Unit,
    onShareWav: () -> Unit,
    onDeleteWav: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ProLockOverlay(
            isLocked = !state.isProUser,
            onUpgradeClick = onNavigateToUpgrade,
            modifier = Modifier.fillMaxWidth(),
        ) {
            ExportPdfCard(isExporting = state.isExporting, onExportPdf = onExportPdf)
        }
        SharePngCard(onSharePng = onSharePng)
        if (state.hasWavRecording) {
            WavRecordingCard(
                isProUser = state.isProUser,
                onShareWav = onShareWav,
                onDeleteWav = onDeleteWav,
            )
        }
        state.message?.let { InlineStatusRow(text = it, tone = InlineStatusTone.Success) }
        state.errorMessage?.let { InlineStatusRow(text = it, tone = InlineStatusTone.Error) }
    }
}

@Composable
private fun ExportPdfCard(isExporting: Boolean, onExportPdf: () -> Unit) {
    DbCheckCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    Icons.Outlined.PictureAsPdf,
                    contentDescription = null,
                    tint = DbCheckTheme.colorScheme.material.primary,
                )
                Text(
                    stringResource(R.string.report_scientific_pdf_report),
                    style = DbCheckTheme.typography.labelMd,
                    color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
                )
            }
            DbCheckButton(
                text =
                    if (isExporting) {
                        stringResource(R.string.report_exporting_pdf)
                    } else {
                        stringResource(R.string.action_export_pdf)
                    },
                onClick = onExportPdf,
                enabled = !isExporting,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SharePngCard(onSharePng: () -> Unit) {
    DbCheckCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Share, contentDescription = null, tint = DbCheckTheme.colorScheme.material.primary)
            DbCheckButton(
                text = stringResource(R.string.action_share_png),
                onClick = onSharePng,
                style = DbCheckButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun WavRecordingCard(isProUser: Boolean, onShareWav: () -> Unit, onDeleteWav: () -> Unit) {
    DbCheckCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Share, contentDescription = null, tint = DbCheckTheme.colorScheme.material.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.report_wav_recording_title),
                        style = DbCheckTheme.typography.labelMd,
                        color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
                    )
                    Text(
                        stringResource(R.string.report_wav_recording_subtitle),
                        style = DbCheckTheme.typography.bodyMd,
                        color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DbCheckButton(
                    text = stringResource(R.string.action_share_wav),
                    onClick = onShareWav,
                    style = if (isProUser) DbCheckButtonStyle.Primary else DbCheckButtonStyle.Secondary,
                    modifier = Modifier.weight(1f),
                )
                DbCheckButton(
                    text = stringResource(R.string.action_delete_wav),
                    onClick = onDeleteWav,
                    style = DbCheckButtonStyle.Secondary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun SessionReportData.dateRangeLabel(): String =
    ReportTextFormatter.dateRange(startTime, endTime, SESSION_DETAIL_DATE_PATTERN)

private fun SessionReportData.durationLabel(): String = ReportTextFormatter.duration(durationMs)

private fun PeakEvent.timeLabel(): String {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return timeFormat.format(Date(peakTime))
}

private const val SESSION_DETAIL_DATE_PATTERN = "MMM dd, yyyy HH:mm"
