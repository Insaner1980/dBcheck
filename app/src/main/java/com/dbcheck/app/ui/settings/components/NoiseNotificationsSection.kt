package com.dbcheck.app.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.DbCheckSlider
import com.dbcheck.app.ui.components.DbCheckToggle
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun NoiseNotificationsSection(
    exposureAlertsEnabled: Boolean,
    peakWarningsEnabled: Boolean,
    notificationThreshold: Int,
    onExposureAlertsChange: (Boolean) -> Unit,
    onPeakWarningsChange: (Boolean) -> Unit,
    onThresholdChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "\uD83D\uDD14 NOISE NOTIFICATIONS",
            style = typography.labelMd,
            color = colors.material.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        DbCheckCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Exposure Alerts", style = typography.bodyLg, color = colors.material.onSurface)
                        Text("Notify when > 85dB for 30min", style = typography.bodyMd, color = colors.material.onSurfaceVariant)
                    }
                    DbCheckToggle(checked = exposureAlertsEnabled, onCheckedChange = onExposureAlertsChange)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Peak Warnings", style = typography.bodyLg, color = colors.material.onSurface)
                        Text("Alert for sudden > 120dB", style = typography.bodyMd, color = colors.material.onSurfaceVariant)
                    }
                    DbCheckToggle(checked = peakWarningsEnabled, onCheckedChange = onPeakWarningsChange)
                }

                Column {
                    Text("Notification Threshold", style = typography.bodyLg, color = colors.material.onSurface)
                    DbCheckSlider(
                        value = notificationThreshold.toFloat(),
                        onValueChange = { onThresholdChange(it.toInt()) },
                        valueRange = 60f..110f,
                        valueLabel = "$notificationThreshold dB${if (notificationThreshold <= 85) " (SAFE)" else ""}",
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("60 dB", style = typography.labelSm, color = colors.material.onSurfaceVariant)
                        Text("85 (SAFE)", style = typography.labelSm, color = colors.material.onSurfaceVariant)
                        Text("110 dB", style = typography.labelSm, color = colors.material.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
