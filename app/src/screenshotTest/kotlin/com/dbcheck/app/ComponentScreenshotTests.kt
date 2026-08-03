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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.dbcheck.app.data.local.preferences.model.WaveformStyle
import com.dbcheck.app.domain.ambient.AmbientSoundPreset
import com.dbcheck.app.domain.audio.AudioInputDeviceType
import com.dbcheck.app.domain.audio.ResponseTime
import com.dbcheck.app.domain.audio.SpectralBandwidth
import com.dbcheck.app.domain.audio.WeightingType
import com.dbcheck.app.domain.calibration.OctaveCalibrationOffsets
import com.dbcheck.app.domain.hearingtest.Ear
import com.dbcheck.app.domain.hearingtest.HearingTestMode
import com.dbcheck.app.domain.noise.DosimeterStandard
import com.dbcheck.app.domain.noise.NoiseLevel
import com.dbcheck.app.domain.noise.NoiseNotificationSchedule
import com.dbcheck.app.domain.noise.SoundReferenceCatalog
import com.dbcheck.app.ui.ambient.AmbientSoundPlaybackCallbacks
import com.dbcheck.app.ui.ambient.AmbientSoundPlaybackScreen
import com.dbcheck.app.ui.ambient.AmbientSoundPlaybackUiState
import com.dbcheck.app.ui.analytics.components.AnalyticsSectionChipRow
import com.dbcheck.app.ui.analytics.components.MonthlyTrendChart
import com.dbcheck.app.ui.analytics.components.SoundDetectionCard
import com.dbcheck.app.ui.analytics.components.SpectralAnalysisCard
import com.dbcheck.app.ui.analytics.components.SpectralAnalysisCardActions
import com.dbcheck.app.ui.analytics.components.SpectralAnalysisCardState
import com.dbcheck.app.ui.analytics.components.YearlyReportCard
import com.dbcheck.app.ui.analytics.components.spectralPreviewBands
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
import com.dbcheck.app.ui.camera.CameraOverlayBottomBar
import com.dbcheck.app.ui.camera.CameraOverlayCaptureControlsActions
import com.dbcheck.app.ui.camera.CameraOverlayCaptureControlsState
import com.dbcheck.app.ui.camera.CameraOverlayReadoutStatus
import com.dbcheck.app.ui.camera.CameraOverlayScreen
import com.dbcheck.app.ui.camera.CameraOverlayUiState
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
import com.dbcheck.app.ui.history.detail.lockedPreviewHistogramBuckets
import com.dbcheck.app.ui.history.components.HistorySearchControls
import com.dbcheck.app.ui.history.components.HistorySearchControlsActions
import com.dbcheck.app.ui.history.components.HistorySearchControlsState
import com.dbcheck.app.ui.history.components.Last24HoursChart
import com.dbcheck.app.ui.history.state.HistorySearchFilter
import com.dbcheck.app.ui.history.state.HourlyExposureUiState
import com.dbcheck.app.ui.hearingtest.active.ActiveTestState
import com.dbcheck.app.ui.hearingtest.active.HearingTestActiveContent
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
import com.dbcheck.app.ui.settings.state.AudioInputDeviceUiState
import com.dbcheck.app.ui.settings.state.CalibrationProfileUiState
import com.dbcheck.app.ui.settings.state.OctaveCalibrationBandUiState
import com.dbcheck.app.ui.settings.state.PassiveMonitoringDailySummaryUiState
import com.dbcheck.app.ui.sleep.SleepSetupAvailability
import com.dbcheck.app.ui.sleep.SleepSetupActions
import com.dbcheck.app.ui.sleep.SleepSetupScreen
import com.dbcheck.app.ui.sleep.SleepSetupUiState
import com.dbcheck.app.ui.theme.DbCheckTheme
import com.dbcheck.app.ui.theme.DbCheckRadii
import java.time.DayOfWeek

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun ButtonStylesPreview() {
    DbCheckTheme {
        ButtonStylesPreviewContent(modifier = Modifier.padding(16.dp))
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ButtonStylesDarkPreview() {
    DbCheckTheme {
        ButtonStylesPreviewContent(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(DbCheckTheme.colorScheme.material.background)
                    .padding(16.dp),
        )
    }
}

@Composable
private fun ButtonStylesPreviewContent(modifier: Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "Enabled",
            style = DbCheckTheme.typography.labelMd,
            color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        DbCheckButton(text = "Primary", onClick = {}, style = DbCheckButtonStyle.Primary)
        Spacer(modifier = Modifier.height(8.dp))
        DbCheckButton(text = "Secondary", onClick = {}, style = DbCheckButtonStyle.Secondary)
        Spacer(modifier = Modifier.height(8.dp))
        DbCheckButton(text = "Tertiary", onClick = {}, style = DbCheckButtonStyle.Tertiary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Disabled",
            style = DbCheckTheme.typography.labelMd,
            color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        DbCheckButton(text = "Primary", onClick = {}, style = DbCheckButtonStyle.Primary, enabled = false)
        Spacer(modifier = Modifier.height(8.dp))
        DbCheckButton(text = "Secondary", onClick = {}, style = DbCheckButtonStyle.Secondary, enabled = false)
        Spacer(modifier = Modifier.height(8.dp))
        DbCheckButton(text = "Tertiary", onClick = {}, style = DbCheckButtonStyle.Tertiary, enabled = false)
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 300)
@Composable
fun CardPreview() {
    DbCheckTheme {
        CardPreviewContent()
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 300, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CardDarkPreview() {
    DbCheckTheme {
        CardPreviewContent()
    }
}

@Composable
private fun CardPreviewContent() {
    DbCheckCard(modifier = Modifier.width(280.dp)) {
        Text(
            text = "42.5 dB",
            style = DbCheckTheme.typography.displayLg,
            color = DbCheckTheme.colorScheme.material.onSurface,
        )
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
                isRecording = true,
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
                        responseTime = ResponseTime.FAST,
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
                        audioInputDevices =
                            listOf(
                                AudioInputDeviceUiState(
                                    id = 11,
                                    displayName = "Built-in microphone",
                                    type = AudioInputDeviceType.BUILT_IN_MIC,
                                    isExternal = false,
                                ),
                                AudioInputDeviceUiState(
                                    id = 42,
                                    displayName = "Studio USB",
                                    type = AudioInputDeviceType.USB,
                                    isExternal = true,
                                ),
                                AudioInputDeviceUiState(
                                    id = 7,
                                    displayName = "studio usb",
                                    type = AudioInputDeviceType.USB,
                                    isExternal = true,
                                ),
                            ),
                        selectedAudioInputDeviceId = null,
                    ),
                actions =
                    AudioCalibrationSectionActions(
                        onSensitivityChange = {},
                        onWeightingChange = {},
                        onResponseTimeChange = {},
                        onSelectAudioInputDevice = {},
                        onCreateProfile = {},
                        onSelectProfile = {},
                        onRenameProfile = { _, _ -> },
                        onDeleteProfile = {},
                        onOpenOctaveCalibration = {},
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
            actions = SleepSetupActions(onBack = {}),
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
        ComponentPreviewContainer {
            LiveSoundLevelChart(
                points = emptyList(),
                isRecording = false,
                animationsEnabled = false,
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun LiveSoundLevelChartActivePreview() {
    DbCheckTheme {
        ComponentPreviewContainer {
            LiveSoundLevelChart(
                points = previewLiveChartData,
                isRecording = true,
                animationsEnabled = false,
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun LiveSoundLevelChartActiveDarkPreview() {
    DbCheckTheme {
        ComponentPreviewContainer {
            LiveSoundLevelChart(
                points = previewLiveChartData,
                isRecording = true,
                animationsEnabled = false,
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun LiveSoundLevelChartPausedDarkPreview() {
    DbCheckTheme {
        ComponentPreviewContainer {
            LiveSoundLevelChart(
                points = previewLiveChartData.take(8),
                isRecording = false,
                animationsEnabled = false,
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
        ComponentPreviewContainer {
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
        ComponentPreviewContainer {
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
        SessionCardPreviewContent(isLocked = true)
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, fontScale = 1.5f)
@Composable
fun SessionCardLargeFontPreview() {
    DbCheckTheme {
        SessionCardPreviewContent(isLocked = false)
    }
}

@Composable
private fun SessionCardPreviewContent(isLocked: Boolean) {
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
            editAction = SessionCardEditAction(isLocked = isLocked, onClick = {}),
        )
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun Last24HoursChartEmptyPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            Last24HoursChart(
                hourlyAverages = emptyList(),
                avgDb = 0f,
                maxDb = 0f,
                trend = "Stable",
                windowStartMs = 0L,
                windowEndMs = 24L * 60L * 60L * 1_000L,
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun Last24HoursChartDataPreview() {
    val windowEndMs = 24L * 60L * 60L * 1_000L
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            Last24HoursChart(
                hourlyAverages =
                    listOf(
                        HourlyExposureUiState(hour = 6, avgDb = 57f, maxDb = 63f, hourStartMs = 6L * 60L * 60L * 1_000L),
                        HourlyExposureUiState(hour = 12, avgDb = 68f, maxDb = 74f, hourStartMs = 12L * 60L * 60L * 1_000L),
                        HourlyExposureUiState(hour = 20, avgDb = 62f, maxDb = 71f, hourStartMs = 20L * 60L * 60L * 1_000L),
                    ),
                avgDb = 62f,
                maxDb = 74f,
                trend = "Stable",
                windowStartMs = 0L,
                windowEndMs = windowEndMs,
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, fontScale = 1.5f)
@Composable
fun AmbientSoundPlaybackLargeFontPreview() {
    DbCheckTheme {
        AmbientSoundPlaybackScreen(
            state =
                AmbientSoundPlaybackUiState(
                    preset = AmbientSoundPreset.BROWN_NOISE,
                    volume = 0.55f,
                    timerMinutes = 30,
                    isProUser = true,
                    title = "Ambient sound",
                    description = "Choose a locally generated ambient sound, volume, and optional stop timer.",
            ),
            onBack = {},
            callbacks =
                AmbientSoundPlaybackCallbacks(
                    onNavigateToUpgrade = {},
                    onPresetChange = {},
                    onVolumeChange = {},
                    onTimerChange = {},
                    onPlay = {},
                    onStop = {},
                    onOpenNotificationSettings = {},
                ),
        )
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun DbHistogramCardPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DbHistogramCard(
                buckets = lockedPreviewHistogramBuckets,
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
                buckets = lockedPreviewHistogramBuckets,
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
                        histogramBuckets = lockedPreviewHistogramBuckets,
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
        SpectralAnalysisPreviewContent(selectedMode = SpectralMode.BARS)
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SpectralAnalysisLiveDarkPreview() {
    DbCheckTheme {
        SpectralAnalysisPreviewContent(selectedMode = SpectralMode.BARS)
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
private fun ComponentPreviewContainer(content: @Composable () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DbCheckRadii.Row))
                .background(DbCheckTheme.colorScheme.material.surface)
                .padding(DbCheckTheme.spacing.space4),
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
        SpectralAnalysisPreviewContent(selectedMode = SpectralMode.SPECTROGRAM)
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SpectralAnalysisSpectrogramDarkPreview() {
    DbCheckTheme {
        SpectralAnalysisPreviewContent(selectedMode = SpectralMode.SPECTROGRAM)
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun CameraOverlayIdleControlsPreview() {
    CameraOverlayControlsPreview(
        state =
            CameraOverlayUiState(
                currentDb = 54.4f,
                status = CameraOverlayReadoutStatus.READY,
                timestampMs = 1_700_000_000_000L,
            ),
        controlsState =
            CameraOverlayCaptureControlsState(
                photoEnabled = true,
                videoEnabled = true,
                isRecordingVideo = false,
                captureFailed = false,
                videoCaptureFailed = false,
            ),
    )
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CameraOverlayRecordingControlsDarkPreview() {
    CameraOverlayControlsPreview(
        state =
            CameraOverlayUiState(
                currentDb = 78.7f,
                status = CameraOverlayReadoutStatus.LIVE,
                timestampMs = 1_700_000_000_000L,
                isRecordingVideo = true,
            ),
        controlsState =
            CameraOverlayCaptureControlsState(
                photoEnabled = false,
                videoEnabled = true,
                isRecordingVideo = true,
                captureFailed = false,
                videoCaptureFailed = false,
            ),
    )
}

@PreviewTest
@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 740,
    fontScale = 1.3f,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
fun CameraOverlayDisabledErrorControlsLargeFontPreview() {
    CameraOverlayControlsPreview(
        state =
            CameraOverlayUiState(
                currentDb = null,
                status = CameraOverlayReadoutStatus.READY,
                videoCaptureFailed = true,
            ),
        controlsState =
            CameraOverlayCaptureControlsState(
                photoEnabled = false,
                videoEnabled = false,
                isRecordingVideo = false,
                captureFailed = false,
                videoCaptureFailed = true,
            ),
    )
}

@Composable
private fun CameraOverlayControlsPreview(
    state: CameraOverlayUiState,
    controlsState: CameraOverlayCaptureControlsState,
) {
    DbCheckTheme {
        CameraOverlayScreen(
            permissionStatus = CameraPermissionStatus.Granted,
            onClose = {},
            onRequestPermission = {},
            onOpenSettings = {},
            overlayContent = {
                CameraOverlayBottomBar(
                    state = state,
                    controlsState = controlsState,
                    actions = CameraOverlayCaptureControlsActions(),
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(
                                horizontal = DbCheckTheme.spacing.space3,
                                vertical = DbCheckTheme.spacing.space6,
                            ),
                )
            },
        )
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun HearingActiveToneOffPreview() {
    HearingActivePreview(state = ActiveTestState(currentPhase = 1, isPlayingTone = false))
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HearingActiveToneOnDarkPreview() {
    HearingActivePreview(state = ActiveTestState(currentPhase = 1, isPlayingTone = true))
}

@PreviewTest
@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 740,
    fontScale = 1.3f,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
fun HearingActiveToneOnLargeFontPreview() {
    HearingActivePreview(state = ActiveTestState(currentPhase = 1, isPlayingTone = true))
}

@Composable
private fun HearingActivePreview(state: ActiveTestState) {
    DbCheckTheme {
        HearingTestActiveContent(
            state = state.copy(currentEar = Ear.LEFT, totalPhases = 12),
            mode = HearingTestMode.FULL,
            onBack = {},
            onRetrySave = {},
            onHearTone = {},
            onMissTone = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun SpectralAnalysisRtaPreview() {
    DbCheckTheme {
        SpectralAnalysisPreviewContent(selectedMode = SpectralMode.RTA)
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SpectralAnalysisRtaDarkPreview() {
    DbCheckTheme {
        SpectralAnalysisPreviewContent(selectedMode = SpectralMode.RTA)
    }
}

@Composable
private fun SpectralAnalysisPreviewContent(selectedMode: SpectralMode) {
    Column(modifier = Modifier.padding(16.dp)) {
        SpectralAnalysisCard(
            state =
                SpectralAnalysisCardState(
                    spectralState =
                        SpectralAnalysisUiState.Live(
                            bands = spectralPreviewBands,
                            dominantFrequencyHz = 2400f,
                            bandwidth = SpectralBandwidth.WIDE,
                        ),
                    spectrogramState = previewSpectrogramState,
                    rtaState = previewRtaState,
                    selectedMode = selectedMode,
                    isLocked = false,
                ),
            actions = SpectralAnalysisCardActions(onUpgradeClick = {}),
        )
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
                        List(spectralPreviewBands.size) { bandIndex ->
                            SpectralBandUiState(
                                normalizedAmplitude = ((rowIndex + bandIndex) % 8 + 1) / 8f,
                                centerFrequencyHz = spectralPreviewBands[bandIndex].centerFrequencyHz,
                            )
                        },
                )
            },
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
