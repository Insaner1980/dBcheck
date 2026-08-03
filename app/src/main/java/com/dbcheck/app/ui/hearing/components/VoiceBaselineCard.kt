package com.dbcheck.app.ui.hearing.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.dbcheck.app.R
import com.dbcheck.app.ui.common.UiNumberFormatter
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.DbCheckCardEmphasis
import com.dbcheck.app.ui.components.ProLockOverlay
import com.dbcheck.app.ui.theme.DbCheckTheme

data class VoiceBaselineCardState(
    val levelDb: Float?,
    val sampleCount: Int,
    val canCalibrate: Boolean,
    val isLocked: Boolean,
)

data class VoiceBaselineCardActions(val onCalibrate: () -> Unit, val onUpgradeClick: () -> Unit)

@Composable
fun VoiceBaselineCard(
    state: VoiceBaselineCardState,
    actions: VoiceBaselineCardActions,
    modifier: Modifier = Modifier,
    cardEmphasis: DbCheckCardEmphasis = DbCheckCardEmphasis.Default,
) {
    ProLockOverlay(
        isLocked = state.isLocked,
        onUpgradeClick = actions.onUpgradeClick,
        modifier = modifier,
    ) {
        DbCheckCard(
            modifier = Modifier.fillMaxWidth(),
            emphasis = cardEmphasis,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space4),
            ) {
                Text(
                    text = stringResource(R.string.settings_voice_baseline_title),
                    style = DbCheckTheme.typography.bodyLg,
                    color = DbCheckTheme.colorScheme.material.onSurface,
                )
                Text(
                    text = stringResource(R.string.settings_voice_baseline_subtitle),
                    style = DbCheckTheme.typography.bodyMd,
                    color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
                )
                Text(
                    text = voiceBaselineLabel(state),
                    style = DbCheckTheme.typography.labelMd,
                    color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
                )
                DbCheckButton(
                    text = stringResource(R.string.settings_voice_baseline_button),
                    onClick = actions.onCalibrate,
                    enabled = state.canCalibrate,
                    style = DbCheckButtonStyle.Secondary,
                    height = DbCheckTheme.spacing.space12,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun voiceBaselineLabel(state: VoiceBaselineCardState): String = state.levelDb?.let { levelDb ->
    pluralStringResource(
        R.plurals.settings_voice_baseline_value,
        state.sampleCount,
        UiNumberFormatter.oneDecimal(levelDb),
        state.sampleCount,
    )
} ?: stringResource(R.string.settings_voice_baseline_empty)
