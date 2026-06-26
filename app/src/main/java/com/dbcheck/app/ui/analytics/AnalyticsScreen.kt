package com.dbcheck.app.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dbcheck.app.R
import com.dbcheck.app.ui.analytics.components.AnalyticsOverviewRangeChipRow
import com.dbcheck.app.ui.analytics.components.AnalyticsSectionCard
import com.dbcheck.app.ui.analytics.components.AnalyticsSectionChipRow
import com.dbcheck.app.ui.analytics.components.EnvironmentMixCard
import com.dbcheck.app.ui.analytics.components.ExposureSummaryCard
import com.dbcheck.app.ui.analytics.components.HearingHealthCard
import com.dbcheck.app.ui.analytics.components.HearingTestCta
import com.dbcheck.app.ui.analytics.components.MonthlyTrendChart
import com.dbcheck.app.ui.analytics.components.SoundDetectionCard
import com.dbcheck.app.ui.analytics.components.SpectralAnalysisCard
import com.dbcheck.app.ui.analytics.components.SpectralAnalysisCardActions
import com.dbcheck.app.ui.analytics.components.SpectralAnalysisCardState
import com.dbcheck.app.ui.analytics.components.WeeklyExposureEmptyCard
import com.dbcheck.app.ui.analytics.components.YearlyReportCard
import com.dbcheck.app.ui.analytics.components.analyticsSectionCards
import com.dbcheck.app.ui.analytics.components.weeklyExposureSectionState
import com.dbcheck.app.ui.analytics.state.AnalyticsOverviewRange
import com.dbcheck.app.ui.analytics.state.AnalyticsSection
import com.dbcheck.app.ui.analytics.state.AnalyticsUiState
import com.dbcheck.app.ui.analytics.state.SpectralMode
import com.dbcheck.app.ui.components.DbCheckTopAppBar
import com.dbcheck.app.ui.components.EmptyState
import com.dbcheck.app.ui.components.SkeletonLoader
import com.dbcheck.app.ui.sleep.components.SleepSetupCta
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun AnalyticsScreen(
    onNavigateToMeter: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToHearingTest: () -> Unit = {},
    onNavigateToSleepSetup: () -> Unit = {},
    onNavigateToUpgrade: () -> Unit = {},
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        DbCheckTopAppBar(
            actionIcon = Icons.Outlined.Person,
            actionContentDescription = stringResource(R.string.a11y_open_settings),
            onActionClick = onNavigateToSettings,
        )

        when (val state = uiState) {
            is AnalyticsUiState.Loading -> LoadingContent()

            is AnalyticsUiState.Empty -> {
                EmptyState(
                    icon = Icons.Outlined.GraphicEq,
                    title = stringResource(R.string.analytics_empty_title),
                    description = stringResource(R.string.analytics_empty_description),
                    ctaText = stringResource(R.string.action_start_measuring),
                    onCtaClick = onNavigateToMeter,
                )
            }

            is AnalyticsUiState.Error -> {
                EmptyState(
                    icon = Icons.Outlined.GraphicEq,
                    title = state.message,
                    description = "",
                    ctaText = stringResource(R.string.action_start_measuring),
                    onCtaClick = onNavigateToMeter,
                )
            }

            is AnalyticsUiState.Success -> {
                AnalyticsContent(
                    state = state,
                    onOverviewRangeSelect = viewModel::onOverviewRangeSelected,
                    onSectionSelect = viewModel::onSectionSelected,
                    onSpectralModeSelect = viewModel::onSpectralModeSelected,
                    onNavigateToHearingTest = onNavigateToHearingTest,
                    onNavigateToSleepSetup = onNavigateToSleepSetup,
                    onNavigateToUpgrade = onNavigateToUpgrade,
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SkeletonLoader(height = 200.dp)
        SkeletonLoader(height = 120.dp)
    }
}

@Composable
private fun AnalyticsContent(
    state: AnalyticsUiState.Success,
    onOverviewRangeSelect: (AnalyticsOverviewRange) -> Unit,
    onSectionSelect: (AnalyticsSection) -> Unit,
    onSpectralModeSelect: (SpectralMode) -> Unit,
    onNavigateToHearingTest: () -> Unit,
    onNavigateToSleepSetup: () -> Unit,
    onNavigateToUpgrade: () -> Unit,
) {
    val spacing = DbCheckTheme.spacing
    val weeklyExposureState = weeklyExposureSectionState(state.hasExposureData)

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.space4),
    ) {
        AnalyticsHeaderControls(
            state = state,
            onOverviewRangeSelect = onOverviewRangeSelect,
            onSectionSelect = onSectionSelect,
        )

        Spacer(Modifier.height(spacing.space2))

        analyticsSectionCards(
            section = state.selectedSection,
            overviewRange = state.selectedOverviewRange,
            isRecording = state.isRecording,
            isProUser = state.isProUser,
            soundDetectionEnabled = state.soundDetectionEnabled,
            sleepCardEnabled = state.sleepCardEnabled,
        ).forEach { card ->
            AnalyticsSectionCardContent(
                card = card,
                state = state,
                weeklyExposureState = weeklyExposureState,
                onSpectralModeSelect = onSpectralModeSelect,
                onNavigateToHearingTest = onNavigateToHearingTest,
                onNavigateToSleepSetup = onNavigateToSleepSetup,
                onNavigateToUpgrade = onNavigateToUpgrade,
            )
        }

        Spacer(Modifier.height(spacing.space4))
    }
}

@Composable
private fun AnalyticsHeaderControls(
    state: AnalyticsUiState.Success,
    onOverviewRangeSelect: (AnalyticsOverviewRange) -> Unit,
    onSectionSelect: (AnalyticsSection) -> Unit,
) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    Column(verticalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space2)) {
        Text(
            text = stringResource(R.string.analytics_weekly_performance),
            style = typography.labelMd,
            color = colors.material.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.analytics_title),
            style = typography.headlineLg,
            color = colors.material.onSurface,
        )
        AnalyticsSectionChipRow(
            selectedSection = state.selectedSection,
            isProUser = state.isProUser,
            onSectionSelect = onSectionSelect,
        )
        if (state.selectedSection == AnalyticsSection.OVERVIEW) {
            AnalyticsOverviewRangeChipRow(
                selectedRange = state.selectedOverviewRange,
                isProUser = state.isProUser,
                onRangeSelect = onOverviewRangeSelect,
            )
        }
    }
}

@Composable
private fun AnalyticsSectionCardContent(
    card: AnalyticsSectionCard,
    state: AnalyticsUiState.Success,
    weeklyExposureState: com.dbcheck.app.ui.analytics.components.WeeklyExposureSectionState,
    onSpectralModeSelect: (SpectralMode) -> Unit,
    onNavigateToHearingTest: () -> Unit,
    onNavigateToSleepSetup: () -> Unit,
    onNavigateToUpgrade: () -> Unit,
) {
    when (card) {
        AnalyticsSectionCard.WEEKLY_EXPOSURE ->
            if (weeklyExposureState.showExposureMetrics) {
                ExposureSummaryCard(averageDb = state.weeklyAverageDb, dailyAverages = state.dailyAverages)
            } else {
                WeeklyExposureEmptyCard(state = weeklyExposureState)
            }

        AnalyticsSectionCard.HEARING_HEALTH ->
            if (weeklyExposureState.showExposureMetrics) {
                HearingHealthCard(healthStatus = state.healthStatus, todayVsWeekPercent = state.todayVsWeekPercent)
            }

        AnalyticsSectionCard.MONTHLY_TREND ->
            MonthlyTrendChart(
                monthlyTrendState = state.monthlyTrend,
                isLocked = !state.isProUser,
                onUpgradeClick = onNavigateToUpgrade,
            )

        AnalyticsSectionCard.YEARLY_REPORT ->
            YearlyReportCard(
                yearlyReportState = state.yearlyReport,
                isLocked = !state.isProUser,
                onUpgradeClick = onNavigateToUpgrade,
            )

        AnalyticsSectionCard.HEARING_TEST ->
            HearingTestCta(
                onStartTest = onNavigateToHearingTest,
                isLocked = !state.isProUser,
                onUpgradeClick = onNavigateToUpgrade,
            )

        AnalyticsSectionCard.SLEEP_SETUP ->
            SleepSetupCta(
                onOpenSleepSetup = onNavigateToSleepSetup,
                isLocked = !state.isProUser,
                onUpgradeClick = onNavigateToUpgrade,
            )

        AnalyticsSectionCard.SPECTRAL_ANALYSIS ->
            SpectralAnalysisCard(
                state =
                    SpectralAnalysisCardState(
                        spectralState = state.spectralAnalysis,
                        selectedMode = state.selectedSpectralMode,
                        isLocked = !state.isProUser,
                        spectrogramState = state.spectrogram,
                        rtaState = state.rta,
                    ),
                actions =
                    SpectralAnalysisCardActions(
                        onModeSelect = onSpectralModeSelect,
                        onUpgradeClick = onNavigateToUpgrade,
                    ),
            )

        AnalyticsSectionCard.SOUND_DETECTION ->
            SoundDetectionCard(
                soundDetectionState = state.soundDetection,
                isLocked = !state.isProUser,
                onUpgradeClick = onNavigateToUpgrade,
            )

        AnalyticsSectionCard.ACTIVE_ENVIRONMENT_MIX ->
            EnvironmentMixCard(
                environmentMixState = state.activeEnvironmentMix,
                isLocked = false,
                titleResId = R.string.environment_mix_active_title,
                onUpgradeClick = onNavigateToUpgrade,
            )

        AnalyticsSectionCard.ENVIRONMENT_MIX ->
            EnvironmentMixCard(
                environmentMixState = state.environmentMix,
                isLocked = !state.isProUser,
                titleResId = R.string.environment_mix_history_title,
                onUpgradeClick = onNavigateToUpgrade,
            )
    }
}
