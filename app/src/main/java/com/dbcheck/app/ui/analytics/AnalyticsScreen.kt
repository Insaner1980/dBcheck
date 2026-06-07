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
import com.dbcheck.app.ui.analytics.components.EnvironmentMixCard
import com.dbcheck.app.ui.analytics.components.ExposureSummaryCard
import com.dbcheck.app.ui.analytics.components.HearingHealthCard
import com.dbcheck.app.ui.analytics.components.HearingTestCta
import com.dbcheck.app.ui.analytics.components.MonthlyTrendChart
import com.dbcheck.app.ui.analytics.components.SpectralAnalysisCard
import com.dbcheck.app.ui.analytics.components.WeeklyExposureEmptyCard
import com.dbcheck.app.ui.analytics.components.YearlyReportCard
import com.dbcheck.app.ui.analytics.components.weeklyExposureSectionState
import com.dbcheck.app.ui.analytics.state.AnalyticsUiState
import com.dbcheck.app.ui.components.DbCheckTopAppBar
import com.dbcheck.app.ui.components.EmptyState
import com.dbcheck.app.ui.components.SkeletonLoader
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun AnalyticsScreen(
    onNavigateToMeter: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToHearingTest: () -> Unit = {},
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
                    onNavigateToHearingTest = onNavigateToHearingTest,
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
    onNavigateToHearingTest: () -> Unit,
    onNavigateToUpgrade: () -> Unit,
) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
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

        Spacer(Modifier.height(spacing.space2))

        if (weeklyExposureState.showExposureMetrics) {
            ExposureSummaryCard(
                averageDb = state.weeklyAverageDb,
                dailyAverages = state.dailyAverages,
            )

            HearingHealthCard(
                healthStatus = state.healthStatus,
                todayVsWeekPercent = state.todayVsWeekPercent,
            )
        } else {
            WeeklyExposureEmptyCard(state = weeklyExposureState)
        }

        SpectralAnalysisCard(
            spectralState = state.spectralAnalysis,
            isLocked = !state.isProUser,
            onUpgradeClick = onNavigateToUpgrade,
        )
        EnvironmentMixCard(
            environmentMixState = state.environmentMix,
            isLocked = !state.isProUser,
            onUpgradeClick = onNavigateToUpgrade,
        )
        MonthlyTrendChart(
            monthlyTrendState = state.monthlyTrend,
            isLocked = !state.isProUser,
            onUpgradeClick = onNavigateToUpgrade,
        )
        YearlyReportCard(
            yearlyReportState = state.yearlyReport,
            isLocked = !state.isProUser,
            onUpgradeClick = onNavigateToUpgrade,
        )

        HearingTestCta(
            onStartTest = onNavigateToHearingTest,
            isLocked = !state.isProUser,
            onUpgradeClick = onNavigateToUpgrade,
        )

        Spacer(Modifier.height(spacing.space4))
    }
}
