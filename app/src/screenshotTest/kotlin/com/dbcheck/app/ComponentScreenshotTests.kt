package com.dbcheck.app

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.dbcheck.app.data.local.preferences.model.WaveformStyle
import com.dbcheck.app.domain.audio.SpectralBandwidth
import com.dbcheck.app.domain.noise.NoiseLevel
import com.dbcheck.app.domain.noise.SoundReferenceCatalog
import com.dbcheck.app.ui.analytics.components.MonthlyTrendChart
import com.dbcheck.app.ui.analytics.components.SpectralAnalysisCard
import com.dbcheck.app.ui.analytics.components.YearlyReportCard
import com.dbcheck.app.ui.analytics.state.EnvironmentMixCategory
import com.dbcheck.app.ui.analytics.state.EnvironmentMixRowUiState
import com.dbcheck.app.ui.analytics.state.MonthlyTrendPointUiState
import com.dbcheck.app.ui.analytics.state.MonthlyTrendUiState
import com.dbcheck.app.ui.analytics.state.SpectralAnalysisUiState
import com.dbcheck.app.ui.analytics.state.SpectralBandUiState
import com.dbcheck.app.ui.analytics.state.YearlyReportUiState
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.SessionCard
import com.dbcheck.app.ui.components.SessionCardEditAction
import com.dbcheck.app.ui.components.SessionCardState
import com.dbcheck.app.ui.meter.MeterModeChipRow
import com.dbcheck.app.ui.meter.components.CircularGauge
import com.dbcheck.app.ui.meter.components.LiveSoundLevelChart
import com.dbcheck.app.ui.meter.components.MeterControls
import com.dbcheck.app.ui.meter.components.SoundReferenceCard
import com.dbcheck.app.ui.meter.components.WaveformVisualization
import com.dbcheck.app.ui.meter.state.LiveChartPointUiState
import com.dbcheck.app.ui.meter.state.MeasurementMode
import com.dbcheck.app.ui.theme.DbCheckTheme

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun ButtonStylesPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DbCheckButton(text = "Primary", onClick = {}, style = DbCheckButtonStyle.Primary)
            Spacer(modifier = Modifier.height(8.dp))
            DbCheckButton(text = "Secondary", onClick = {}, style = DbCheckButtonStyle.Secondary)
            Spacer(modifier = Modifier.height(8.dp))
            DbCheckButton(text = "Tertiary", onClick = {}, style = DbCheckButtonStyle.Tertiary)
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ButtonStylesDarkPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DbCheckButton(text = "Primary", onClick = {}, style = DbCheckButtonStyle.Primary)
            Spacer(modifier = Modifier.height(8.dp))
            DbCheckButton(text = "Secondary", onClick = {}, style = DbCheckButtonStyle.Secondary)
            Spacer(modifier = Modifier.height(8.dp))
            DbCheckButton(text = "Tertiary", onClick = {}, style = DbCheckButtonStyle.Tertiary)
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 300)
@Composable
fun CardPreview() {
    DbCheckTheme {
        DbCheckCard(modifier = Modifier.width(280.dp)) {
            Text(
                text = "42.5 dB",
                style = DbCheckTheme.typography.displayLg,
                color = DbCheckTheme.colorScheme.material.onSurface,
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 300, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CardDarkPreview() {
    DbCheckTheme {
        DbCheckCard(modifier = Modifier.width(280.dp)) {
            Text(
                text = "42.5 dB",
                style = DbCheckTheme.typography.displayLg,
                color = DbCheckTheme.colorScheme.material.onSurface,
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun MeterGaugePreview() {
    DbCheckTheme {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularGauge(
                currentDb = 82.4f,
                noiseLevel = NoiseLevel.ELEVATED,
                animationsEnabled = false,
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun MeterControlsPreview() {
    DbCheckTheme {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MeterControls(
                isRecording = false,
                isShareEnabled = false,
                onToggleRecording = {},
                onReset = {},
                onShare = {},
            )
            Spacer(modifier = Modifier.height(16.dp))
            MeterControls(
                isRecording = true,
                isShareEnabled = true,
                onToggleRecording = {},
                onReset = {},
                onShare = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun MeterModeChipRowFreePreview() {
    DbCheckTheme {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            MeterModeChipRow(
                measurementMode = MeasurementMode.DB_METER,
                isProUser = false,
                onSelectMode = {},
                onLockedDosimeterClick = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun MeterModeChipRowProPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            MeterModeChipRow(
                measurementMode = MeasurementMode.DOSIMETER,
                isProUser = true,
                onSelectMode = {},
                onLockedDosimeterClick = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun LiveSoundLevelChartEmptyPreview() {
    DbCheckTheme {
        LiveSoundLevelChartPreviewContainer {
            LiveSoundLevelChart(
                points = emptyList(),
                isRecording = false,
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun LiveSoundLevelChartActivePreview() {
    DbCheckTheme {
        LiveSoundLevelChartPreviewContainer {
            LiveSoundLevelChart(
                points = previewLiveChartData,
                isRecording = true,
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun LiveSoundLevelChartActiveDarkPreview() {
    DbCheckTheme {
        LiveSoundLevelChartPreviewContainer {
            LiveSoundLevelChart(
                points = previewLiveChartData,
                isRecording = true,
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun LiveSoundLevelChartPausedDarkPreview() {
    DbCheckTheme {
        LiveSoundLevelChartPreviewContainer {
            LiveSoundLevelChart(
                points = previewLiveChartData.take(8),
                isRecording = false,
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun SoundReferenceCardCollapsedPreview() {
    DbCheckTheme {
        SoundReferenceCardPreviewContainer {
            SoundReferenceCard(
                currentDb = 67f,
                markers = SoundReferenceCatalog.referenceMarkers,
                nearestMarker = SoundReferenceCatalog.nearestReferenceMarker(67f),
                currentPosition = SoundReferenceCatalog.markerPosition(67f),
                expanded = false,
                onExpandedChange = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SoundReferenceCardExpandedDarkPreview() {
    DbCheckTheme {
        SoundReferenceCardPreviewContainer {
            SoundReferenceCard(
                currentDb = 101f,
                markers = SoundReferenceCatalog.referenceMarkers,
                nearestMarker = SoundReferenceCatalog.nearestReferenceMarker(101f),
                currentPosition = SoundReferenceCatalog.markerPosition(101f),
                expanded = true,
                onExpandedChange = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun WaveformStylesPreview() {
    DbCheckTheme {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WaveformVisualization(data = previewWaveformData, style = WaveformStyle.LINE)
            WaveformVisualization(data = previewWaveformData, style = WaveformStyle.FILLED)
            WaveformVisualization(data = previewWaveformData, style = WaveformStyle.BARS)
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun SessionCardPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SessionCard(
                state =
                    SessionCardState(
                        emoji = "dB",
                        title = "Warehouse calibration run with a longer title",
                        metadata = "18 MIN / 68 AVG / A-WEIGHTED",
                        peakDb = 94f,
                        avgDb = 68f,
                        tags = listOf("workshop", "calibration", "shift-a"),
                    ),
                editAction = SessionCardEditAction(isLocked = true, onClick = {}),
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun SpectralAnalysisLockedPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SpectralAnalysisCard(
                spectralState = SpectralAnalysisUiState.LockedPreview,
                isLocked = true,
                onUpgradeClick = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun SpectralAnalysisIdlePreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SpectralAnalysisCard(
                spectralState = SpectralAnalysisUiState.Idle,
                isLocked = false,
                onUpgradeClick = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun SpectralAnalysisLivePreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SpectralAnalysisCard(
                spectralState =
                    SpectralAnalysisUiState.Live(
                        bands =
                            List(24) { index ->
                                SpectralBandUiState(
                                    normalizedAmplitude = ((index % 8) + 1) / 8f,
                                )
                            },
                        dominantFrequencyHz = 2400f,
                        bandwidth = SpectralBandwidth.WIDE,
                    ),
                isLocked = false,
                onUpgradeClick = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun MonthlyTrendLockedPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            MonthlyTrendChart(
                monthlyTrendState = MonthlyTrendUiState.LockedPreview,
                isLocked = true,
                onUpgradeClick = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun MonthlyTrendDataPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            MonthlyTrendChart(
                monthlyTrendState =
                    MonthlyTrendUiState.Data(
                        points =
                            List(30) { index ->
                                MonthlyTrendPointUiState(
                                    dayStartMs = index.toLong(),
                                    laeqDb =
                                        if (index % 5 == 0) {
                                            null
                                        } else {
                                            58f + (index % 8) * 3f
                                        },
                                    maxDb =
                                        if (index % 5 == 0) {
                                            null
                                        } else {
                                            72f + (index % 8) * 2f
                                        },
                                )
                            },
                        laeqDb = 68.4f,
                        loudestDb = 91.2f,
                    ),
                isLocked = false,
                onUpgradeClick = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun MonthlyTrendEmptyPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            MonthlyTrendChart(
                monthlyTrendState = MonthlyTrendUiState.Empty,
                isLocked = false,
                onUpgradeClick = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun YearlyReportLockedPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            YearlyReportCard(
                yearlyReportState = YearlyReportUiState.LockedPreview,
                isLocked = true,
                onUpgradeClick = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun YearlyReportDataPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            YearlyReportCard(
                yearlyReportState =
                    YearlyReportUiState.Data(
                        totalSessions = 86,
                        laeqDb = 67.8f,
                        loudestDayLabel = "May 8",
                        loudestDb = 94.2f,
                        zoneRows =
                            listOf(
                                EnvironmentMixRowUiState(EnvironmentMixCategory.QUIET, 34),
                                EnvironmentMixRowUiState(EnvironmentMixCategory.MODERATE, 42),
                                EnvironmentMixRowUiState(EnvironmentMixCategory.LOUD, 18),
                                EnvironmentMixRowUiState(EnvironmentMixCategory.CRITICAL, 6),
                            ),
                    ),
                isLocked = false,
                onUpgradeClick = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun YearlyReportEmptyPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            YearlyReportCard(
                yearlyReportState = YearlyReportUiState.Empty,
                isLocked = false,
                onUpgradeClick = {},
            )
        }
    }
}

private val previewWaveformData =
    listOf(
        0.10f,
        0.25f,
        0.70f,
        0.35f,
        0.55f,
        0.92f,
        0.42f,
        0.18f,
        0.64f,
        0.30f,
        0.78f,
        0.48f,
        0.22f,
        0.58f,
        0.36f,
        0.68f,
    )

@Composable
private fun SoundReferenceCardPreviewContainer(content: @Composable () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(DbCheckTheme.colorScheme.material.surface)
                .padding(16.dp),
    ) {
        content()
    }
}

@Composable
private fun LiveSoundLevelChartPreviewContainer(content: @Composable () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(DbCheckTheme.colorScheme.material.surface)
                .padding(16.dp),
    ) {
        content()
    }
}

private val previewLiveChartData =
    listOf(
        LiveChartPointUiState(timestampMs = 0L, db = 54f),
        LiveChartPointUiState(timestampMs = 4_000L, db = 62f),
        LiveChartPointUiState(timestampMs = 8_000L, db = 69f),
        LiveChartPointUiState(timestampMs = 12_000L, db = 82f),
        LiveChartPointUiState(timestampMs = 16_000L, db = 91f),
        LiveChartPointUiState(timestampMs = 20_000L, db = 76f),
        LiveChartPointUiState(timestampMs = 24_000L, db = 88f),
        LiveChartPointUiState(timestampMs = 30_000L, db = 72f),
    )
