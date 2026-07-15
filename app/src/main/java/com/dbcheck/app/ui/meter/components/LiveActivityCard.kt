package com.dbcheck.app.ui.meter.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
    modifier: Modifier = Modifier,
) {
    val spacing = DbCheckTheme.spacing
    val colors = DbCheckTheme.colorScheme

    DbCheckCard(
        modifier = modifier.fillMaxWidth(),
        emphasis = DbCheckCardEmphasis.Elevated,
        contentPadding = PaddingValues(spacing.cardPadding),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.groupGap),
        ) {
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
}
