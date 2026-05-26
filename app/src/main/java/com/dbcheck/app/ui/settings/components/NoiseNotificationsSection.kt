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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dbcheck.app.R
import com.dbcheck.app.data.local.preferences.model.UserPreferenceDefaults
import com.dbcheck.app.domain.noise.NoiseAlertPolicy
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
    val thresholdMin = UserPreferenceDefaults.NOTIFICATION_THRESHOLD_MIN.toFloat()
    val thresholdMax = UserPreferenceDefaults.NOTIFICATION_THRESHOLD_MAX.toFloat()
    val thresholdRange = thresholdMin..thresholdMax
    val thresholdValueLabel =
        notificationThresholdValueLabel(
            notificationThreshold = notificationThreshold,
            valueLabel = stringResource(R.string.notification_db_value, notificationThreshold),
            defaultValueLabel = stringResource(
                R.string.noise_notifications_threshold_default_value,
                notificationThreshold,
            ),
        )
    val thresholdReferenceLabel =
        stringResource(R.string.notification_db_value, UserPreferenceDefaults.NOTIFICATION_THRESHOLD)
    val thresholdMinLabel =
        stringResource(R.string.notification_db_value, UserPreferenceDefaults.NOTIFICATION_THRESHOLD_MIN)
    val thresholdMaxLabel =
        stringResource(R.string.notification_db_value, UserPreferenceDefaults.NOTIFICATION_THRESHOLD_MAX)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.noise_notifications_title),
            style = typography.labelMd,
            color = colors.material.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        DbCheckCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NotificationToggleRow(
                    title = stringResource(R.string.noise_notifications_exposure_alerts),
                    description =
                        stringResource(
                            R.string.noise_notifications_exposure_description,
                            NoiseAlertPolicy.EXPOSURE_DURATION_MINUTES,
                            notificationThreshold,
                        ),
                    checked = exposureAlertsEnabled,
                    onCheckedChange = onExposureAlertsChange,
                )

                NotificationToggleRow(
                    title = stringResource(R.string.noise_notifications_peak_warnings),
                    description =
                        stringResource(
                            R.string.noise_notifications_peak_description,
                            NoiseAlertPolicy.PEAK_WARNING_DB.toInt(),
                        ),
                    checked = peakWarningsEnabled,
                    onCheckedChange = onPeakWarningsChange,
                )

                NotificationThresholdControl(
                    notificationThreshold = notificationThreshold,
                    onThresholdChange = onThresholdChange,
                    thresholdRange = thresholdRange,
                    thresholdValueLabel = thresholdValueLabel,
                    thresholdMinLabel = thresholdMinLabel,
                    thresholdReferenceLabel = thresholdReferenceLabel,
                    thresholdMaxLabel = thresholdMaxLabel,
                )
            }
        }
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = typography.bodyLg, color = colors.material.onSurface)
            Text(description, style = typography.bodyMd, color = colors.material.onSurfaceVariant)
        }
        DbCheckToggle(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun NotificationThresholdControl(
    notificationThreshold: Int,
    onThresholdChange: (Int) -> Unit,
    thresholdRange: ClosedFloatingPointRange<Float>,
    thresholdValueLabel: String,
    thresholdMinLabel: String,
    thresholdReferenceLabel: String,
    thresholdMaxLabel: String,
) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme

    Column {
        Text(
            stringResource(R.string.noise_notifications_threshold),
            style = typography.bodyLg,
            color = colors.material.onSurface,
        )
        DbCheckSlider(
            value = notificationThreshold.toFloat(),
            onValueChange = { onThresholdChange(it.toInt()) },
            valueRange = thresholdRange,
            valueLabel = thresholdValueLabel,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                thresholdMinLabel,
                style = typography.labelSm,
                color = colors.material.onSurfaceVariant,
            )
            Text(
                thresholdReferenceLabel,
                style = typography.labelSm,
                color = colors.material.onSurfaceVariant,
            )
            Text(
                thresholdMaxLabel,
                style = typography.labelSm,
                color = colors.material.onSurfaceVariant,
            )
        }
    }
}

internal fun notificationThresholdValueLabel(
    notificationThreshold: Int,
    valueLabel: String,
    defaultValueLabel: String,
): String = if (notificationThreshold == UserPreferenceDefaults.NOTIFICATION_THRESHOLD) {
        defaultValueLabel
    } else {
        valueLabel
    }
