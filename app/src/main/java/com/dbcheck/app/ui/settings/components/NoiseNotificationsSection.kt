package com.dbcheck.app.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import com.dbcheck.app.R
import com.dbcheck.app.data.local.preferences.model.UserPreferenceDefaults
import com.dbcheck.app.domain.noise.NoiseAlertPolicy
import com.dbcheck.app.domain.noise.NoiseNotificationSchedule
import com.dbcheck.app.domain.passive.PassiveMonitoringConfig
import com.dbcheck.app.ui.components.DbCheckAlertDialog
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.DbCheckChip
import com.dbcheck.app.ui.components.DbCheckChipDensity
import com.dbcheck.app.ui.components.DbCheckSlider
import com.dbcheck.app.ui.components.ProLockOverlay
import com.dbcheck.app.ui.settings.state.PassiveMonitoringDailySummaryUiState
import com.dbcheck.app.ui.theme.DbCheckTheme
import java.time.DayOfWeek
import java.util.Locale
import kotlin.math.roundToInt

@Immutable
private data class NotificationScheduleContentState(val schedule: NoiseNotificationSchedule)

@Immutable
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
    val passiveMonitoringPermissionDenied: Boolean = false,
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
    val onOpenMicrophoneSettings: () -> Unit = {},
    val onUpgradeClick: () -> Unit,
)

@Composable
@Suppress("LongMethod")
fun NoiseNotificationsSection(
    state: NoiseNotificationsSectionState,
    actions: NoiseNotificationsSectionActions,
    modifier: Modifier = Modifier,
) {
    val spacing = DbCheckTheme.spacing
    val exposureAlertsEnabled = state.exposureAlertsEnabled
    val peakWarningsEnabled = state.peakWarningsEnabled
    val notificationThreshold = state.notificationThreshold
    val notificationSchedule = state.notificationSchedule
    val audibleAlarmEnabled = state.audibleAlarmEnabled
    val ttsRiskPromptEnabled = state.ttsRiskPromptEnabled
    val passiveMonitoringActive = state.passiveMonitoringActive
    val passiveMonitoringDailySummary = state.passiveMonitoringDailySummary
    val passiveMonitoringErrorMessage = state.passiveMonitoringErrorMessage
    val passiveMonitoringPermissionDenied = state.passiveMonitoringPermissionDenied
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
    val onOpenMicrophoneSettings = actions.onOpenMicrophoneSettings
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
        SettingsSectionHeader(title = stringResource(R.string.noise_notifications_title))

        DbCheckCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.space5),
            ) {
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
                    permissionDenied = passiveMonitoringPermissionDenied,
                    onStartPassiveMonitoring = onStartPassiveMonitoring,
                    onStopPassiveMonitoring = onStopPassiveMonitoring,
                    onOpenMicrophoneSettings = onOpenMicrophoneSettings,
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
                    scheduleState = NotificationScheduleContentState(notificationSchedule),
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
        Column(verticalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space5)) {
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
    permissionDenied: Boolean,
    onStartPassiveMonitoring: () -> Unit,
    onStopPassiveMonitoring: () -> Unit,
    onOpenMicrophoneSettings: () -> Unit,
) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val spacing = DbCheckTheme.spacing
    val startConfirmation = remember { PassiveMonitoringStartConfirmationController() }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
        SettingsDescriptionRow(
            title = stringResource(R.string.noise_notifications_passive_monitoring_title),
            subtitle =
                stringResource(
                    R.string.noise_notifications_passive_monitoring_subtitle,
                    PassiveMonitoringConfig.DEFAULT_SAMPLE_DURATION_MINUTES,
                ),
        )
        CompactDisclosureInfo(
            fullText = stringResource(R.string.noise_notifications_passive_monitoring_disclosure),
            compactLabel = stringResource(R.string.noise_notifications_passive_monitoring_disclosure_compact),
            dialogTitle = stringResource(R.string.noise_notifications_passive_monitoring_title),
            showFullInline = active,
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
        val passiveAction: () -> Unit =
            if (active) {
                onStopPassiveMonitoring
            } else {
                { startConfirmation.request() }
            }
        DbCheckButton(
            text =
                if (active) {
                    stringResource(R.string.noise_notifications_passive_monitoring_stop)
                } else {
                    stringResource(R.string.noise_notifications_passive_monitoring_start)
                },
            onClick = passiveAction,
            style = DbCheckButtonStyle.Secondary,
            height = spacing.space12,
            modifier = Modifier.fillMaxWidth(),
        )
        if (permissionDenied) {
            DbCheckButton(
                text = stringResource(R.string.action_open_settings),
                onClick = onOpenMicrophoneSettings,
                style = DbCheckButtonStyle.Secondary,
                height = spacing.space12,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (startConfirmation.isOpen) {
            PassiveMonitoringStartDialog(
                onConfirm = { startConfirmation.confirm(onStartPassiveMonitoring) },
                onCancel = startConfirmation::cancel,
                onDismiss = startConfirmation::dismiss,
            )
        }
    }
}

@Composable
private fun PassiveMonitoringStartDialog(onConfirm: () -> Unit, onCancel: () -> Unit, onDismiss: () -> Unit) {
    DbCheckAlertDialog(
        title = stringResource(R.string.noise_notifications_passive_monitoring_title),
        body = stringResource(R.string.noise_notifications_passive_monitoring_disclosure),
        confirmText = stringResource(R.string.noise_notifications_passive_monitoring_start),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        dismissText = stringResource(R.string.action_cancel),
        onDismissClick = onCancel,
    )
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
private fun NotificationLiveValueHeader(title: String, value: String) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val spacing = DbCheckTheme.spacing

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        Text(
            text = title,
            style = typography.bodyLg,
            color = colors.material.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = typography.labelMd,
            color = colors.material.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
        NotificationLiveValueHeader(
            title = stringResource(R.string.noise_notifications_threshold),
            value = thresholdValueLabel,
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
    scheduleState: NotificationScheduleContentState,
    onScheduleChange: (NoiseNotificationSchedule) -> Unit,
) {
    val schedule = scheduleState.schedule
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing
    val dayLabels = notificationScheduleDayLabels()
    val startTimeLabel = notificationScheduleTimeLabel(schedule.startMinuteOfDay)
    val endTimeLabel = notificationScheduleTimeLabel(schedule.endMinuteOfDay)
    val summary =
        notificationScheduleSummaryLabel(
            schedule = schedule,
            dayLabels =
                NotificationScheduleSummaryDayLabels(
                    everyDay = stringResource(R.string.noise_notifications_schedule_every_day),
                    noDays = stringResource(R.string.noise_notifications_schedule_no_days),
                    byDay = dayLabels,
                ),
            windowLabels =
                NotificationScheduleSummaryWindowLabels(
                    allDay = stringResource(R.string.noise_notifications_schedule_all_day),
                    overnightTemplate = stringResource(R.string.noise_notifications_schedule_overnight),
                    windowTemplate = stringResource(R.string.noise_notifications_schedule_window),
                    startTime = startTimeLabel,
                    endTime = endTimeLabel,
                ),
        )

    Column(verticalArrangement = Arrangement.spacedBy(spacing.space5)) {
        Column {
            NotificationLiveValueHeader(
                title = stringResource(R.string.noise_notifications_schedule_title),
                value = summary,
            )
            Text(
                text = stringResource(R.string.noise_notifications_schedule_description),
                style = typography.bodyMd,
                color = colors.material.onSurfaceVariant,
            )
        }

        NotificationScheduleDayChips(
            scheduleState = scheduleState,
            dayLabels = dayLabels,
            onScheduleChange = onScheduleChange,
        )

        NotificationScheduleHourSlider(
            label = stringResource(R.string.noise_notifications_schedule_start),
            minuteOfDay = schedule.startMinuteOfDay,
            contentDescription =
                stringResource(
                    R.string.a11y_noise_notifications_schedule_start,
                    startTimeLabel,
                ),
            timeLabel = startTimeLabel,
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
                    endTimeLabel,
                ),
            timeLabel = endTimeLabel,
            onMinuteChange = { endMinute ->
                onScheduleChange(schedule.copy(endMinuteOfDay = endMinute))
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotificationScheduleDayChips(
    scheduleState: NotificationScheduleContentState,
    dayLabels: Map<DayOfWeek, String>,
    onScheduleChange: (NoiseNotificationSchedule) -> Unit,
) {
    val schedule = scheduleState.schedule
    val selectedStateDescription = stringResource(R.string.a11y_selected)
    val notSelectedStateDescription = stringResource(R.string.a11y_not_selected)
    val spacing = DbCheckTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
        Text(
            text = stringResource(R.string.noise_notifications_schedule_days),
            style = DbCheckTheme.typography.labelMd,
            color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.space2),
            verticalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            DayOfWeek.values().forEach { day ->
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
        density = DbCheckChipDensity.Compact,
        showSelectedCheck = false,
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
    timeLabel: String,
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
        valueLabel = "$label $timeLabel",
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

internal data class NotificationScheduleSummaryDayLabels(
    val everyDay: String,
    val noDays: String,
    val byDay: Map<DayOfWeek, String>,
)

internal data class NotificationScheduleSummaryWindowLabels(
    val allDay: String,
    val overnightTemplate: String,
    val windowTemplate: String,
    val startTime: String,
    val endTime: String,
)

internal fun notificationScheduleSummaryLabel(
    schedule: NoiseNotificationSchedule,
    dayLabels: NotificationScheduleSummaryDayLabels,
    windowLabels: NotificationScheduleSummaryWindowLabels,
): String {
    val daySummary =
        when {
            schedule.activeDays == NoiseNotificationSchedule.ALL_DAYS -> dayLabels.everyDay

            schedule.activeDays.isEmpty() -> dayLabels.noDays

            else ->
                schedule.activeDays
                    .sortedBy { day -> day.value }
                    .joinToString(", ") { day -> dayLabels.byDay.getValue(day) }
        }
    val windowSummary =
        notificationScheduleWindowLabel(
            schedule = schedule,
            allDayLabel = windowLabels.allDay,
            overnightTemplate = windowLabels.overnightTemplate,
            windowTemplate = windowLabels.windowTemplate,
            startTimeLabel = windowLabels.startTime,
            endTimeLabel = windowLabels.endTime,
        )
    return "$daySummary - $windowSummary"
}

private fun notificationScheduleWindowLabel(
    schedule: NoiseNotificationSchedule,
    allDayLabel: String,
    overnightTemplate: String,
    windowTemplate: String,
    startTimeLabel: String,
    endTimeLabel: String,
): String = when {
    schedule.isFullDay -> allDayLabel

    schedule.startMinuteOfDay > schedule.endMinuteOfDay ->
        String.format(
            Locale.US,
            overnightTemplate,
            startTimeLabel,
            endTimeLabel,
        )

    else ->
        String.format(
            Locale.US,
            windowTemplate,
            startTimeLabel,
            endTimeLabel,
        )
}

@Composable
private fun notificationScheduleTimeLabel(minuteOfDay: Int): String = stringResource(
    R.string.noise_notifications_schedule_time,
    minuteOfDay / MINUTES_PER_HOUR,
    minuteOfDay % MINUTES_PER_HOUR,
)

private fun Int.minuteOfDayToHour(): Int = (this / MINUTES_PER_HOUR).coerceIn(0, LAST_HOUR_OF_DAY)

private fun Set<DayOfWeek>.toggle(day: DayOfWeek): Set<DayOfWeek> = if (day in this) {
        this - day
    } else {
        this + day
    }

private const val MINUTES_PER_HOUR = 60
private const val LAST_HOUR_OF_DAY = 23
private const val HOUR_SLIDER_STEPS = 22
