package com.dbcheck.app.ui.analytics

import androidx.annotation.StringRes
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
import com.dbcheck.app.ui.hearing.components.HearingStatusRow
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun AnalyticsScreen(
    actions: AnalyticsScreenActions = AnalyticsScreenActions(),
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AnalyticsScreenContent(
        state = uiState,
        actions = actions,
        onOverviewRangeSelect = viewModel::onOverviewRangeSelected,
        onSectionSelect = viewModel::onSectionSelected,
        onSpectralModeSelect = viewModel::onSpectralModeSelected,
    )
}

@Composable
internal fun AnalyticsScreenContent(
    state: AnalyticsUiState,
    actions: AnalyticsScreenActions = AnalyticsScreenActions(),
    onOverviewRangeSelect: (AnalyticsOverviewRange) -> Unit = {},
    onSectionSelect: (AnalyticsSection) -> Unit = {},
    onSpectralModeSelect: (SpectralMode) -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxSize()) {
        DbCheckTopAppBar()

        when (state) {
            is AnalyticsUiState.Loading -> LoadingContent()

            is AnalyticsUiState.Empty -> {
                EmptyState(
                    icon = Icons.Outlined.GraphicEq,
                    title = stringResource(R.string.analytics_empty_title),
                    description = stringResource(R.string.analytics_empty_description),
                    ctaText = stringResource(R.string.action_start_measuring),
                    onCtaClick = actions.onNavigateToMeter,
                )
            }

            is AnalyticsUiState.Error -> {
                EmptyState(
                    icon = Icons.Outlined.GraphicEq,
                    title = state.message,
                    description = "",
                    ctaText = stringResource(R.string.action_start_measuring),
                    onCtaClick = actions.onNavigateToMeter,
                )
            }

            is AnalyticsUiState.Success -> {
                AnalyticsContent(
                    state = state,
                    onOverviewRangeSelect = onOverviewRangeSelect,
                    onSectionSelect = onSectionSelect,
                    onSpectralModeSelect = onSpectralModeSelect,
                    navigationActions = actions,
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.padding(DbCheckTheme.spacing.pageMargin),
        verticalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.groupGap),
    ) {
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
    navigationActions: AnalyticsScreenActions,
) {
    val spacing = DbCheckTheme.spacing
    val weeklyExposureState = weeklyExposureSectionState(state.hasExposureData)

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.pageMargin),
        verticalArrangement = Arrangement.spacedBy(spacing.sectionGap),
    ) {
        AnalyticsHeaderControls(
            state = state,
            onOverviewRangeSelect = onOverviewRangeSelect,
            onSectionSelect = onSectionSelect,
        )

        analyticsCardGroups(
            section = state.selectedSection,
            overviewRange = state.selectedOverviewRange,
            isRecording = state.isRecording,
            isProUser = state.isProUser,
            soundDetectionEnabled = state.soundDetectionEnabled,
        ).forEach { group ->
            AnalyticsCardGroupContent(
                group = group,
                state = state,
                weeklyExposureState = weeklyExposureState,
                onSpectralModeSelect = onSpectralModeSelect,
                navigationActions = navigationActions,
            )
        }

        Spacer(Modifier.height(spacing.space4))
    }
}

@Composable
private fun AnalyticsCardGroupContent(
    group: AnalyticsCardGroup,
    state: AnalyticsUiState.Success,
    weeklyExposureState: com.dbcheck.app.ui.analytics.components.WeeklyExposureSectionState,
    onSpectralModeSelect: (SpectralMode) -> Unit,
    navigationActions: AnalyticsScreenActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.groupGap)) {
        Text(
            text = stringResource(group.titleResId),
            style = DbCheckTheme.typography.labelMd,
            color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
        )
        group.cards.forEach { card ->
            AnalyticsSectionCardContent(
                card = card,
                state = state,
                weeklyExposureState = weeklyExposureState,
                onSpectralModeSelect = onSpectralModeSelect,
                navigationActions = navigationActions,
            )
        }
    }
}

private data class AnalyticsCardGroup(@param:StringRes val titleResId: Int, val cards: List<AnalyticsSectionCard>)

private fun analyticsCardGroups(
    section: AnalyticsSection,
    overviewRange: AnalyticsOverviewRange,
    isRecording: Boolean,
    isProUser: Boolean,
    soundDetectionEnabled: Boolean,
): List<AnalyticsCardGroup> {
    val cards =
        analyticsSectionCards(
            section = section,
            overviewRange = overviewRange,
            isRecording = isRecording,
            isProUser = isProUser,
            soundDetectionEnabled = soundDetectionEnabled,
        )
    return when (section) {
        AnalyticsSection.OVERVIEW ->
            overviewAnalyticsCardGroups(cards)

        AnalyticsSection.SPECTRAL ->
            listOf(AnalyticsCardGroup(R.string.analytics_group_spectral, cards))

        AnalyticsSection.ENVIRONMENT ->
            listOf(AnalyticsCardGroup(R.string.analytics_group_environment, cards))
    }
}

private fun overviewAnalyticsCardGroups(cards: List<AnalyticsSectionCard>): List<AnalyticsCardGroup> = listOf(
        AnalyticsCardGroup(
            titleResId = R.string.analytics_group_exposure,
            cards =
                cards.filter {
                    it == AnalyticsSectionCard.WEEKLY_EXPOSURE ||
                        it == AnalyticsSectionCard.MONTHLY_TREND
                },
        ),
        AnalyticsCardGroup(
            titleResId = R.string.analytics_group_hearing,
            cards =
                cards.filter {
                    it == AnalyticsSectionCard.HEARING_STATUS
                },
        ),
        AnalyticsCardGroup(
            titleResId = R.string.analytics_group_reports,
            cards = cards.filter { it == AnalyticsSectionCard.YEARLY_REPORT },
        ),
    ).filter { it.cards.isNotEmpty() }

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
    navigationActions: AnalyticsScreenActions,
) {
    when (card) {
        AnalyticsSectionCard.WEEKLY_EXPOSURE,
        AnalyticsSectionCard.HEARING_STATUS,
        AnalyticsSectionCard.MONTHLY_TREND,
        AnalyticsSectionCard.YEARLY_REPORT,
        -> OverviewSectionCardContent(
            card = card,
            state = state,
            weeklyExposureState = weeklyExposureState,
            navigationActions = navigationActions,
        )

        AnalyticsSectionCard.SPECTRAL_ANALYSIS ->
            SpectralSectionCardContent(
                state = state,
                onSpectralModeSelect = onSpectralModeSelect,
                onNavigateToUpgrade = navigationActions.onNavigateToUpgrade,
            )

        AnalyticsSectionCard.SOUND_DETECTION,
        AnalyticsSectionCard.ACTIVE_ENVIRONMENT_MIX,
        AnalyticsSectionCard.ENVIRONMENT_MIX,
        -> EnvironmentSectionCardContent(
            card = card,
            state = state,
            onNavigateToUpgrade = navigationActions.onNavigateToUpgrade,
        )
    }
}

@Composable
private fun OverviewSectionCardContent(
    card: AnalyticsSectionCard,
    state: AnalyticsUiState.Success,
    weeklyExposureState: com.dbcheck.app.ui.analytics.components.WeeklyExposureSectionState,
    navigationActions: AnalyticsScreenActions,
) {
    when (card) {
        AnalyticsSectionCard.WEEKLY_EXPOSURE ->
            if (weeklyExposureState.showExposureMetrics) {
                ExposureSummaryCard(averageDb = state.weeklyAverageDb, dailyAverages = state.dailyAverages)
            } else {
                WeeklyExposureEmptyCard(state = weeklyExposureState)
            }

        AnalyticsSectionCard.HEARING_STATUS ->
            HearingStatusRow(
                summary = state.hearingHealthSummary,
                onNavigateToHearing = navigationActions.onNavigateToHearing,
            )

        AnalyticsSectionCard.MONTHLY_TREND ->
            MonthlyTrendChart(
                monthlyTrendState = state.monthlyTrend,
                isLocked = !state.isProUser,
                onUpgradeClick = navigationActions.onNavigateToUpgrade,
            )

        AnalyticsSectionCard.YEARLY_REPORT ->
            YearlyReportCard(
                yearlyReportState = state.yearlyReport,
                isLocked = !state.isProUser,
                onUpgradeClick = navigationActions.onNavigateToUpgrade,
            )

        else -> Unit
    }
}

@Composable
private fun SpectralSectionCardContent(
    state: AnalyticsUiState.Success,
    onSpectralModeSelect: (SpectralMode) -> Unit,
    onNavigateToUpgrade: () -> Unit,
) {
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
}

@Composable
private fun EnvironmentSectionCardContent(
    card: AnalyticsSectionCard,
    state: AnalyticsUiState.Success,
    onNavigateToUpgrade: () -> Unit,
) {
    when (card) {
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

        else -> Unit
    }
}
