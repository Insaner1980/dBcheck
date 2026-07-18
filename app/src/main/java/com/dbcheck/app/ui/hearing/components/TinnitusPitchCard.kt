package com.dbcheck.app.ui.hearing.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dbcheck.app.R
import com.dbcheck.app.domain.tinnitus.TinnitusPitchProfile
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.ProLockOverlay
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun TinnitusPitchCard(
    profile: TinnitusPitchProfile,
    isLocked: Boolean,
    onOpenPitchMatcher: () -> Unit,
    modifier: Modifier = Modifier,
    onUpgradeClick: () -> Unit = {},
) {
    ProLockOverlay(
        isLocked = isLocked,
        onUpgradeClick = onUpgradeClick,
        modifier = modifier,
    ) {
        TinnitusPitchCardContent(
            profile =
                if (isLocked) {
                    TinnitusPitchProfile(leftFrequencyHz = 1_000f, rightFrequencyHz = 4_000f)
                } else {
                    profile
                },
            onOpenPitchMatcher = onOpenPitchMatcher,
        )
    }
}

@Composable
private fun TinnitusPitchCardContent(profile: TinnitusPitchProfile, onOpenPitchMatcher: () -> Unit) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val spacing = DbCheckTheme.spacing

    DbCheckCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space3), modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.tinnitus_pitch_card_title),
                style = typography.labelMd,
                color = colors.material.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.tinnitus_pitch_card_description),
                style = typography.bodyMd,
                color = colors.material.onSurfaceVariant,
            )
            Text(
                text = profileSummary(profile),
                style = typography.dataMd,
                color = colors.material.onSurface,
            )
            DbCheckButton(
                text = stringResource(R.string.tinnitus_pitch_open),
                onClick = onOpenPitchMatcher,
                modifier = Modifier.fillMaxWidth(),
                style = DbCheckButtonStyle.Secondary,
                height = 48.dp,
            )
        }
    }
}

@Composable
private fun profileSummary(profile: TinnitusPitchProfile): String = if (profile.hasSavedPitch) {
            stringResource(
                R.string.tinnitus_pitch_saved_summary,
            profile.leftFrequencyHz?.let { pitchCardFrequencyLabel(it) } ?: stringResource(R.string.value_unavailable),
            profile.rightFrequencyHz?.let { pitchCardFrequencyLabel(it) } ?: stringResource(R.string.value_unavailable),
        )
    } else {
        stringResource(R.string.tinnitus_pitch_no_saved_profile)
    }

@Composable
private fun pitchCardFrequencyLabel(frequencyHz: Float): String = if (frequencyHz >= 1_000f) {
        stringResource(R.string.tinnitus_pitch_frequency_khz, frequencyHz / 1_000f)
    } else {
        stringResource(R.string.tinnitus_pitch_frequency_hz, frequencyHz)
    }
