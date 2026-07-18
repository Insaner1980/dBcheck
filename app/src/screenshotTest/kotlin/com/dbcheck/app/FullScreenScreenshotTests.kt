package com.dbcheck.app

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.data.local.preferences.model.WaveformStyle
import com.dbcheck.app.domain.audio.ResponseTime
import com.dbcheck.app.domain.audio.SpectralBandwidth
import com.dbcheck.app.domain.hearing.HearingHealthStatus
import com.dbcheck.app.domain.hearing.HearingHealthSummary
import com.dbcheck.app.domain.hearingtest.HearingRecoveryStatus
import com.dbcheck.app.domain.noise.DosimeterStandard
import com.dbcheck.app.domain.noise.NoiseLevel
import com.dbcheck.app.domain.noise.NoiseNotificationSchedule
import com.dbcheck.app.domain.session.Session
import com.dbcheck.app.domain.tinnitus.TinnitusPitchProfile
import com.dbcheck.app.ui.analytics.AnalyticsScreenContent
import com.dbcheck.app.ui.analytics.state.AnalyticsOverviewRange
import com.dbcheck.app.ui.analytics.state.AnalyticsSection
import com.dbcheck.app.ui.analytics.state.AnalyticsUiState
import com.dbcheck.app.ui.analytics.state.DailyExposureUiState
import com.dbcheck.app.ui.analytics.state.EnvironmentMixCategory
import com.dbcheck.app.ui.analytics.state.EnvironmentMixRowUiState
import com.dbcheck.app.ui.analytics.state.EnvironmentMixUiState
import com.dbcheck.app.ui.analytics.state.MonthlyTrendPointUiState
import com.dbcheck.app.ui.analytics.state.MonthlyTrendUiState
import com.dbcheck.app.ui.analytics.state.SoundDetectionChipUiState
import com.dbcheck.app.ui.analytics.state.SoundDetectionUiState
import com.dbcheck.app.ui.analytics.state.SpectralAnalysisUiState
import com.dbcheck.app.ui.analytics.state.SpectralBandUiState
import com.dbcheck.app.ui.analytics.state.YearlyReportUiState
import com.dbcheck.app.ui.components.BottomNavBar
import com.dbcheck.app.ui.components.BottomNavItem
import com.dbcheck.app.ui.hearing.HearingRecoveryUiState
import com.dbcheck.app.ui.hearing.HearingScreenContent
import com.dbcheck.app.ui.hearing.HearingTestUiState
import com.dbcheck.app.ui.hearing.HearingUiState
import com.dbcheck.app.ui.history.HistoryScreenContent
import com.dbcheck.app.ui.history.HistorySuccessActions
import com.dbcheck.app.ui.history.state.HistoryUiState
import com.dbcheck.app.ui.history.state.HourlyExposureUiState
import com.dbcheck.app.ui.meter.MeterScreenActions
import com.dbcheck.app.ui.meter.MeterScreenContent
import com.dbcheck.app.ui.meter.state.DosimeterUiState
import com.dbcheck.app.ui.meter.state.LiveChartPointUiState
import com.dbcheck.app.ui.meter.state.MeasurementMode
import com.dbcheck.app.ui.meter.state.MeterSessionInfoUiState
import com.dbcheck.app.ui.meter.state.MeterUiState
import com.dbcheck.app.ui.navigation.BottomNavDestination
import com.dbcheck.app.ui.navigation.Screen
import com.dbcheck.app.ui.settings.SettingsCalibrationContent
import com.dbcheck.app.ui.settings.SettingsDataPrivacyContent
import com.dbcheck.app.ui.settings.SettingsDisplayContent
import com.dbcheck.app.ui.settings.SettingsHomePage
import com.dbcheck.app.ui.settings.SettingsNotificationsContent
import com.dbcheck.app.ui.settings.SettingsOctaveCalibrationContent
import com.dbcheck.app.ui.settings.SettingsProAboutContent
import com.dbcheck.app.ui.settings.components.AudioCalibrationSectionActions
import com.dbcheck.app.ui.settings.components.DataExportSectionActions
import com.dbcheck.app.ui.settings.components.DisplayAndFeaturesSectionActions
import com.dbcheck.app.ui.settings.components.HealthSyncSectionActions
import com.dbcheck.app.ui.settings.components.LockscreenMeterSectionActions
import com.dbcheck.app.ui.settings.components.NoiseNotificationsSectionActions
import com.dbcheck.app.ui.settings.components.ProUpsellCardActions
import com.dbcheck.app.ui.settings.octaveCalibrationPresentation
import com.dbcheck.app.ui.settings.state.CalibrationProfileUiState
import com.dbcheck.app.ui.settings.state.HealthConnectAvailabilityUi
import com.dbcheck.app.ui.settings.state.HealthConnectUiState
import com.dbcheck.app.ui.settings.state.LocalBackupUiState
import com.dbcheck.app.ui.settings.state.OctaveCalibrationBandUiState
import com.dbcheck.app.ui.settings.state.PassiveMonitoringDailySummaryUiState
import com.dbcheck.app.ui.settings.state.SettingsUiState
import com.dbcheck.app.ui.theme.DbCheckTheme
import java.time.DayOfWeek

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun MeterIdleLightPreview() = MeterFullScreenPreview(meterIdleState())

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MeterIdleDarkPreview() = MeterFullScreenPreview(meterIdleState())

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun MeterRecordingLightPreview() = MeterFullScreenPreview(meterRecordingState())

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MeterRecordingDarkPreview() = MeterFullScreenPreview(meterRecordingState())

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun MeterDosimeterLightPreview() = MeterFullScreenPreview(meterDosimeterState())

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MeterDosimeterDarkPreview() = MeterFullScreenPreview(meterDosimeterState())

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun TrendsOverviewLightPreview() = TrendsFullScreenPreview(analyticsOverviewState())

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TrendsOverviewDarkPreview() = TrendsFullScreenPreview(analyticsOverviewState())

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun TrendsSpectralLightPreview() = TrendsFullScreenPreview(analyticsSpectralState())

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TrendsSpectralDarkPreview() = TrendsFullScreenPreview(analyticsSpectralState())

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun TrendsEnvironmentLightPreview() = TrendsFullScreenPreview(analyticsEnvironmentState())

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TrendsEnvironmentDarkPreview() = TrendsFullScreenPreview(analyticsEnvironmentState())

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun HearingFreeLightPreview() = HearingFullScreenPreview(hearingFreeState())

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HearingFreeDarkPreview() = HearingFullScreenPreview(hearingFreeState())

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun HearingProLightPreview() = HearingFullScreenPreview(hearingProState())

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HearingProDarkPreview() = HearingFullScreenPreview(hearingProState())

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun HistoryEmptyLightPreview() = HistoryFullScreenPreview(HistoryUiState.Empty)

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HistoryEmptyDarkPreview() = HistoryFullScreenPreview(HistoryUiState.Empty)

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun HistorySessionsLightPreview() = HistoryFullScreenPreview(historySessionsState())

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HistorySessionsDarkPreview() = HistoryFullScreenPreview(historySessionsState())

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun SettingsHubLightPreview() = SettingsHubFullScreenPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SettingsHubDarkPreview() = SettingsHubFullScreenPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun SettingsCalibrationLightPreview() = SettingsCalibrationFullScreenPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SettingsCalibrationDarkPreview() = SettingsCalibrationFullScreenPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun SettingsOctaveCalibrationLightPreview() = SettingsOctaveFullScreenPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SettingsOctaveCalibrationDarkPreview() = SettingsOctaveFullScreenPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun SettingsNotificationsLightPreview() = SettingsNotificationsFullScreenPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SettingsNotificationsDarkPreview() = SettingsNotificationsFullScreenPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun SettingsDataPrivacyLightPreview() = SettingsDataPrivacyFullScreenPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SettingsDataPrivacyDarkPreview() = SettingsDataPrivacyFullScreenPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun SettingsDisplayLightPreview() = SettingsDisplayFullScreenPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SettingsDisplayDarkPreview() = SettingsDisplayFullScreenPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun SettingsProAboutLightPreview() = SettingsProAboutFullScreenPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SettingsProAboutDarkPreview() = SettingsProAboutFullScreenPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, fontScale = 1.5f, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun MeterIdleLargeFontPreview() = MeterFullScreenPreview(meterIdleState())

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, fontScale = 1.5f, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun HearingProLargeFontPreview() = HearingFullScreenPreview(hearingProState())

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, fontScale = 1.5f, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun HistorySessionsLargeFontPreview() = HistoryFullScreenPreview(historySessionsState())

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, fontScale = 1.5f, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun SettingsNotificationsLargeFontPreview() = SettingsNotificationsFullScreenPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, fontScale = 1.5f, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun SettingsDataPrivacyLargeFontPreview() = SettingsDataPrivacyFullScreenPreview()

@Composable
private fun MeterFullScreenPreview(state: MeterUiState) {
    FullScreenAppShell(Screen.Meter.route) {
        MeterScreenContent(state, previewMeterActions)
    }
}

@Composable
private fun TrendsFullScreenPreview(state: AnalyticsUiState.Success) {
    FullScreenAppShell(Screen.Analytics.route) {
        AnalyticsScreenContent(state)
    }
}

@Composable
private fun HearingFullScreenPreview(state: HearingUiState) {
    FullScreenAppShell(Screen.Hearing.route) {
        HearingScreenContent(state = state, actions = com.dbcheck.app.ui.hearing.HearingScreenActions()) {}
    }
}

@Composable
private fun HistoryFullScreenPreview(state: HistoryUiState) {
    FullScreenAppShell(Screen.History.route) {
        HistoryScreenContent(state = state, successActions = HistorySuccessActions())
    }
}

@Composable
private fun SettingsHubFullScreenPreview() {
    FullScreenAppShell(Screen.Settings.route) {
        SettingsHomePage(onNavigate = {})
    }
}

@Composable
private fun SettingsCalibrationFullScreenPreview() {
    FullScreenAppShell(Screen.Settings.route) {
        SettingsCalibrationContent(
            uiState = previewSettingsState(),
            onBack = {},
            actions = previewCalibrationActions,
        )
    }
}

@Composable
private fun SettingsOctaveFullScreenPreview() {
    val state = previewSettingsState()
    FullScreenAppShell(Screen.Settings.route) {
        SettingsOctaveCalibrationContent(
            uiState = state,
            presentation = octaveCalibrationPresentation(state),
            onBack = {},
            onUpgradeClick = {},
            onOffsetChange = { _, _, _ -> },
            onReset = {},
        )
    }
}

@Composable
private fun SettingsNotificationsFullScreenPreview() {
    FullScreenAppShell(Screen.Settings.route) {
        SettingsNotificationsContent(
            uiState = previewSettingsState().copy(passiveMonitoringActive = false),
            passiveMonitoringPermissionDenied = true,
            onBack = {},
            actions = previewNotificationActions,
        )
    }
}

@Composable
private fun SettingsDataPrivacyFullScreenPreview() {
    FullScreenAppShell(Screen.Settings.route) {
        SettingsDataPrivacyContent(
            uiState = previewSettingsState(),
            coarseLocationPermissionGranted = false,
            coarseLocationPermissionDenied = true,
            onBack = {},
            healthSyncActions = previewHealthSyncActions,
            dataExportActions = previewDataExportActions,
            lockscreenActions = previewLockscreenActions,
        )
    }
}

@Composable
private fun SettingsDisplayFullScreenPreview() {
    FullScreenAppShell(Screen.Settings.route) {
        SettingsDisplayContent(
            uiState = previewSettingsState(),
            onBack = {},
            actions = previewDisplayActions,
        )
    }
}

@Composable
private fun SettingsProAboutFullScreenPreview() {
    FullScreenAppShell(Screen.Settings.route) {
        SettingsProAboutContent(
            uiState = previewSettingsState().copy(isProUser = false),
            onBack = {},
            actions = ProUpsellCardActions(onUpgradeClick = {}),
        )
    }
}

@Composable
private fun FullScreenAppShell(currentRoute: String, content: @Composable () -> Unit) {
    DbCheckTheme {
        val items =
            BottomNavDestination.entries.map { destination ->
                BottomNavItem(
                    label = stringResource(destination.labelRes),
                    selectedIcon = destination.selectedIcon,
                    unselectedIcon = destination.unselectedIcon,
                    route = destination.screen.route,
                )
            }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = DbCheckTheme.colorScheme.material.background,
            bottomBar = { BottomNavBar(items = items, currentRoute = currentRoute, onItemClick = {}) },
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) { content() }
        }
    }
}

private fun meterIdleState() =
    MeterUiState(
        currentDb = 36.4f,
        minDb = 34.8f,
        avgDb = 36.1f,
        maxDb = 38.2f,
        peakDb = 41.6f,
        noiseLevel = NoiseLevel.QUIET,
        isMicPermissionGranted = true,
        isProUser = true,
        dosimeterCardEnabled = true,
    )

private fun meterRecordingState() =
    meterIdleState().copy(
        currentDb = 74.6f,
        minDb = 51.2f,
        avgDb = 68.7f,
        maxDb = 82.3f,
        peakDb = 94.1f,
        sampleCount = 1_240,
        noiseLevel = NoiseLevel.ELEVATED,
        isRecording = true,
        sessionDurationMs = 754_000L,
        equivalentLevelDb = 69.2f,
        liveChartPoints = previewLiveChartPoints(),
        waveformData = List(48) { index -> ((index % 9) - 4) / 5f },
        sessionInfo =
            MeterSessionInfoUiState(
                isRecording = true,
                durationMs = 754_000L,
                inputDeviceName = "Built-in microphone",
                showProDetails = true,
            ),
    )

private fun meterDosimeterState() =
    meterRecordingState().copy(
        currentDb = 86.2f,
        noiseLevel = NoiseLevel.DANGEROUS,
        measurementMode = MeasurementMode.DOSIMETER,
        dosimeter =
            DosimeterUiState.Data(
                standard = DosimeterStandard.NIOSH_REL,
                laeqDb = 84.6f,
                twaDb = 82.1f,
                dosePercent = 63.4f,
                projectedDosePercent = 118.7f,
                remainingExposureMs = 4_380_000L,
                durationMs = 7_620_000L,
                sampleCount = 8_400,
            ),
    )

private fun previewLiveChartPoints(): List<LiveChartPointUiState> =
    List(24) { index ->
        LiveChartPointUiState(timestampMs = PREVIEW_NOW_MS - (23 - index) * 1_000L, db = 61f + (index % 7) * 2.4f)
    }

private fun analyticsOverviewState() =
    AnalyticsUiState.Success(
        weeklyAverageDb = 68.4f,
        dailyAverages =
            List(7) { index ->
                DailyExposureUiState(
                    dayStartMs = PREVIEW_NOW_MS - (6 - index) * DAY_MS,
                    avgDb = 61f + index * 1.8f,
                    maxDb = 78f + index * 1.5f,
                    isToday = index == 6,
                )
            },
        hearingHealthSummary = HearingHealthSummary(68.4f, HearingHealthStatus.SAFE, -6),
        isProUser = true,
        selectedOverviewRange = AnalyticsOverviewRange.MONTHLY,
        monthlyTrend =
            MonthlyTrendUiState.Data(
                points =
                    List(12) { index ->
                        MonthlyTrendPointUiState(
                            dayStartMs = PREVIEW_NOW_MS - (11 - index) * DAY_MS,
                            laeqDb = 62f + index,
                            maxDb = 78f + index * 0.8f,
                        )
                    },
                laeqDb = 67.8f,
                loudestDb = 88.2f,
            ),
        yearlyReport =
            YearlyReportUiState.Data(
                totalSessions = 148,
                laeqDb = 66.7f,
                loudestDayLabel = "Jul 12",
                loudestDb = 94.3f,
                zoneRows = previewEnvironmentRows(),
            ),
    )

private fun analyticsSpectralState() =
    analyticsOverviewState().copy(
        selectedSection = AnalyticsSection.SPECTRAL,
        isRecording = true,
        spectralAnalysis =
            SpectralAnalysisUiState.Live(
                bands = List(24) { index -> SpectralBandUiState((index % 8 + 2) / 10f, 25f * (index + 1)) },
                dominantFrequencyHz = 1_250f,
                bandwidth = SpectralBandwidth.WIDE,
            ),
    )

private fun analyticsEnvironmentState() =
    analyticsOverviewState().copy(
        selectedSection = AnalyticsSection.ENVIRONMENT,
        isRecording = true,
        soundDetectionEnabled = true,
        soundDetection =
            SoundDetectionUiState.Live(
                label = "Conversation",
                confidencePercent = 91,
                recentDetections =
                    listOf(
                        SoundDetectionChipUiState("Speech", 91),
                        SoundDetectionChipUiState("Traffic", 76),
                    ),
            ),
        activeEnvironmentMix = EnvironmentMixUiState.Data(previewEnvironmentRows()),
        environmentMix = EnvironmentMixUiState.Data(previewEnvironmentRows()),
    )

private fun previewEnvironmentRows() =
    listOf(
        EnvironmentMixRowUiState(EnvironmentMixCategory.QUIET, 28),
        EnvironmentMixRowUiState(EnvironmentMixCategory.MODERATE, 44),
        EnvironmentMixRowUiState(EnvironmentMixCategory.LOUD, 22),
        EnvironmentMixRowUiState(EnvironmentMixCategory.CRITICAL, 6),
    )

private fun hearingFreeState() =
    HearingUiState(
        isProUser = false,
        latestHearingTest = HearingTestUiState.NoResult,
        hearingRecovery = HearingRecoveryUiState.LockedPreview,
    )

private fun hearingProState() =
    HearingUiState(
        isProUser = true,
        hearingHealthSummary = HearingHealthSummary(67.3f, HearingHealthStatus.SAFE, -4),
        latestHearingTest =
            HearingTestUiState.Result(
                id = 7L,
                timestamp = PREVIEW_NOW_MS - DAY_MS,
                overallScore = 92,
                rating = "Excellent",
                avgThreshold = 12.5f,
            ),
        hearingRecovery =
            HearingRecoveryUiState.Result(
                averageShiftDb = 2.5f,
                maxShiftDb = 5f,
                status = HearingRecoveryStatus.STABLE,
                timestamp = PREVIEW_NOW_MS,
            ),
        tinnitusPitchProfile = TinnitusPitchProfile(6_300f, 6_700f, PREVIEW_NOW_MS),
        sleepCardVisible = true,
        voiceBaselineLevelDb = 63.5f,
        voiceBaselineSampleCount = 18,
        voiceBaselineCapturedAtMs = PREVIEW_NOW_MS,
        isRecording = true,
        soundDetectionEnabled = true,
    )

private fun historySessionsState() =
    HistoryUiState.Success(
        last24HoursData =
            List(12) { index ->
                HourlyExposureUiState(index + 8, 58f + index * 1.4f, 72f + index * 1.7f, PREVIEW_NOW_MS - index * 3_600_000L)
            },
        last24HoursAvg = 66.8f,
        last24HoursMax = 91.4f,
        last24HoursTrend = "-4%",
        last24HoursWindowStartMs = PREVIEW_NOW_MS - DAY_MS,
        last24HoursWindowEndMs = PREVIEW_NOW_MS,
        recentSessions = previewSessions(),
        sleepSessionIds = setOf(2L),
        weeklyTrendPercent = -8,
        weeklyTrendLabel = "Lower than last week",
        safeHours = 19.5f,
        isProUser = true,
    )

private fun previewSessions() =
    listOf(
        previewSession(1L, "🎧", "Evening commute through Helsinki city centre", 68.4f, 92.1f, listOf("Commute", "Traffic")),
        previewSession(2L, "🌙", "Quiet bedroom sleep monitoring", 41.8f, 67.3f, listOf("Sleep", "Home")),
        previewSession(3L, "🎸", "Rehearsal room sound check", 84.6f, 101.2f, listOf("Music", "Work")),
    )

private fun previewSession(id: Long, emoji: String, name: String, avgDb: Float, peakDb: Float, tags: List<String>) =
    Session(
        id = id,
        startTime = PREVIEW_NOW_MS - id * 3_600_000L,
        endTime = PREVIEW_NOW_MS - id * 3_600_000L + 2_400_000L,
        minDb = avgDb - 12f,
        avgDb = avgDb,
        maxDb = peakDb - 3f,
        peakDb = peakDb,
        name = name,
        emoji = emoji,
        tags = tags,
        isActive = false,
        frequencyWeighting = "A",
    )

private fun previewSettingsState() =
    SettingsUiState(
        themeMode = "system",
        exposureAlertsEnabled = true,
        peakWarningsEnabled = true,
        notificationThreshold = 86,
        notificationSchedule =
            NoiseNotificationSchedule(
                activeDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                startMinuteOfDay = 8 * 60,
                endMinuteOfDay = 18 * 60,
            ),
        micSensitivityOffset = 2.5f,
        frequencyWeighting = "A",
        selectedCalibrationProfileId = 2L,
        calibrationProfiles = previewCalibrationProfiles(),
        responseTime = ResponseTime.FAST,
        waveformStyle = WaveformStyle.BARS,
        refreshRate = MeterRefreshRate.HIGH,
        lockscreenMeterEnabled = true,
        showLockscreenMeterPublicly = false,
        healthConnectEnabled = true,
        heartRateOverlayEnabled = true,
        technicalMetadataEnabled = true,
        dosimeterCardEnabled = true,
        soundDetectionEnabled = false,
        sleepCardEnabled = true,
        wavRecordingDefaultEnabled = false,
        audibleAlarmEnabled = true,
        ttsRiskPromptEnabled = false,
        passiveMonitoringDailySummary =
            PassiveMonitoringDailySummaryUiState(true, 4, 96, 67.2f, 91.5f),
        healthConnectStatus =
            HealthConnectUiState(
                availability = HealthConnectAvailabilityUi.AVAILABLE,
                noiseSyncGranted = true,
                heartRateReadGranted = false,
            ),
        localBackups =
            listOf(
                LocalBackupUiState(
                    filePath = "/preview/backup.db",
                    fileName = "dBcheck_backup_2026-07-18.db",
                    displayName = "Jul 18, 2026 · 14:30",
                    createdAtMillis = PREVIEW_NOW_MS,
                    sizeBytes = 2_480_000L,
                ),
            ),
        isProUser = true,
    )

private fun previewCalibrationProfiles() =
    listOf(
        CalibrationProfileUiState(1L, "Device default", 0f, isDefault = true, isSelected = false, canDelete = false),
        CalibrationProfileUiState(
            id = 2L,
            name = "Field microphone",
            micSensitivityOffset = 2.5f,
            octaveBandOffsets =
                listOf(31.5f, 63f, 125f, 250f, 500f, 1_000f, 2_000f, 4_000f, 8_000f, 16_000f).mapIndexed {
                        index,
                        frequency,
                    -> OctaveCalibrationBandUiState(frequency, (index - 4) * 0.4f)
                },
            isDefault = false,
            isSelected = true,
            canDelete = true,
        ),
    )

private val previewMeterActions =
    MeterScreenActions({}, {}, {}, {}, {}, {}, {}, {})

private val previewCalibrationActions =
    AudioCalibrationSectionActions({}, {}, {}, {}, {}, {}, { _, _ -> }, {}, {}, {})

private val previewNotificationActions =
    NoiseNotificationsSectionActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})

private val previewHealthSyncActions =
    HealthSyncSectionActions({}, {}, {}, {}, {}, {})

private val previewDataExportActions =
    DataExportSectionActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})

private val previewLockscreenActions = LockscreenMeterSectionActions({}, {}, {})

private val previewDisplayActions =
    DisplayAndFeaturesSectionActions({}, {}, {}, {}, {}, {}, {}, {})

private const val DAY_MS = 86_400_000L
private const val PREVIEW_NOW_MS = 1_721_300_400_000L
