package com.dbcheck.app.ui.meter.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.dbcheck.app.R
import com.dbcheck.app.data.local.preferences.model.WaveformStyle
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.DbCheckCardEmphasis
import com.dbcheck.app.ui.meter.state.LiveChartPointUiState
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun LiveActivityCard(
    points: List<LiveChartPointUiState>,
    isRecording: Boolean,
    waveformData: List<Float>,
    waveformStyle: WaveformStyle,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    DbCheckCard(
        modifier = modifier.fillMaxWidth(),
        emphasis = DbCheckCardEmphasis.Elevated,
        contentPadding = PaddingValues(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            LiveDetailsHeader(
                isRecording = isRecording,
                expanded = expanded,
                onExpandedChange = onExpandedChange,
            )

            if (expanded) {
                LiveDetailsBody(
                    points = points,
                    isRecording = isRecording,
                    waveformData = waveformData,
                    waveformStyle = waveformStyle,
                )
            }
        }
    }
}

@Composable
private fun LiveDetailsHeader(isRecording: Boolean, expanded: Boolean, onExpandedChange: (Boolean) -> Unit) {
    val spacing = DbCheckTheme.spacing
    val colors = DbCheckTheme.colorScheme
    val stateLabel =
        stringResource(
            if (expanded) {
                R.string.a11y_meter_live_details_expanded
            } else {
                R.string.a11y_meter_live_details_collapsed
            },
        )
    val actionLabel =
        stringResource(
            if (expanded) {
                R.string.a11y_meter_live_details_collapse
            } else {
                R.string.a11y_meter_live_details_expand
            },
        )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = spacing.space12)
                .semantics {
                    this.stateDescription = stateLabel
                }.clickable(
                    role = Role.Button,
                    onClick = { onExpandedChange(!expanded) },
                ).padding(horizontal = spacing.cardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.meter_live_details),
            style = DbCheckTheme.typography.labelLg,
            color = colors.material.onSurface,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text =
                stringResource(
                    if (isRecording) {
                        R.string.meter_live_activity_recording
                    } else {
                        R.string.meter_live_activity_live
                    },
                ),
            style = DbCheckTheme.typography.labelMd,
            color = if (isRecording) colors.material.error else colors.material.primary,
        )
        Icon(
            imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = actionLabel,
            tint = colors.material.onSurfaceVariant,
        )
    }
}

@Composable
private fun LiveDetailsBody(
    points: List<LiveChartPointUiState>,
    isRecording: Boolean,
    waveformData: List<Float>,
    waveformStyle: WaveformStyle,
) {
    val spacing = DbCheckTheme.spacing

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = spacing.cardPadding,
                    end = spacing.cardPadding,
                    bottom = spacing.cardPadding,
                ),
        verticalArrangement = Arrangement.spacedBy(spacing.groupGap),
    ) {
        LiveSoundLevelChart(
            points = points,
            isRecording = isRecording,
        )
        WaveformVisualization(
            data = waveformData,
            style = waveformStyle,
            height = spacing.space8,
        )
    }
}
