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
import com.dbcheck.app.domain.ambient.AmbientSoundPreset
import com.dbcheck.app.domain.audio.ResponseTime
import com.dbcheck.app.domain.audio.SpectralBandwidth
import com.dbcheck.app.domain.audio.WeightingType
import com.dbcheck.app.domain.calibration.OctaveCalibrationOffsets
import com.dbcheck.app.domain.noise.DosimeterStandard
import com.dbcheck.app.domain.noise.NoiseLevel
import com.dbcheck.app.domain.noise.NoiseNotificationSchedule
import com.dbcheck.app.domain.noise.SoundReferenceCatalog
import com.dbcheck.app.domain.report.DbHistogramBucket
import com.dbcheck.app.ui.ambient.AmbientSoundPlaybackContent
import com.dbcheck.app.ui.ambient.AmbientSoundPlaybackUiState
import com.dbcheck.app.ui.analytics.components.AnalyticsSectionChipRow
import com.dbcheck.app.ui.analytics.components.MonthlyTrendChart
import com.dbcheck.app.ui.analytics.components.SoundDetectionCard
import com.dbcheck.app.ui.analytics.components.SpectralAnalysisCard
import com.dbcheck.app.ui.analytics.components.SpectralAnalysisCardActions
import com.dbcheck.app.ui.analytics.components.SpectralAnalysisCardState
import com.dbcheck.app.ui.analytics.components.YearlyReportCard
import com.dbcheck.app.ui.analytics.state.AnalyticsSection
import com.dbcheck.app.ui.analytics.state.EnvironmentMixCategory
import com.dbcheck.app.ui.analytics.state.EnvironmentMixRowUiState
import com.dbcheck.app.ui.analytics.state.MonthlyTrendPointUiState
import com.dbcheck.app.ui.analytics.state.MonthlyTrendUiState
import com.dbcheck.app.ui.analytics.state.RtaBandUiState
import com.dbcheck.app.ui.analytics.state.RtaUiState
import com.dbcheck.app.ui.analytics.state.SpectralAnalysisUiState
import com.dbcheck.app.ui.analytics.state.SpectralBandUiState
import com.dbcheck.app.ui.analytics.state.SpectralMode
import com.dbcheck.app.ui.analytics.state.SoundDetectionChipUiState
import com.dbcheck.app.ui.analytics.state.SoundDetectionUiState
import com.dbcheck.app.ui.analytics.state.SpectrogramRowUiState
import com.dbcheck.app.ui.analytics.state.SpectrogramUiState
import com.dbcheck.app.ui.analytics.state.YearlyReportUiState
import com.dbcheck.app.ui.camera.CameraOverlayScreen
import com.dbcheck.app.ui.camera.CameraPermissionStatus
import com.dbcheck.app.ui.camera.CameraPreviewUnavailableContent
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.SessionCard
import com.dbcheck.app.ui.components.SessionCardEditAction
import com.dbcheck.app.ui.components.SessionCardState
import com.dbcheck.app.ui.history.detail.DbHistogramCard
import com.dbcheck.app.ui.history.detail.SleepInsightPeriodUiState
import com.dbcheck.app.ui.history.detail.SleepInsightsCard
import com.dbcheck.app.ui.history.detail.SleepInsightsUiState
import com.dbcheck.app.ui.history.detail.SleepResultsCard
import com.dbcheck.app.ui.history.detail.SleepResultsUiState
import com.dbcheck.app.ui.history.components.HistorySearchControls
import com.dbcheck.app.ui.history.components.HistorySearchControlsActions
import com.dbcheck.app.ui.history.components.HistorySearchControlsState
import com.dbcheck.app.ui.history.state.HistorySearchFilter
import com.dbcheck.app.ui.meter.MeterModeChipRow
import com.dbcheck.app.ui.meter.components.CircularGauge
import com.dbcheck.app.ui.meter.components.DosimeterGaugeCard
import com.dbcheck.app.ui.meter.components.LiveSoundLevelChart
import com.dbcheck.app.ui.meter.components.MeterControls
import com.dbcheck.app.ui.meter.components.MeterControlsActions
import com.dbcheck.app.ui.meter.components.MeterControlsState
import com.dbcheck.app.ui.meter.components.MeterSessionInfoBar
import com.dbcheck.app.ui.meter.components.SoundReferenceCard
import com.dbcheck.app.ui.meter.components.WaveformVisualization
import com.dbcheck.app.ui.meter.state.DosimeterUiState
import com.dbcheck.app.ui.meter.state.LiveChartPointUiState
import com.dbcheck.app.ui.meter.state.MeasurementMode
import com.dbcheck.app.ui.meter.state.MeterSessionInfoUiState
import com.dbcheck.app.ui.settings.components.AudioCalibrationSection
import com.dbcheck.app.ui.settings.components.AudioCalibrationSectionActions
import com.dbcheck.app.ui.settings.components.AudioCalibrationSectionState
import com.dbcheck.app.ui.settings.components.NoiseNotificationsSection
import com.dbcheck.app.ui.settings.components.NoiseNotificationsSectionActions
import com.dbcheck.app.ui.settings.components.NoiseNotificationsSectionState
import com.dbcheck.app.ui.settings.state.CalibrationProfileUiState
import com.dbcheck.app.ui.settings.state.OctaveCalibrationBandUiState
import com.dbcheck.app.ui.settings.state.PassiveMonitoringDailySummaryUiState
import com.dbcheck.app.ui.sleep.SleepSetupAvailability
import com.dbcheck.app.ui.sleep.SleepSetupScreen
import com.dbcheck.app.ui.sleep.SleepSetupUiState
import com.dbcheck.app.ui.theme.DbCheckTheme
import java.time.DayOfWeek

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
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(DbCheckTheme.colorScheme.material.background)
                    .padding(16.dp),
        ) {
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
                state = MeterControlsState(isRecording = false, isShareEnabled = false),
                actions =
                    MeterControlsActions(
                        onToggleRecording = {},
                        onReset = {},
                        onShare = {},
                    ),
            )
            Spacer(modifier = Modifier.height(16.dp))
            MeterControls(
                state = MeterControlsState(isRecording = true, isShareEnabled = true),
                actions =
                    MeterControlsActions(
                        onToggleRecording = {},
                        onReset = {},
                        onShare = {},
                    ),
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, fontScale = 1.5f)
@Composable
fun MeterControlsLargeFontPreview() {
    DbCheckTheme {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MeterControls(
                state = MeterControlsState(isRecording = true, isShareEnabled = true, isCameraOverlayEnabled = true),
                actions =
                    MeterControlsActions(
                        onToggleRecording = {},
                        onReset = {},
                        onShare = {},
                        onCameraOverlayClick = {},
                    ),
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun CameraOverlayGrantedPreview() {
    DbCheckTheme {
        CameraOverlayScreen(
            permissionStatus = CameraPermissionStatus.Granted,
            onClose = {},
            onRequestPermission = {},
            onOpenSettings = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun CameraOverlayDeniedPreview() {
    DbCheckTheme {
        CameraOverlayScreen(
            permissionStatus = CameraPermissionStatus.Denied,
            onClose = {},
            onRequestPermission = {},
            onOpenSettings = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CameraOverlayPermanentlyDeniedDarkPreview() {
    DbCheckTheme {
        CameraOverlayScreen(
            permissionStatus = CameraPermissionStatus.PermanentlyDenied,
            onClose = {},
            onRequestPermission = {},
            onOpenSettings = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun CameraOverlayUnavailablePreview() {
    DbCheckTheme {
        CameraOverlayScreen(
            permissionStatus = CameraPermissionStatus.Granted,
            onClose = {},
            onRequestPermission = {},
            onOpenSettings = {},
            previewContent = { CameraPreviewUnavailableContent() },
            overlayContent = {},
        )
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
                dosimeterCardEnabled = false,
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
                dosimeterCardEnabled = true,
                onSelectMode = {},
                onLockedDosimeterClick = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun AudioCalibrationProfilesPreview() {
    DbCheckTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(DbCheckTheme.colorScheme.material.surface)
                    .padding(16.dp),
        ) {
            AudioCalibrationSection(
                state =
                    AudioCalibrationSectionState(
                        sensitivityOffset = 2.5f,
                        frequencyWeighting = WeightingType.A.name,
                        isProUser = true,
                        profiles =
                            listOf(
                                CalibrationProfileUiState(
                                    id = 1L,
                                    name = "Device default",
                                    micSensitivityOffset = 0f,
                                    isDefault = true,
                                    isSelected = false,
                                    canDelete = false,
                                ),
                                CalibrationProfileUiState(
                                    id = 2L,
                                    name = "Field mic",
                                    micSensitivityOffset = 2.5f,
                                    octaveBandOffsets = previewOctaveBandOffsets(),
                                    isDefault = false,
                                    isSelected = true,
                                    canDelete = true,
                                ),
                            ),
                        selectedProfileId = 2L,
                        profileErrorMessage = null,
                        audioInputDevices = emptyList(),
                        selectedAudioInputDeviceId = null,
                    ),
                actions =
                    AudioCalibrationSectionActions(
                        onSensitivityChange = {},
                        onWeightingChange = {},
                        onSelectAudioInputDevice = {},
                        onCreateProfile = {},
                        onSelectProfile = {},
                        onRenameProfile = { _, _ -> },
                        onDeleteProfile = {},
                        onOctaveBandOffsetChange = { _, _, _ -> },
                        onResetOctaveBandOffsets = {},
                        onUpgradeClick = {},
                    ),
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun NoiseNotificationSchedulePreview() {
    DbCheckTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(DbCheckTheme.colorScheme.material.surface)
                    .padding(16.dp),
        ) {
            NoiseNotificationsSection(
                state =
                    NoiseNotificationsSectionState(
                        exposureAlertsEnabled = true,
                        peakWarningsEnabled = true,
                        notificationThreshold = 85,
                        audibleAlarmEnabled = true,
                        ttsRiskPromptEnabled = true,
                        passiveMonitoringActive = false,
                        passiveMonitoringDailySummary =
                            PassiveMonitoringDailySummaryUiState(
                                hasSamples = true,
                                sampleCount = 2,
                                readingCount = 12,
                                averageDb = 74f,
                                peakDb = 91f,
                            ),
                        passiveMonitoringErrorMessage = null,
                        isProUser = true,
                        notificationSchedule =
                            NoiseNotificationSchedule(
                                activeDays =
                                    setOf(
                                        DayOfWeek.MONDAY,
                                        DayOfWeek.TUESDAY,
                                        DayOfWeek.WEDNESDAY,
                                        DayOfWeek.THURSDAY,
                                        DayOfWeek.FRIDAY,
                                    ),
                                startMinuteOfDay = 8 * 60,
                                endMinuteOfDay = 18 * 60,
                            ),
                    ),
                actions =
                    NoiseNotificationsSectionActions(
                        onExposureAlertsChange = {},
                        onPeakWarningsChange = {},
                        onThresholdChange = {},
                        onScheduleChange = {},
                        onAudibleAlarmChange = {},
                        onTtsRiskPromptChange = {},
                        onAudibleAlarmPreview = {},
                        onStartPassiveMonitoring = {},
                        onStopPassiveMonitoring = {},
                        onUpgradeClick = {},
                    ),
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun SleepSetupScreenPreview() {
    DbCheckTheme {
        SleepSetupScreen(
            uiState = SleepSetupUiState(availability = SleepSetupAvailability.Ready),
            onBack = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun AnalyticsSectionChipRowFreePreview() {
    DbCheckTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(DbCheckTheme.colorScheme.material.surface)
                    .padding(16.dp),
        ) {
            AnalyticsSectionChipRow(
                selectedSection = AnalyticsSection.SPECTRAL,
                isProUser = false,
                onSectionSelect = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AnalyticsSectionChipRowProDarkPreview() {
    DbCheckTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(DbCheckTheme.colorScheme.material.surface)
                    .padding(16.dp),
        ) {
            AnalyticsSectionChipRow(
                selectedSection = AnalyticsSection.ENVIRONMENT,
                isProUser = true,
                onSectionSelect = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun MeterSessionInfoBarFreePreview() {
    DbCheckTheme {
        MeterSessionInfoPreviewContainer {
            MeterSessionInfoBar(
                sessionInfo =
                    MeterSessionInfoUiState(
                        isRecording = true,
                        durationMs = 65_000L,
                        weighting = WeightingType.A,
                        responseTime = ResponseTime.FAST,
                        showProDetails = false,
                    ),
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MeterSessionInfoBarProDarkPreview() {
    DbCheckTheme {
        MeterSessionInfoPreviewContainer {
            MeterSessionInfoBar(
                sessionInfo =
                    MeterSessionInfoUiState(
                        isRecording = true,
                        durationMs = 3_723_000L,
                        weighting = WeightingType.C,
                        responseTime = ResponseTime.SLOW,
                        sampleRateHz = 44_100,
                        inputDeviceName = "USB-C microphone",
                        showProDetails = true,
                    ),
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
fun DosimeterGaugeLowPreview() {
    DbCheckTheme {
        DosimeterGaugePreviewContainer {
            DosimeterGaugeCard(
                dosimeter =
                    DosimeterUiState.Data(
                        standard = DosimeterStandard.NIOSH_REL,
                        laeqDb = 82.4f,
                        twaDb = 79.1f,
                        dosePercent = 24f,
                        projectedDosePercent = 48f,
                        remainingExposureMs = 21_600_000L,
                        durationMs = 3_600_000L,
                        sampleCount = 120,
                    ),
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun DosimeterGaugeNearLimitPreview() {
    DbCheckTheme {
        DosimeterGaugePreviewContainer {
            DosimeterGaugeCard(
                dosimeter =
                    DosimeterUiState.Data(
                        standard = DosimeterStandard.NIOSH_REL,
                        laeqDb = 86.7f,
                        twaDb = 84.4f,
                        dosePercent = 88f,
                        projectedDosePercent = 176f,
                        remainingExposureMs = 1_200_000L,
                        durationMs = 14_400_000L,
                        sampleCount = 420,
                    ),
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DosimeterGaugeOverLimitDarkPreview() {
    DbCheckTheme {
        DosimeterGaugePreviewContainer {
            DosimeterGaugeCard(
                dosimeter =
                    DosimeterUiState.Data(
                        standard = DosimeterStandard.OSHA_PEL,
                        laeqDb = 97.3f,
                        twaDb = 91.8f,
                        dosePercent = 132f,
                        projectedDosePercent = 264f,
                        remainingExposureMs = 0L,
                        durationMs = 14_400_000L,
                        sampleCount = 540,
                    ),
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
                        isSleepSession = true,
                    ),
                editAction = SessionCardEditAction(isLocked = true, onClick = {}),
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, fontScale = 1.5f)
@Composable
fun SessionCardLargeFontPreview() {
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
                        isSleepSession = true,
                    ),
                editAction = SessionCardEditAction(isLocked = false, onClick = {}),
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, fontScale = 1.5f)
@Composable
fun AmbientSoundPlaybackLargeFontPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            AmbientSoundPlaybackContent(
                state =
                    AmbientSoundPlaybackUiState(
                        preset = AmbientSoundPreset.BROWN_NOISE,
                        volume = 0.55f,
                        timerMinutes = 30,
                        isProUser = true,
                        title = "Ambient sound",
                        description = "Choose a locally generated ambient sound, volume, and optional stop timer.",
                    ),
                onPresetChange = {},
                onVolumeChange = {},
                onTimerChange = {},
                onPlay = {},
                onStop = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun DbHistogramCardPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DbHistogramCard(
                buckets = previewHistogramBuckets,
                isLocked = false,
                onUpgradeClick = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DbHistogramCardLockedDarkPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DbHistogramCard(
                buckets = previewHistogramBuckets,
                isLocked = true,
                onUpgradeClick = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun SleepResultsCardPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SleepResultsCard(
                state =
                    SleepResultsUiState(
                        targetDurationMinutes = 480,
                        recordedDurationMs = 7 * 60L * 60L * 1_000L + 42 * 60L * 1_000L,
                        equivalentLevelLabel = "LAeq",
                        equivalentLevelDb = 64.8f,
                        maxDb = 89.7f,
                        lcPeakDb = 111.3f,
                        peakEventCount = 2,
                        loudPeriodCount = 3,
                        sampleCount = 462,
                        histogramBuckets = previewHistogramBuckets,
                    ),
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun SleepInsightsCardPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SleepInsightsCard(
                state =
                    SleepInsightsUiState(
                        isAvailable = true,
                        notableEventCount = 3,
                        loudestPeriod =
                            SleepInsightPeriodUiState(
                                durationMs = 8 * 60L * 1_000L,
                                maxDb = 89.7f,
                            ),
                    ),
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
                state =
                    SpectralAnalysisCardState(
                        spectralState = SpectralAnalysisUiState.LockedPreview,
                        spectrogramState = SpectrogramUiState.LockedPreview,
                        rtaState = RtaUiState.LockedPreview,
                        selectedMode = SpectralMode.BARS,
                        isLocked = true,
                    ),
                actions = SpectralAnalysisCardActions(onUpgradeClick = {}),
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
                state =
                    SpectralAnalysisCardState(
                        spectralState = SpectralAnalysisUiState.Idle,
                        spectrogramState = SpectrogramUiState.Empty,
                        rtaState = RtaUiState.Empty,
                        selectedMode = SpectralMode.BARS,
                        isLocked = false,
                    ),
                actions = SpectralAnalysisCardActions(onUpgradeClick = {}),
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
                state =
                    SpectralAnalysisCardState(
                        spectralState =
                            SpectralAnalysisUiState.Live(
                                bands = previewSpectralBands(),
                                dominantFrequencyHz = 2400f,
                                bandwidth = SpectralBandwidth.WIDE,
                            ),
                        spectrogramState = previewSpectrogramState,
                        rtaState = previewRtaState,
                        selectedMode = SpectralMode.BARS,
                        isLocked = false,
                    ),
                actions = SpectralAnalysisCardActions(onUpgradeClick = {}),
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

private val previewHistogramBuckets =
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

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun HistorySearchControlsProPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            HistorySearchControls(
                state =
                    HistorySearchControlsState(
                        searchQuery = "Workshop",
                        selectedFilter = HistorySearchFilter.A_WEIGHTED,
                        isLocked = false,
                    ),
                actions =
                    HistorySearchControlsActions(
                        onSearchQueryChange = {},
                        onFilterSelect = {},
                        onClearSearch = {},
                        onUpgradeClick = {},
                    ),
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HistorySearchControlsLockedDarkPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            HistorySearchControls(
                state =
                    HistorySearchControlsState(
                        searchQuery = "",
                        selectedFilter = HistorySearchFilter.ALL,
                        isLocked = true,
                    ),
                actions =
                    HistorySearchControlsActions(
                        onSearchQueryChange = {},
                        onFilterSelect = {},
                        onClearSearch = {},
                        onUpgradeClick = {},
                    ),
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun SoundDetectionLockedPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SoundDetectionCard(
                soundDetectionState = SoundDetectionUiState.LockedPreview,
                isLocked = true,
                onUpgradeClick = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun SoundDetectionIdlePreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SoundDetectionCard(
                soundDetectionState = SoundDetectionUiState.Idle,
                isLocked = false,
                onUpgradeClick = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun SoundDetectionLivePreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SoundDetectionCard(
                soundDetectionState =
                    SoundDetectionUiState.Live(
                        label = "Speech",
                        confidencePercent = 82,
                        recentDetections =
                            listOf(
                                SoundDetectionChipUiState(label = "Speech", confidencePercent = 82),
                                SoundDetectionChipUiState(label = "Music", confidencePercent = 61),
                                SoundDetectionChipUiState(label = "Vehicle", confidencePercent = 47),
                            ),
                    ),
                isLocked = false,
                onUpgradeClick = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SoundDetectionErrorDarkPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SoundDetectionCard(
                soundDetectionState = SoundDetectionUiState.Error("Sound detection unavailable"),
                isLocked = false,
                onUpgradeClick = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun SpectralAnalysisSpectrogramPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SpectralAnalysisCard(
                state =
                    SpectralAnalysisCardState(
                        spectralState =
                            SpectralAnalysisUiState.Live(
                                bands = previewSpectralBands(),
                                dominantFrequencyHz = 2400f,
                                bandwidth = SpectralBandwidth.WIDE,
                            ),
                        spectrogramState = previewSpectrogramState,
                        rtaState = previewRtaState,
                        selectedMode = SpectralMode.SPECTROGRAM,
                        isLocked = false,
                    ),
                actions = SpectralAnalysisCardActions(onUpgradeClick = {}),
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun SpectralAnalysisRtaPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SpectralAnalysisCard(
                state =
                    SpectralAnalysisCardState(
                        spectralState =
                            SpectralAnalysisUiState.Live(
                                bands = previewSpectralBands(),
                                dominantFrequencyHz = 2400f,
                                bandwidth = SpectralBandwidth.WIDE,
                            ),
                        spectrogramState = previewSpectrogramState,
                        rtaState = previewRtaState,
                        selectedMode = SpectralMode.RTA,
                        isLocked = false,
                    ),
                actions = SpectralAnalysisCardActions(onUpgradeClick = {}),
            )
        }
    }
}

private val previewSpectrogramState
    get() =
        SpectrogramUiState.Data(
        rows =
            List(12) { rowIndex ->
                SpectrogramRowUiState(
                    timestampMs = rowIndex.toLong(),
                    bands =
                        List(previewSpectralCenterFrequenciesHz.size) { bandIndex ->
                            SpectralBandUiState(
                                normalizedAmplitude = ((rowIndex + bandIndex) % 8 + 1) / 8f,
                                centerFrequencyHz = previewSpectralCenterFrequenciesHz[bandIndex],
                            )
                        },
                )
            },
    )

private fun previewSpectralBands(): List<SpectralBandUiState> =
    previewSpectralAmplitudes.mapIndexed { index, amplitude ->
        SpectralBandUiState(
            normalizedAmplitude = amplitude,
            centerFrequencyHz = previewSpectralCenterFrequenciesHz[index],
        )
    }

private val previewSpectralCenterFrequenciesHz =
    listOf(
        24f,
        33f,
        46f,
        65f,
        91f,
        127f,
        178f,
        249f,
        349f,
        489f,
        685f,
        960f,
        1_345f,
        1_884f,
        2_400f,
        3_699f,
        5_184f,
        7_264f,
        10_177f,
        14_258f,
        19_975f,
        20_000f,
        20_000f,
        20_000f,
    )

private val previewSpectralAmplitudes =
    listOf(
        0.12f,
        0.16f,
        0.2f,
        0.24f,
        0.28f,
        0.34f,
        0.42f,
        0.5f,
        0.58f,
        0.66f,
        0.74f,
        0.82f,
        0.9f,
        0.94f,
        0.98f,
        0.82f,
        0.64f,
        0.48f,
        0.36f,
        0.28f,
        0.22f,
        0.16f,
        0.12f,
        0.08f,
    )

private val previewRtaState =
    RtaUiState.Data(
        bands =
            listOf(
                RtaBandUiState(centerFrequencyHz = 31.62f, normalizedAmplitude = 0.16f),
                RtaBandUiState(centerFrequencyHz = 63.10f, normalizedAmplitude = 0.24f),
                RtaBandUiState(centerFrequencyHz = 125.89f, normalizedAmplitude = 0.36f),
                RtaBandUiState(centerFrequencyHz = 251.19f, normalizedAmplitude = 0.58f),
                RtaBandUiState(centerFrequencyHz = 501.19f, normalizedAmplitude = 0.74f),
                RtaBandUiState(centerFrequencyHz = 1_000f, normalizedAmplitude = 1f),
                RtaBandUiState(centerFrequencyHz = 1_995.26f, normalizedAmplitude = 0.68f),
                RtaBandUiState(centerFrequencyHz = 3_981.07f, normalizedAmplitude = 0.52f),
                RtaBandUiState(centerFrequencyHz = 7_943.28f, normalizedAmplitude = 0.34f),
                RtaBandUiState(centerFrequencyHz = 15_848.93f, normalizedAmplitude = 0.22f),
            ),
    )

private fun previewOctaveBandOffsets(): List<OctaveCalibrationBandUiState> =
    OctaveCalibrationOffsets.supportedCenterFrequenciesHz.map { centerFrequencyHz ->
        OctaveCalibrationBandUiState(
            centerFrequencyHz = centerFrequencyHz,
            offsetDb =
                when {
                    centerFrequencyHz in 990f..1_010f -> 2f
                    centerFrequencyHz in 1_900f..2_100f -> -1.5f
                    centerFrequencyHz in 3_900f..4_100f -> 0.5f
                    else -> 0f
                },
        )
    }

@Composable
private fun DosimeterGaugePreviewContainer(content: @Composable () -> Unit) {
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
private fun MeterSessionInfoPreviewContainer(content: @Composable () -> Unit) {
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
