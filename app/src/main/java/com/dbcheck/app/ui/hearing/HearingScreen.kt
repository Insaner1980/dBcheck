package com.dbcheck.app.ui.hearing

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dbcheck.app.R
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.DbCheckTopAppBar
import com.dbcheck.app.ui.hearing.components.AmbientSoundCard
import com.dbcheck.app.ui.hearing.components.HearingHealthCard
import com.dbcheck.app.ui.hearing.components.HearingRecoveryCard
import com.dbcheck.app.ui.hearing.components.HearingRecoveryCardState
import com.dbcheck.app.ui.hearing.components.HearingTestCta
import com.dbcheck.app.ui.hearing.components.TinnitusPitchCard
import com.dbcheck.app.ui.hearing.components.VoiceBaselineCard
import com.dbcheck.app.ui.hearing.components.VoiceBaselineCardActions
import com.dbcheck.app.ui.hearing.components.VoiceBaselineCardState
import com.dbcheck.app.ui.sleep.components.SleepSetupCta
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun HearingScreen(
    actions: HearingScreenActions = HearingScreenActions(),
    viewModel: HearingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HearingScreenContent(
        state = state,
        actions = actions,
        onCalibrateVoiceBaseline = viewModel::calibrateVoiceBaseline,
    )
}

@Composable
internal fun HearingScreenContent(
    state: HearingUiState,
    actions: HearingScreenActions,
    onCalibrateVoiceBaseline: () -> Unit,
) {
    val spacing = DbCheckTheme.spacing

    Column(modifier = Modifier.fillMaxSize()) {
        DbCheckTopAppBar()
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.pageMargin),
            verticalArrangement = Arrangement.spacedBy(spacing.sectionGap),
        ) {
            Text(
                text = stringResource(R.string.hearing_hub_title),
                style = DbCheckTheme.typography.headlineLg,
                color = DbCheckTheme.colorScheme.material.onSurface,
            )
            HearingStatusSection(state)
            HearingSection(titleResId = R.string.hearing_hub_check_hearing_section) {
                HearingTestCta(
                    onStartTest = actions.onNavigateToHearingTest,
                    isLocked = !state.isProUser,
                    onUpgradeClick = actions.onNavigateToUpgrade,
                )
            }
            HearingSection(titleResId = R.string.hearing_hub_recovery_section) {
                HearingRecoveryCard(
                    state = state.hearingRecovery.toCardState(),
                    isLocked = !state.isProUser,
                    onStartBaseline = actions.onNavigateToHearingTest,
                    onStartRecoveryCheck = actions.onNavigateToHearingRecovery,
                    onUpgradeClick = actions.onNavigateToUpgrade,
                )
            }
            HearingSection(titleResId = R.string.hearing_hub_tinnitus_section) {
                TinnitusPitchCard(
                    profile = state.tinnitusPitchProfile,
                    isLocked = !state.isProUser,
                    onOpenPitchMatcher = actions.onNavigateToTinnitusPitch,
                    onUpgradeClick = actions.onNavigateToUpgrade,
                )
            }
            HearingSection(titleResId = R.string.hearing_hub_voice_baseline_section) {
                VoiceBaselineCard(
                    state =
                        VoiceBaselineCardState(
                            levelDb = state.voiceBaselineLevelDb,
                            sampleCount = state.voiceBaselineSampleCount,
                            canCalibrate = state.canCalibrateVoiceBaseline,
                            isLocked = !state.isProUser,
                        ),
                    actions =
                        VoiceBaselineCardActions(
                            onCalibrate = onCalibrateVoiceBaseline,
                            onUpgradeClick = actions.onNavigateToUpgrade,
                        ),
                )
            }
            HearingToolsSection(state = state, actions = actions)
            Spacer(Modifier.height(spacing.space4))
        }
    }
}

@Composable
private fun HearingStatusSection(state: HearingUiState) {
    HearingSection(titleResId = R.string.hearing_hub_status_section) {
        state.hearingHealthSummary?.let { summary ->
            HearingHealthCard(summary = summary)
        } ?: HearingMessageCard(messageResId = R.string.hearing_hub_status_no_data)
        LatestHearingTestCard(state.latestHearingTest)
    }
}

@Composable
private fun LatestHearingTestCard(state: HearingTestUiState) {
    val message =
        when (state) {
            HearingTestUiState.NoResult -> stringResource(R.string.hearing_hub_latest_test_no_result)

            is HearingTestUiState.Result ->
                stringResource(
                    R.string.hearing_hub_latest_test_result,
                    state.rating,
                    state.overallScore,
                    state.avgThreshold,
                )
        }

    DbCheckCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space2)) {
            Text(
                text = stringResource(R.string.hearing_hub_latest_test_title),
                style = DbCheckTheme.typography.labelMd,
                color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
            )
            Text(
                text = message,
                style = DbCheckTheme.typography.bodyMd,
                color = DbCheckTheme.colorScheme.material.onSurface,
            )
        }
    }
}

@Composable
private fun HearingToolsSection(state: HearingUiState, actions: HearingScreenActions) {
    HearingSection(titleResId = R.string.hearing_hub_tools_section) {
        if (state.sleepCardVisible) {
            SleepSetupCta(
                onOpenSleepSetup = actions.onNavigateToSleepMonitor,
                isLocked = !state.isProUser,
                onUpgradeClick = actions.onNavigateToUpgrade,
            )
        }
        AmbientSoundCard(
            isLocked = !state.isProUser,
            onOpenAmbientSound = actions.onNavigateToAmbientSounds,
            onUpgradeClick = actions.onNavigateToUpgrade,
        )
    }
}

@Composable
private fun HearingSection(@StringRes titleResId: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.groupGap)) {
        Text(
            text = stringResource(titleResId),
            style = DbCheckTheme.typography.labelMd,
            color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun HearingMessageCard(@StringRes messageResId: Int) {
    DbCheckCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(messageResId),
            style = DbCheckTheme.typography.bodyMd,
            color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
        )
    }
}

private fun HearingRecoveryUiState.toCardState(): HearingRecoveryCardState = when (this) {
    HearingRecoveryUiState.LockedPreview -> HearingRecoveryCardState.LockedPreview

    HearingRecoveryUiState.MissingBaseline -> HearingRecoveryCardState.MissingBaseline

    HearingRecoveryUiState.Ready -> HearingRecoveryCardState.Ready

    is HearingRecoveryUiState.Result ->
        HearingRecoveryCardState.Result(
            averageShiftDb = averageShiftDb,
            maxShiftDb = maxShiftDb,
            status = status,
        )
}
