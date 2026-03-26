package com.dbcheck.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dbcheck.app.ui.theme.DbCheckTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DbCheckSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    valueLabel: String? = null,
    enabled: Boolean = true,
) {
    val colors = DbCheckTheme.colorScheme

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (valueLabel != null) {
            Text(
                text = valueLabel,
                style = DbCheckTheme.typography.dataMd,
                color = colors.material.onSurface,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = colors.material.primary,
                activeTrackColor = colors.material.primary,
                inactiveTrackColor = colors.material.surfaceContainerHighest,
                disabledThumbColor = colors.material.onSurfaceVariant,
                disabledActiveTrackColor = colors.material.onSurfaceVariant.copy(alpha = 0.38f),
                disabledInactiveTrackColor = colors.material.surfaceContainerHighest,
            ),
        )
    }
}
