package com.dbcheck.app.ui.meter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun StatCard(
    label: String,
    value: Float,
    modifier: Modifier = Modifier,
) {
    val colors = DbCheckTheme.colorScheme
    val shapes = DbCheckTheme.shapes
    val spacing = DbCheckTheme.spacing
    val typography = DbCheckTheme.typography

    Column(
        modifier =
            modifier
                .clip(shapes.large)
                .background(colors.material.surfaceContainerHigh)
                .padding(horizontal = spacing.space5, vertical = spacing.space4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.space1),
    ) {
        Text(
            text = label.uppercase(),
            style = typography.labelMd,
            color = colors.material.onSurfaceVariant,
        )
        Text(
            text = value.toInt().toString(),
            style = typography.dataLg,
            color = colors.material.onSurface,
        )
        Text(
            text = "dB",
            style = typography.labelSm,
            color = colors.material.onSurfaceVariant,
        )
    }
}
