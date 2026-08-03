package com.dbcheck.app.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.DpSize
import com.dbcheck.app.ui.theme.DbCheckTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DbCheckSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueLabel: String,
    minLabel: String,
    maxLabel: String,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
) {
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing
    val interactionSource = remember { MutableInteractionSource() }
    val sliderColors =
        SliderDefaults.colors(
            thumbColor = colors.accent,
            activeTrackColor = colors.accent,
            inactiveTrackColor = colors.material.surfaceContainerHighest,
            disabledThumbColor = colors.material.onSurfaceVariant,
            disabledActiveTrackColor = colors.material.onSurfaceVariant.copy(alpha = 0.38f),
            disabledInactiveTrackColor = colors.material.surfaceContainerHighest,
        )

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics {
                    stateDescription = "$valueLabel, $minLabel – $maxLabel"
                },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = valueLabel,
            style = DbCheckTheme.typography.dataMd,
            color =
                if (enabled) {
                    colors.material.onSurface
                } else {
                    colors.material.onSurfaceVariant
                },
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            interactionSource = interactionSource,
            colors = sliderColors,
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    colors = sliderColors,
                    enabled = enabled,
                    thumbSize = DpSize(spacing.sliderThumbSize, spacing.sliderThumbSize),
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(spacing.sliderTrackHeight),
                    enabled = enabled,
                    colors = sliderColors,
                    drawStopIndicator = null,
                    drawTick = { _, _ -> },
                )
            },
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = minLabel,
                style = DbCheckTheme.typography.labelSm,
                color = colors.material.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = maxLabel,
                style = DbCheckTheme.typography.labelSm,
                color = colors.material.onSurfaceVariant,
            )
        }
    }
}
