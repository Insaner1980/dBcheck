package com.dbcheck.app.ui.analytics.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dbcheck.app.R
import com.dbcheck.app.domain.hearingtest.HearingRecoveryStatus
import com.dbcheck.app.ui.analytics.state.HearingRecoveryUiState
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.ProLockOverlay
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun HearingRecoveryCard(
    state: HearingRecoveryUiState,
    isLocked: Boolean,
    onStartBaseline: () -> Unit,
    onStartRecoveryCheck: () -> Unit,
    modifier: Modifier = Modifier,
    onUpgradeClick: () -> Unit = {},
) {
    ProLockOverlay(
        isLocked = isLocked,
        onUpgradeClick = onUpgradeClick,
        modifier = modifier,
    ) {
        val visibleState =
            if (isLocked) {
                HearingRecoveryUiState.LockedPreview
            } else {
                state
            }
        HearingRecoveryContent(
            state = visibleState,
            onStartBaseline = onStartBaseline,
            onStartRecoveryCheck = onStartRecoveryCheck,
        )
    }
}

@Composable
private fun HearingRecoveryContent(
    state: HearingRecoveryUiState,
    onStartBaseline: () -> Unit,
    onStartRecoveryCheck: () -> Unit,
) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val spacing = DbCheckTheme.spacing

    DbCheckCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            Text(
                text = stringResource(R.string.hearing_recovery_title),
                style = typography.labelMd,
                color = colors.material.onSurfaceVariant,
            )
            Text(
                text = descriptionFor(state),
                style = typography.bodyMd,
                color = colors.material.onSurfaceVariant,
            )
            RecoveryMetrics(state)
            RecoveryAction(
                state = state,
                onStartBaseline = onStartBaseline,
                onStartRecoveryCheck = onStartRecoveryCheck,
            )
        }
    }
}

@Composable
private fun RecoveryMetrics(state: HearingRecoveryUiState) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val metricState = metricStateFor(state)

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.hearing_recovery_average_shift),
                style = typography.labelSm,
                color = colors.material.onSurfaceVariant,
            )
            Text(metricState.averageShiftLabel, style = typography.dataLg, color = colors.material.onSurface)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.hearing_recovery_max_shift),
                style = typography.labelSm,
                color = colors.material.onSurfaceVariant,
            )
            Text(metricState.maxShiftLabel, style = typography.dataLg, color = colors.material.onSurface)
        }
    }
}

@Composable
private fun RecoveryAction(
    state: HearingRecoveryUiState,
    onStartBaseline: () -> Unit,
    onStartRecoveryCheck: () -> Unit,
) {
    when (state) {
        HearingRecoveryUiState.MissingBaseline ->
            DbCheckButton(
                text = stringResource(R.string.hearing_recovery_start_baseline),
                onClick = onStartBaseline,
                modifier = Modifier.fillMaxWidth(),
                height = 48.dp,
            )

        HearingRecoveryUiState.Ready,
        is HearingRecoveryUiState.Result,
        HearingRecoveryUiState.LockedPreview,
        ->
            DbCheckButton(
                text = stringResource(R.string.hearing_recovery_start_short_check),
                onClick = onStartRecoveryCheck,
                modifier = Modifier.fillMaxWidth(),
                style = DbCheckButtonStyle.Secondary,
                height = 48.dp,
            )
    }
}

@Composable
private fun descriptionFor(state: HearingRecoveryUiState): String = when (state) {
    HearingRecoveryUiState.LockedPreview -> stringResource(R.string.hearing_recovery_locked_preview)
    HearingRecoveryUiState.MissingBaseline -> stringResource(R.string.hearing_recovery_missing_baseline)
    HearingRecoveryUiState.Ready -> stringResource(R.string.hearing_recovery_description)
    is HearingRecoveryUiState.Result -> stringResource(statusCopyResId(state.status))
}

private fun statusCopyResId(status: HearingRecoveryStatus): Int = when (status) {
    HearingRecoveryStatus.STABLE -> R.string.hearing_recovery_result_stable
    HearingRecoveryStatus.SMALL_SHIFT -> R.string.hearing_recovery_result_small_shift
    HearingRecoveryStatus.ELEVATED_SHIFT -> R.string.hearing_recovery_result_elevated_shift
}

@Composable
private fun metricStateFor(state: HearingRecoveryUiState): RecoveryMetricState = when (state) {
    is HearingRecoveryUiState.Result ->
        RecoveryMetricState(
            averageShiftLabel = shiftLabel(state.averageShiftDb),
            maxShiftLabel = shiftLabel(state.maxShiftDb),
        )

    HearingRecoveryUiState.LockedPreview -> RecoveryMetricState(shiftLabel(6f), shiftLabel(12f))

    HearingRecoveryUiState.MissingBaseline,
    HearingRecoveryUiState.Ready,
    -> RecoveryMetricState("--", "--")
}

@Composable
private fun shiftLabel(shiftDb: Float): String = stringResource(R.string.hearing_recovery_shift_db, shiftDb)

private data class RecoveryMetricState(val averageShiftLabel: String, val maxShiftLabel: String)
