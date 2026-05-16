package com.dbcheck.app.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dbcheck.app.data.local.preferences.model.UserPreferenceDefaults
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
    val thresholdMin = UserPreferenceDefaults.NOTIFICATION_THRESHOLD_MIN.toFloat()
    val thresholdMax = UserPreferenceDefaults.NOTIFICATION_THRESHOLD_MAX.toFloat()
    val thresholdRange = thresholdMin..thresholdMax

    SettingsSectionCard(title = "\uD83D\uDD14 NOISE NOTIFICATIONS", modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Exposure Alerts", style = typography.bodyLg, color = colors.material.onSurface)
                Text(
                    exposureAlertDescription(notificationThreshold),
                    style = typography.bodyMd,
                    color = colors.material.onSurfaceVariant,
                )
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
                Text(
                    PEAK_WARNING_DESCRIPTION,
                    style = typography.bodyMd,
                    color = colors.material.onSurfaceVariant,
                )
            }
            DbCheckToggle(checked = peakWarningsEnabled, onCheckedChange = onPeakWarningsChange)
        }

        Column {
            Text("Notification Threshold", style = typography.bodyLg, color = colors.material.onSurface)
            DbCheckSlider(
                value = notificationThreshold.toFloat(),
                onValueChange = { onThresholdChange(it.toInt()) },
                valueRange = thresholdRange,
                valueLabel = notificationThresholdValueLabel(notificationThreshold),
            )
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                Text(
                    "${UserPreferenceDefaults.NOTIFICATION_THRESHOLD_MIN} dB",
                    style = typography.labelSm,
                    color = colors.material.onSurfaceVariant,
                )
                Text(
                    notificationThresholdReferenceLabel(),
                    style = typography.labelSm,
                    color = colors.material.onSurfaceVariant,
                    )
                Text(
                    "${UserPreferenceDefaults.NOTIFICATION_THRESHOLD_MAX} dB",
                    style = typography.labelSm,
                    color = colors.material.onSurfaceVariant,
                )
            }
        }
    }
}

internal fun exposureAlertDescription(notificationThreshold: Int): String =
    "Alert when 30 min average reaches $notificationThreshold dB"

internal const val PEAK_WARNING_DESCRIPTION = "Alert when peak reaches 120 dB"

internal fun notificationThresholdValueLabel(notificationThreshold: Int): String = "$notificationThreshold dB" +
    if (notificationThreshold == UserPreferenceDefaults.NOTIFICATION_THRESHOLD) {
        " (default)"
    } else {
        ""
    }

internal fun notificationThresholdReferenceLabel(): String = "${UserPreferenceDefaults.NOTIFICATION_THRESHOLD} dB"
