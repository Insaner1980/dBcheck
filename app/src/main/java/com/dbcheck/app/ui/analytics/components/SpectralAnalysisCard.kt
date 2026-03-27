package com.dbcheck.app.ui.analytics.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.ProLockOverlay
import com.dbcheck.app.ui.theme.DbCheckTheme
import kotlin.math.sin

@Composable
fun SpectralAnalysisCard(
    isLocked: Boolean,
    onUpgradeClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme

    ProLockOverlay(
        isLocked = isLocked,
        onUpgradeClick = onUpgradeClick,
        modifier = modifier,
    ) {
        DbCheckCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "SPECTRAL ANALYSIS",
                        style = typography.labelMd,
                        color = colors.material.onSurfaceVariant,
                    )
                    Text(
                        text = "● LIVE CAPTURE",
                        style = typography.labelMd,
                        color = colors.material.primary,
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Placeholder frequency bars
                val barColor = colors.material.primary.copy(alpha = 0.7f)
                val barHeights = remember {
                    List(24) { i ->
                        (sin(i * 0.5) * 0.4 + 0.3 + (if (i in 6..12) 0.3 else 0.0)).toFloat()
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                ) {
                    val gap = 3f
                    val barWidth = (size.width - gap * (barHeights.size - 1)) / barHeights.size

                    barHeights.forEachIndexed { index, height ->
                        val barH = height * size.height
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(index * (barWidth + gap), size.height - barH),
                            size = Size(barWidth, barH),
                            cornerRadius = CornerRadius(4f, 4f),
                        )
                    }
                }

                // Axis labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("20Hz", style = typography.labelSm, color = colors.material.onSurfaceVariant)
                    Text("1kHz", style = typography.labelSm, color = colors.material.onSurfaceVariant)
                    Text("20kHz", style = typography.labelSm, color = colors.material.onSurfaceVariant)
                }

                Spacer(Modifier.height(16.dp))

                // Data readouts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                ) {
                    Column {
                        Text("DOMINANT", style = typography.labelMd, color = colors.material.onSurfaceVariant)
                        Text("2.4 kHz", style = typography.dataLg, color = colors.material.onSurface)
                    }
                    Column {
                        Text("BANDWIDTH", style = typography.labelMd, color = colors.material.onSurfaceVariant)
                        Text("Wide", style = typography.dataLg, color = colors.material.onSurface)
                    }
                }
            }
        }
    }
}
