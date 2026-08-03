package com.dbcheck.app.ui.hearingtest.active

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dbcheck.app.R
import com.dbcheck.app.domain.hearingtest.HearingTestMode
import com.dbcheck.app.ui.common.UiNumberFormatter
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckTopAppBar
import com.dbcheck.app.ui.components.DbCheckTopAppBarModel
import com.dbcheck.app.ui.components.shouldUseCompactHeightScrolling
import com.dbcheck.app.ui.theme.DbCheckMotion
import com.dbcheck.app.ui.theme.DbCheckTheme
import com.dbcheck.app.util.labelStringRes
import com.dbcheck.app.util.lowercaseNameStringRes

@Composable
fun HearingTestActiveScreen(
    onTestComplete: (Long) -> Unit,
    onBack: () -> Unit,
    mode: HearingTestMode = HearingTestMode.FULL,
    viewModel: ActiveTestViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnTestComplete by rememberUpdatedState(onTestComplete)

    LaunchedEffect(mode) {
        viewModel.startTest(mode)
    }

    LaunchedEffect(state.completedTestId) {
        state.completedTestId?.let(currentOnTestComplete)
    }

    HearingTestActiveContent(
        state = state,
        mode = mode,
        onBack = onBack,
        onRetrySave = viewModel::retrySaveResult,
        onHearTone = viewModel::onHeard,
        onMissTone = viewModel::onNotHeard,
    )
}

@Composable
internal fun HearingTestActiveContent(
    state: ActiveTestState,
    mode: HearingTestMode,
    onBack: () -> Unit,
    onRetrySave: () -> Unit,
    onHearTone: () -> Unit,
    onMissTone: () -> Unit,
) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val spacing = DbCheckTheme.spacing
    val routeTitle =
        stringResource(
            if (mode == HearingTestMode.RECOVERY) {
                R.string.hearing_recovery_setup_title
            } else {
                R.string.hearing_test_title
            },
        )

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.material.background),
    ) {
        DbCheckTopAppBar(
            model = DbCheckTopAppBarModel.Pushed(title = routeTitle, onBackClick = onBack),
        )
        BoxWithConstraints(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
        ) {
            val useScrollableContent = shouldUseCompactHeightScrolling(maxHeight.value)
            val scrollState = rememberScrollState()
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .then(
                            if (useScrollableContent) {
                                Modifier.verticalScroll(scrollState)
                            } else {
                                Modifier
                            },
                        )
                        .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HearingTestProgressSection(state)
                HearingFrequencySection(state)
                Spacer(Modifier.height(spacing.space8))
                HearingTestInstruction(state)

                if (useScrollableContent) {
                    Spacer(Modifier.height(spacing.space8))
                } else {
                    Spacer(Modifier.weight(1f))
                }

                HearingTestErrorAndRetry(state = state, onRetrySave = onRetrySave)
                HearingTestResponseButtons(
                    state = state,
                    onHearTone = onHearTone,
                    onMissTone = onMissTone,
                )
                Spacer(Modifier.height(spacing.space8))
            }
        }
    }
}

@Composable
private fun HearingTestProgressSection(state: ActiveTestState) {
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing
    val typography = DbCheckTheme.typography

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(spacing.space10))
        Text(
            text = stringResource(R.string.hearing_active_phase, state.currentPhase, state.totalPhases),
            style = typography.labelMd,
            color = colors.material.onSurfaceVariant,
        )
        Spacer(Modifier.height(spacing.space3))
        LinearProgressIndicator(
            progress = { state.currentPhase.toFloat() / state.totalPhases },
            modifier = Modifier.fillMaxWidth(),
            color = colors.material.onSurface,
            trackColor = colors.material.surfaceContainerHigh,
            drawStopIndicator = {},
        )
        Spacer(Modifier.height(spacing.space4))
        Text(
            text = stringResource(state.currentEar.labelStringRes()),
            style = typography.labelLg,
            color = colors.material.onSurfaceVariant,
        )
        Spacer(Modifier.height(spacing.space16))
    }
}

@Composable
private fun HearingFrequencySection(state: ActiveTestState) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    Box(
        modifier = Modifier.size(224.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (state.isPlayingTone) {
            HearingTonePulseRing()
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier =
                    Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(colors.material.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.hearing_active_testing),
                        style = typography.labelMd,
                        color = colors.material.onSurfaceVariant,
                    )
                    Text(
                        text = "${UiNumberFormatter.integer(state.currentFrequency)} Hz",
                        style = typography.displayMd,
                        color = colors.material.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun HearingTestInstruction(state: ActiveTestState) {
    Text(
        text =
            stringResource(
                R.string.hearing_active_instruction,
                stringResource(state.currentEar.lowercaseNameStringRes()),
            ),
        style = DbCheckTheme.typography.bodyLg,
        color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 20.dp),
    )
}

@Composable
private fun HearingTestErrorAndRetry(state: ActiveTestState, onRetrySave: () -> Unit) {
    val spacing = DbCheckTheme.spacing

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        state.errorMessage?.let { error ->
            Text(
                text = error,
                style = DbCheckTheme.typography.bodyMd,
                color = DbCheckTheme.colorScheme.material.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(spacing.space3))
        }
        if (state.canRetrySave) {
            DbCheckButton(
                text = stringResource(R.string.action_try_again),
                onClick = onRetrySave,
                modifier = Modifier.fillMaxWidth(),
                height = 56.dp,
            )
            Spacer(Modifier.height(spacing.space3))
        }
    }
}

@Composable
private fun HearingTestResponseButtons(state: ActiveTestState, onHearTone: () -> Unit, onMissTone: () -> Unit) {
    val controlsEnabled = !state.isSavingResult && !state.isLocked && !state.isComplete

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space3),
    ) {
        DbCheckButton(
            text = stringResource(R.string.action_i_hear_it),
            onClick = onHearTone,
            enabled = controlsEnabled,
            modifier = Modifier.fillMaxWidth(),
            height = 56.dp,
        )
        DbCheckButton(
            text = stringResource(R.string.action_i_do_not_hear_it),
            onClick = onMissTone,
            enabled = controlsEnabled,
            modifier = Modifier.fillMaxWidth(),
            style = DbCheckButtonStyle.Secondary,
            height = 56.dp,
        )
    }
}

@Composable
private fun HearingTonePulseRing() {
    val colors = DbCheckTheme.colorScheme
    val pulseTransition = rememberInfiniteTransition(label = "hearing-tone-pulse")
    val pulse by
        pulseTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = DbCheckMotion.Breathing, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "hearing-tone-pulse-progress",
        )

    Box(
        modifier =
            Modifier
                .size(216.dp)
                .scale(
                    DbCheckMotion.HearingTonePulseBaseScale +
                        (pulse * DbCheckMotion.HearingTonePulseScaleRange),
                )
                .border(
                    width = DbCheckTheme.spacing.hairline,
                    color =
                        colors.material.onSurfaceVariant.copy(
                            alpha =
                                DbCheckMotion.HearingTonePulseBaseAlpha +
                                    (pulse * DbCheckMotion.HearingTonePulseAlphaRange),
                        ),
                    shape = CircleShape,
                ),
    )
}
