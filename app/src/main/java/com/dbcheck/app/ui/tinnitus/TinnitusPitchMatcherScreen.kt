package com.dbcheck.app.ui.tinnitus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dbcheck.app.R
import com.dbcheck.app.domain.hearingtest.Ear
import com.dbcheck.app.domain.tinnitus.TinnitusPitchPolicy
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.DbCheckChip
import com.dbcheck.app.ui.components.DbCheckSetupHeader
import com.dbcheck.app.ui.components.DbCheckSetupScaffold
import com.dbcheck.app.ui.components.DbCheckSlider
import com.dbcheck.app.ui.components.ProLockOverlay
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun TinnitusPitchMatcherScreen(
    onBack: () -> Unit,
    onNavigateToUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TinnitusPitchMatcherViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DbCheckSetupScaffold(
        onBack = onBack,
        modifier = modifier,
        header = {
            DbCheckSetupHeader(
                phase = stringResource(R.string.tinnitus_pitch_phase),
                title = state.title,
                description = state.description,
            )
        },
        cta = {
            ProLockOverlay(
                isLocked = state.isLocked,
                onUpgradeClick = onNavigateToUpgrade,
            ) {
                TinnitusPitchActions(
                    onPreview = viewModel::playPreview,
                    onSave = viewModel::saveProfile,
                )
            }
        },
    ) {
        ProLockOverlay(
            isLocked = state.isLocked,
            onUpgradeClick = onNavigateToUpgrade,
        ) {
            TinnitusPitchMatcherContent(
                state = state,
                onEarSelect = viewModel::selectEar,
                onFrequencyChange = viewModel::updateFrequency,
            )
        }
    }
}

@Composable
private fun TinnitusPitchMatcherContent(
    state: TinnitusPitchMatcherUiState,
    onEarSelect: (Ear) -> Unit,
    onFrequencyChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val spacing = DbCheckTheme.spacing

    Column(modifier = modifier) {
        EarSelector(selectedEar = state.selectedEar, onEarSelect = onEarSelect)

        Spacer(Modifier.height(spacing.space4))

        DbCheckCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                Text(
                    text = stringResource(R.string.tinnitus_pitch_current_frequency),
                    style = typography.labelMd,
                    color = colors.material.onSurfaceVariant,
                )
                DbCheckSlider(
                    value = state.currentFrequencyHz,
                    onValueChange = onFrequencyChange,
                    valueRange = TinnitusPitchPolicy.MIN_FREQUENCY_HZ..TinnitusPitchPolicy.MAX_FREQUENCY_HZ,
                    steps = TINNITUS_PITCH_SLIDER_STEPS,
                    valueLabel = frequencyLabel(state.currentFrequencyHz),
                )
                SavedPitchSummary(state)
                state.errorMessage?.let {
                    Text(text = it, style = typography.labelSm, color = colors.material.error)
                }
                state.saveMessage?.let {
                    Text(text = it, style = typography.labelSm, color = colors.material.primary)
                }
                Text(
                    text = state.disclaimer,
                    style = typography.labelSm,
                    color = colors.material.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TinnitusPitchActions(onPreview: () -> Unit, onSave: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space3),
        modifier = Modifier.fillMaxWidth(),
    ) {
        DbCheckButton(
            text = stringResource(R.string.tinnitus_pitch_preview),
            onClick = onPreview,
            modifier = Modifier.weight(1f),
            style = DbCheckButtonStyle.Secondary,
            height = DbCheckTheme.spacing.space12,
        )
        DbCheckButton(
            text = stringResource(R.string.tinnitus_pitch_save),
            onClick = onSave,
            modifier = Modifier.weight(1f),
            height = DbCheckTheme.spacing.space12,
        )
    }
}

@Composable
private fun EarSelector(selectedEar: Ear, onEarSelect: (Ear) -> Unit) {
    val spacing = DbCheckTheme.spacing
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.space2), modifier = Modifier.fillMaxWidth()) {
        Ear.entries.forEach { ear ->
            DbCheckChip(
                onClick = { onEarSelect(ear) },
                text =
                    when (ear) {
                        Ear.LEFT -> stringResource(R.string.tinnitus_pitch_left_ear)
                        Ear.RIGHT -> stringResource(R.string.tinnitus_pitch_right_ear)
                    },
                selected = selectedEar == ear,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Headphones,
                        contentDescription = null,
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SavedPitchSummary(state: TinnitusPitchMatcherUiState) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val summary =
        if (state.hasSavedProfile) {
            stringResource(
                R.string.tinnitus_pitch_saved_summary,
                state.leftFrequencyHz?.let { frequencyLabel(it) } ?: stringResource(R.string.value_unavailable),
                state.rightFrequencyHz?.let { frequencyLabel(it) } ?: stringResource(R.string.value_unavailable),
            )
        } else {
            stringResource(R.string.tinnitus_pitch_no_saved_profile)
        }
    Text(text = summary, style = typography.labelSm, color = colors.material.onSurfaceVariant)
}

@Composable
private fun frequencyLabel(frequencyHz: Float): String = if (frequencyHz >= 1_000f) {
        stringResource(R.string.tinnitus_pitch_frequency_khz, frequencyHz / 1_000f)
    } else {
        stringResource(R.string.tinnitus_pitch_frequency_hz, frequencyHz)
    }

private val TINNITUS_PITCH_SLIDER_STEPS =
    (
        (
            (TinnitusPitchPolicy.MAX_FREQUENCY_HZ - TinnitusPitchPolicy.MIN_FREQUENCY_HZ) /
        TinnitusPitchPolicy.FREQUENCY_STEP_HZ
        ).toInt() - 1
    ).coerceAtLeast(0)
