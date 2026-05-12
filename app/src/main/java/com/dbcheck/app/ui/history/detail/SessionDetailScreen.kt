package com.dbcheck.app.ui.history.detail

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dbcheck.app.domain.noise.NoiseLevel
import com.dbcheck.app.domain.report.PeakEvent
import com.dbcheck.app.domain.report.SessionReportData
import com.dbcheck.app.ui.analytics.components.HeartRateOverlay
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.ProLockOverlay
import com.dbcheck.app.ui.history.components.SessionNamingSheet
import com.dbcheck.app.ui.theme.DbCheckTheme
import com.dbcheck.app.util.DurationFormatter
import com.dbcheck.app.util.PdfChartRenderer
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
    var showNamingSheet by remember { mutableStateOf(false) }
    val pdfLauncher =
        rememberLauncherForActivityResult(
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

    LaunchedEffect(Unit) {
        viewModel.sharePngIntents.collect { intent ->
            runCatching {
                context.startActivity(Intent.createChooser(intent, "Share session"))
            }.onFailure {
                viewModel.onSharePngUnavailable()
            }
        }
    }

    SessionDetailContent(
        state = uiState,
        onBack = onBack,
        onNavigateToUpgrade = onNavigateToUpgrade,
        onExportPdf = { pdfLauncher.launch(viewModel.suggestedPdfName()) },
        onEditMetadata = {
            if (uiState.isProUser) {
                showNamingSheet = true
            } else {
                onNavigateToUpgrade()
            }
        },
        onSharePng = viewModel::createSharePngIntent,
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
private fun SessionDetailContent(
    state: SessionDetailUiState,
    onBack: () -> Unit,
    onNavigateToUpgrade: () -> Unit,
    onExportPdf: () -> Unit,
    onEditMetadata: () -> Unit,
    onSharePng: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SessionDetailTopBar(
            onBack = onBack,
            title = state.report?.sessionName ?: "Session",
            showMetadataAction = state.report != null,
            isMetadataLocked = !state.isProUser,
            onEditMetadata = onEditMetadata,
        )

        when {
            state.isLoading -> LoadingDetail()
            state.isNotFound -> MissingDetail()
            state.report != null ->
                SessionDetailLoaded(
                    report = state.report,
                    state = state,
                    onNavigateToUpgrade = onNavigateToUpgrade,
                    onExportPdf = onExportPdf,
                    onSharePng = onSharePng,
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
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.material.onSurface)
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
                    contentDescription = if (isMetadataLocked) "Unlock session naming" else "Edit session",
                    tint = colors.material.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LoadingDetail() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Loading session...", color = DbCheckTheme.colorScheme.material.onSurfaceVariant)
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
                "Session Not Found",
                style = DbCheckTheme.typography.headlineMd,
                color = DbCheckTheme.colorScheme.material.onSurface,
            )
            Text(
                "This session is no longer available.",
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
        item {
            TimeSeriesCard(
                report = report,
                showHeartRateOverlay = state.isProUser && state.heartRateOverlayEnabled,
                heartRateSamples = state.heartRateSamples,
            )
        }
        item { PeakEventsCard(report.peakEvents) }
        item {
            ReportActions(
                state = state,
                onNavigateToUpgrade = onNavigateToUpgrade,
                onExportPdf = onExportPdf,
                onSharePng = onSharePng,
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
        Text("SESSION DETAIL", style = typography.labelMd, color = colors.material.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            report.sessionEmoji?.let { emoji ->
                Text(emoji, style = typography.headlineLg, color = colors.material.onSurface)
            }
            Text(report.sessionName, style = typography.headlineLg, color = colors.material.onSurface)
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
        Text("Duration ${report.durationLabel()}", style = typography.bodyMd, color = colors.material.onSurfaceVariant)
    }
}

@Composable
private fun KpiGrid(report: SessionReportData) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KpiCard("LAeq", "${report.laeqDb.formatOne()} dB", Modifier.weight(1f))
            KpiCard("LCpeak", "${report.lcPeakDb.formatOne()} dB", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KpiCard("TWA", "${report.twaDb.formatOne()} dB", Modifier.weight(1f))
            KpiCard("Dose", "${report.dosePercent.formatOne()}%", Modifier.weight(1f))
        }
    }
}

@Composable
private fun KpiCard(
    label: String,
    value: String,
    modifier: Modifier,
) {
    DbCheckCard(modifier = modifier.height(112.dp)) {
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
private fun TimeSeriesCard(
    report: SessionReportData,
    showHeartRateOverlay: Boolean,
    heartRateSamples: List<HeartRateSampleUiState>,
) {
    DbCheckCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "TIME SERIES",
                style = DbCheckTheme.typography.labelMd,
                color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
            )
            if (report.timeSeries.size < 2) {
                Text(
                    "No chart samples available",
                    style = DbCheckTheme.typography.bodyMd,
                    color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
                )
            } else {
                SessionTimeSeriesChart(report)
            }
            if (showHeartRateOverlay) {
                if (heartRateSamples.isEmpty()) {
                    Text(
                        "No Health Connect heart rate samples for this session",
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
    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(168.dp),
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
            drawLine(colors.ghostBorder, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
        drawPath(path, color = colors.material.primary, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun PeakEventsCard(events: List<PeakEvent>) {
    DbCheckCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "PEAK EVENTS",
                style = DbCheckTheme.typography.labelMd,
                color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
            )
            if (events.isEmpty()) {
                Text(
                    "No events at or above 85 dBA",
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
            "${event.maxDb.formatOne()} dB",
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
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ProLockOverlay(
            isLocked = !state.isProUser,
            onUpgradeClick = onNavigateToUpgrade,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(164.dp),
        ) {
            ExportPdfCard(isExporting = state.isExporting, onExportPdf = onExportPdf)
        }
        SharePngCard(onSharePng = onSharePng)
        state.message?.let { ActionMessage(it, isError = false) }
        state.errorMessage?.let { ActionMessage(it, isError = true) }
    }
}

@Composable
private fun ExportPdfCard(
    isExporting: Boolean,
    onExportPdf: () -> Unit,
) {
    DbCheckCard(modifier = Modifier.fillMaxWidth().height(164.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    Icons.Outlined.PictureAsPdf,
                    contentDescription = null,
                    tint = DbCheckTheme.colorScheme.material.primary,
                )
                Text(
                    "SCIENTIFIC PDF REPORT",
                    style = DbCheckTheme.typography.labelMd,
                    color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
                )
            }
            DbCheckButton(
                text = if (isExporting) "Exporting..." else "Export PDF",
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
                text = "Share PNG",
                onClick = onSharePng,
                style = DbCheckButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ActionMessage(
    text: String,
    isError: Boolean,
) {
    val color = if (isError) DbCheckTheme.colorScheme.material.error else DbCheckTheme.colorScheme.success
    Text(
        text = text,
        style = DbCheckTheme.typography.bodyMd,
        color = color,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
}

private fun SessionReportData.dateRangeLabel(): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return "${dateFormat.format(Date(startTime))} - ${dateFormat.format(Date(endTime))}"
}

private fun SessionReportData.durationLabel(): String = DurationFormatter.formatClockDuration(durationMs)

private fun PeakEvent.timeLabel(): String {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return timeFormat.format(Date(peakTime))
}

private fun Float.formatOne(): String = "%.1f".format(Locale.US, this)
