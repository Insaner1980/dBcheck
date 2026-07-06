package com.dbcheck.app.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.dbcheck.app.R
import com.dbcheck.app.data.local.preferences.model.UserPreferenceDefaults
import com.dbcheck.app.domain.noise.NoiseAlertPolicy
import com.dbcheck.app.domain.noise.NoiseNotificationSchedule
import com.dbcheck.app.domain.passive.PassiveMonitoringConfig
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.DbCheckChip
import com.dbcheck.app.ui.components.DbCheckSlider
import com.dbcheck.app.ui.components.ProLockOverlay
import com.dbcheck.app.ui.settings.state.PassiveMonitoringDailySummaryUiState
import com.dbcheck.app.ui.theme.DbCheckTheme
import java.time.DayOfWeek
import java.util.Locale
import kotlin.math.roundToInt

data class NoiseNotificationsSectionState(
    val exposureAlertsEnabled: Boolean,
    val peakWarningsEnabled: Boolean,
    val notificationThreshold: Int,
    val notificationSchedule: NoiseNotificationSchedule,
    val audibleAlarmEnabled: Boolean,
    val ttsRiskPromptEnabled: Boolean,
    val passiveMonitoringActive: Boolean,
    val passiveMonitoringDailySummary: PassiveMonitoringDailySummaryUiState,
    val passiveMonitoringErrorMessage: String?,
    val isProUser: Boolean,
)

data class NoiseNotificationsSectionActions(
    val onExposureAlertsChange: (Boolean) -> Unit,
    val onPeakWarningsChange: (Boolean) -> Unit,
    val onThresholdChange: (Int) -> Unit,
    val onScheduleChange: (NoiseNotificationSchedule) -> Unit,
    val onAudibleAlarmChange: (Boolean) -> Unit,
    val onTtsRiskPromptChange: (Boolean) -> Unit,
    val onAudibleAlarmPreview: () -> Unit,
    val onStartPassiveMonitoring: () -> Unit,
    val onStopPassiveMonitoring: () -> Unit,
    val onUpgradeClick: () -> Unit,
)

@Composable
@Suppress("LongMethod")
fun NoiseNotificationsSection(
    state: NoiseNotificationsSectionState,
    actions: NoiseNotificationsSectionActions,
    modifier: Modifier = Modifier,
) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme
    val exposureAlertsEnabled = state.exposureAlertsEnabled
    val peakWarningsEnabled = state.peakWarningsEnabled
    val notificationThreshold = state.notificationThreshold
    val notificationSchedule = state.notificationSchedule
    val audibleAlarmEnabled = state.audibleAlarmEnabled
    val ttsRiskPromptEnabled = state.ttsRiskPromptEnabled
    val passiveMonitoringActive = state.passiveMonitoringActive
    val passiveMonitoringDailySummary = state.passiveMonitoringDailySummary
    val passiveMonitoringErrorMessage = state.passiveMonitoringErrorMessage
    val isProUser = state.isProUser
    val onExposureAlertsChange = actions.onExposureAlertsChange
    val onPeakWarningsChange = actions.onPeakWarningsChange
    val onThresholdChange = actions.onThresholdChange
    val onScheduleChange = actions.onScheduleChange
    val onAudibleAlarmChange = actions.onAudibleAlarmChange
    val onTtsRiskPromptChange = actions.onTtsRiskPromptChange
    val onAudibleAlarmPreview = actions.onAudibleAlarmPreview
    val onStartPassiveMonitoring = actions.onStartPassiveMonitoring
    val onStopPassiveMonitoring = actions.onStopPassiveMonitoring
    val onUpgradeClick = actions.onUpgradeClick
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

                AudibleAlarmControls(
                    audibleAlarmEnabled = audibleAlarmEnabled,
                    isProUser = isProUser,
                    onAudibleAlarmChange = onAudibleAlarmChange,
                    onAudibleAlarmPreview = onAudibleAlarmPreview,
                    onUpgradeClick = onUpgradeClick,
                )

                TtsRiskPromptControls(
                    ttsRiskPromptEnabled = ttsRiskPromptEnabled,
                    isProUser = isProUser,
                    onTtsRiskPromptChange = onTtsRiskPromptChange,
                    onUpgradeClick = onUpgradeClick,
                )

                PassiveMonitoringControls(
                    active = passiveMonitoringActive,
                    dailySummary = passiveMonitoringDailySummary,
                    errorMessage = passiveMonitoringErrorMessage,
                    onStartPassiveMonitoring = onStartPassiveMonitoring,
                    onStopPassiveMonitoring = onStopPassiveMonitoring,
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

                NotificationScheduleControl(
                    schedule = notificationSchedule,
                    onScheduleChange = onScheduleChange,
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
    enabled: Boolean = true,
) {
    SettingsToggleDescriptionRow(
        title = title,
        subtitle = description,
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
    )
}

@Composable
private fun AudibleAlarmControls(
    audibleAlarmEnabled: Boolean,
    isProUser: Boolean,
    onAudibleAlarmChange: (Boolean) -> Unit,
    onAudibleAlarmPreview: () -> Unit,
    onUpgradeClick: () -> Unit,
) {
    ProLockOverlay(
        isLocked = !isProUser,
        onUpgradeClick = onUpgradeClick,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NotificationToggleRow(
                title = stringResource(R.string.noise_notifications_audible_alarm_title),
                description = stringResource(R.string.noise_notifications_audible_alarm_description),
                checked = audibleAlarmEnabled,
                onCheckedChange = onAudibleAlarmChange,
                enabled = isProUser,
            )
            DbCheckButton(
                text = stringResource(R.string.noise_notifications_audible_alarm_preview),
                onClick = onAudibleAlarmPreview,
                enabled = isProUser,
                style = DbCheckButtonStyle.Secondary,
                height = DbCheckTheme.spacing.space12,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TtsRiskPromptControls(
    ttsRiskPromptEnabled: Boolean,
    isProUser: Boolean,
    onTtsRiskPromptChange: (Boolean) -> Unit,
    onUpgradeClick: () -> Unit,
) {
    ProLockOverlay(
        isLocked = !isProUser,
        onUpgradeClick = onUpgradeClick,
    ) {
        NotificationToggleRow(
            title = stringResource(R.string.noise_notifications_tts_risk_prompt_title),
            description = stringResource(R.string.noise_notifications_tts_risk_prompt_description),
            checked = ttsRiskPromptEnabled,
            onCheckedChange = onTtsRiskPromptChange,
            enabled = isProUser,
        )
    }
}

@Composable
private fun PassiveMonitoringControls(
    active: Boolean,
    dailySummary: PassiveMonitoringDailySummaryUiState,
    errorMessage: String?,
    onStartPassiveMonitoring: () -> Unit,
    onStopPassiveMonitoring: () -> Unit,
) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val spacing = DbCheckTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
        SettingsDescriptionRow(
            title = stringResource(R.string.noise_notifications_passive_monitoring_title),
            subtitle =
                stringResource(
                    R.string.noise_notifications_passive_monitoring_subtitle,
                    PassiveMonitoringConfig.DEFAULT_SAMPLE_DURATION_MINUTES,
                ),
        )
        Text(
            text = stringResource(R.string.noise_notifications_passive_monitoring_disclosure),
            style = typography.bodyMd,
            color = colors.warning,
        )
        Text(
            text = passiveMonitoringSummaryLabel(dailySummary),
            style = typography.labelMd,
            color = colors.material.onSurfaceVariant,
        )
        errorMessage?.let { message ->
            Text(
                text = message,
                style = typography.bodyMd,
                color = colors.material.error,
            )
        }
        DbCheckButton(
            text =
                if (active) {
                    stringResource(R.string.noise_notifications_passive_monitoring_stop)
                } else {
                    stringResource(R.string.noise_notifications_passive_monitoring_start)
                },
            onClick = if (active) onStopPassiveMonitoring else onStartPassiveMonitoring,
            style = DbCheckButtonStyle.Secondary,
            height = spacing.space12,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun passiveMonitoringSummaryLabel(summary: PassiveMonitoringDailySummaryUiState): String =
    if (summary.hasSamples && summary.averageDb != null && summary.peakDb != null) {
        pluralStringResource(
            R.plurals.noise_notifications_passive_monitoring_summary,
            summary.sampleCount,
            summary.sampleCount,
            summary.averageDb.toInt(),
            summary.peakDb.toInt(),
        )
    } else {
        stringResource(R.string.noise_notifications_passive_monitoring_summary_empty)
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

@Composable
private fun NotificationScheduleControl(
    schedule: NoiseNotificationSchedule,
    onScheduleChange: (NoiseNotificationSchedule) -> Unit,
) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme
    val dayLabels = notificationScheduleDayLabels()
    val summary =
        notificationScheduleSummaryLabel(
            schedule = schedule,
            everyDayLabel = stringResource(R.string.noise_notifications_schedule_every_day),
            noDaysLabel = stringResource(R.string.noise_notifications_schedule_no_days),
            allDayLabel = stringResource(R.string.noise_notifications_schedule_all_day),
            overnightTemplate = stringResource(R.string.noise_notifications_schedule_overnight),
            windowTemplate = stringResource(R.string.noise_notifications_schedule_window),
            dayLabels = dayLabels,
        )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            Text(
                text = stringResource(R.string.noise_notifications_schedule_title),
                style = typography.bodyLg,
                color = colors.material.onSurface,
            )
            Text(
                text = stringResource(R.string.noise_notifications_schedule_description),
                style = typography.bodyMd,
                color = colors.material.onSurfaceVariant,
            )
            Text(
                text = summary,
                style = typography.labelMd,
                color = colors.material.primary,
            )
        }

        NotificationScheduleDayChips(
            schedule = schedule,
            dayLabels = dayLabels,
            onScheduleChange = onScheduleChange,
        )

        NotificationScheduleHourSlider(
            label = stringResource(R.string.noise_notifications_schedule_start),
            minuteOfDay = schedule.startMinuteOfDay,
            contentDescription =
                stringResource(
                    R.string.a11y_noise_notifications_schedule_start,
                    notificationScheduleTimeLabel(schedule.startMinuteOfDay),
                ),
            onMinuteChange = { startMinute ->
                onScheduleChange(schedule.copy(startMinuteOfDay = startMinute))
            },
        )

        NotificationScheduleHourSlider(
            label = stringResource(R.string.noise_notifications_schedule_end),
            minuteOfDay = schedule.endMinuteOfDay,
            contentDescription =
                stringResource(
                    R.string.a11y_noise_notifications_schedule_end,
                    notificationScheduleTimeLabel(schedule.endMinuteOfDay),
                ),
            onMinuteChange = { endMinute ->
                onScheduleChange(schedule.copy(endMinuteOfDay = endMinute))
            },
        )
    }
}

@Composable
private fun NotificationScheduleDayChips(
    schedule: NoiseNotificationSchedule,
    dayLabels: Map<DayOfWeek, String>,
    onScheduleChange: (NoiseNotificationSchedule) -> Unit,
) {
    val selectedStateDescription = stringResource(R.string.a11y_selected)
    val notSelectedStateDescription = stringResource(R.string.a11y_not_selected)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.noise_notifications_schedule_days),
            style = DbCheckTheme.typography.labelMd,
            color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
        )
        DayOfWeek.values().toList().chunked(DAY_CHIPS_PER_ROW).forEach { rowDays ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowDays.forEach { day ->
                    val selected = day in schedule.activeDays
                    NotificationScheduleDayChip(
                        dayLabel = dayLabels.getValue(day),
                        selected = selected,
                        selectedStateDescription = selectedStateDescription,
                        notSelectedStateDescription = notSelectedStateDescription,
                        onClick = {
                            onScheduleChange(
                                schedule.copy(activeDays = schedule.activeDays.toggle(day)),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationScheduleDayChip(
    dayLabel: String,
    selected: Boolean,
    selectedStateDescription: String,
    notSelectedStateDescription: String,
    onClick: () -> Unit,
) {
    val dayContentDescription =
        stringResource(
            if (selected) {
                R.string.a11y_noise_notifications_schedule_day_active
            } else {
                R.string.a11y_noise_notifications_schedule_day_inactive
            },
            dayLabel,
        )
    val dayStateDescription =
        if (selected) {
            selectedStateDescription
        } else {
            notSelectedStateDescription
        }

    DbCheckChip(
        text = dayLabel,
        selected = selected,
        onClick = onClick,
        horizontalPadding = 10.dp,
        modifier =
            Modifier.semantics {
                contentDescription = dayContentDescription
                stateDescription = dayStateDescription
            },
    )
}

@Composable
private fun NotificationScheduleHourSlider(
    label: String,
    minuteOfDay: Int,
    contentDescription: String,
    onMinuteChange: (Int) -> Unit,
) {
    val hour = minuteOfDay.minuteOfDayToHour()

    DbCheckSlider(
        value = hour.toFloat(),
        onValueChange = { rawHour ->
            onMinuteChange(rawHour.roundToInt().coerceIn(0, LAST_HOUR_OF_DAY) * MINUTES_PER_HOUR)
        },
        valueRange = 0f..LAST_HOUR_OF_DAY.toFloat(),
        steps = HOUR_SLIDER_STEPS,
        valueLabel = "$label ${notificationScheduleTimeLabel(minuteOfDay)}",
        modifier =
            Modifier.semantics {
                this.contentDescription = contentDescription
            },
    )
}

@Composable
private fun notificationScheduleDayLabels(): Map<DayOfWeek, String> = linkedMapOf(
        DayOfWeek.MONDAY to stringResource(R.string.noise_notifications_schedule_day_monday),
        DayOfWeek.TUESDAY to stringResource(R.string.noise_notifications_schedule_day_tuesday),
        DayOfWeek.WEDNESDAY to stringResource(R.string.noise_notifications_schedule_day_wednesday),
        DayOfWeek.THURSDAY to stringResource(R.string.noise_notifications_schedule_day_thursday),
        DayOfWeek.FRIDAY to stringResource(R.string.noise_notifications_schedule_day_friday),
        DayOfWeek.SATURDAY to stringResource(R.string.noise_notifications_schedule_day_saturday),
        DayOfWeek.SUNDAY to stringResource(R.string.noise_notifications_schedule_day_sunday),
    )

internal fun notificationThresholdValueLabel(
    notificationThreshold: Int,
    valueLabel: String,
    defaultValueLabel: String,
): String = if (notificationThreshold == UserPreferenceDefaults.NOTIFICATION_THRESHOLD) {
        defaultValueLabel
    } else {
        valueLabel
    }

internal fun notificationScheduleSummaryLabel(
    schedule: NoiseNotificationSchedule,
    everyDayLabel: String,
    noDaysLabel: String,
    allDayLabel: String,
    overnightTemplate: String,
    windowTemplate: String,
    dayLabels: Map<DayOfWeek, String>,
): String {
    val daySummary =
        when {
            schedule.activeDays == NoiseNotificationSchedule.ALL_DAYS -> everyDayLabel

            schedule.activeDays.isEmpty() -> noDaysLabel

            else ->
                schedule.activeDays
                    .sortedBy { day -> day.value }
                    .joinToString(", ") { day -> dayLabels.getValue(day) }
        }
    val windowSummary =
        notificationScheduleWindowLabel(
            schedule = schedule,
            allDayLabel = allDayLabel,
            overnightTemplate = overnightTemplate,
            windowTemplate = windowTemplate,
        )
    return "$daySummary - $windowSummary"
}

private fun notificationScheduleWindowLabel(
    schedule: NoiseNotificationSchedule,
    allDayLabel: String,
    overnightTemplate: String,
    windowTemplate: String,
): String = when {
    schedule.isFullDay -> allDayLabel

    schedule.startMinuteOfDay > schedule.endMinuteOfDay ->
        String.format(
            Locale.US,
            overnightTemplate,
            notificationScheduleTimeLabel(schedule.startMinuteOfDay),
            notificationScheduleTimeLabel(schedule.endMinuteOfDay),
        )

    else ->
        String.format(
            Locale.US,
            windowTemplate,
            notificationScheduleTimeLabel(schedule.startMinuteOfDay),
            notificationScheduleTimeLabel(schedule.endMinuteOfDay),
        )
}

private fun notificationScheduleTimeLabel(minuteOfDay: Int): String =
    String.format(Locale.US, "%02d:%02d", minuteOfDay / MINUTES_PER_HOUR, minuteOfDay % MINUTES_PER_HOUR)

private fun Int.minuteOfDayToHour(): Int = (this / MINUTES_PER_HOUR).coerceIn(0, LAST_HOUR_OF_DAY)

private fun Set<DayOfWeek>.toggle(day: DayOfWeek): Set<DayOfWeek> = if (day in this) {
        this - day
    } else {
        this + day
    }

private const val MINUTES_PER_HOUR = 60
private const val LAST_HOUR_OF_DAY = 23
private const val HOUR_SLIDER_STEPS = 22
private const val DAY_CHIPS_PER_ROW = 4
