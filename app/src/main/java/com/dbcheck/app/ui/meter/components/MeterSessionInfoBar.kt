package com.dbcheck.app.ui.meter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dbcheck.app.R
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.meter.state.MeterSessionInfoUiState
import com.dbcheck.app.ui.theme.DbCheckTheme
import com.dbcheck.app.util.DurationFormatter
import com.dbcheck.app.util.displayNameStringRes
import java.util.Locale

private data class MeterSessionInfoLabels(
    val duration: String,
    val weighting: String,
    val response: String,
    val sampleRate: String,
    val inputDevice: String,
)

@Composable
fun MeterSessionInfoBar(sessionInfo: MeterSessionInfoUiState, modifier: Modifier = Modifier) {
    val labels =
        MeterSessionInfoLabels(
            duration = MeterSessionInfoFormatter.durationLabel(sessionInfo.durationMs),
            weighting = stringResource(sessionInfo.weighting.displayNameStringRes()),
            response = stringResource(sessionInfo.responseTime.displayNameStringRes()),
            sampleRate = MeterSessionInfoFormatter.sampleRateLabel(sessionInfo.sampleRateHz),
            inputDevice =
                MeterSessionInfoFormatter.inputDeviceLabel(
                    inputDeviceName = sessionInfo.inputDeviceName,
                    defaultInputLabel = stringResource(R.string.meter_session_input_default),
                ),
        )
    val contentDescription = meterSessionInfoContentDescription(sessionInfo.showProDetails, labels)

    DbCheckCard(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { this.contentDescription = contentDescription },
    ) {
        MeterSessionInfoContent(labels = labels, showProDetails = sessionInfo.showProDetails)
    }
}

@Composable
private fun meterSessionInfoContentDescription(showProDetails: Boolean, labels: MeterSessionInfoLabels): String =
    if (showProDetails) {
        stringResource(
            R.string.a11y_meter_session_info_pro,
            labels.duration,
            labels.weighting,
            labels.response,
            labels.sampleRate,
            labels.inputDevice,
        )
    } else {
        stringResource(
            R.string.a11y_meter_session_info,
            labels.duration,
            labels.weighting,
            labels.response,
        )
    }

@Composable
private fun MeterSessionInfoContent(labels: MeterSessionInfoLabels, showProDetails: Boolean) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RecordingIndicator()
            Text(
                text = labels.duration,
                style = DbCheckTheme.typography.dataMd,
                color = DbCheckTheme.colorScheme.material.onSurface,
            )
        }

        Spacer(Modifier.height(DbCheckTheme.spacing.space3))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space3),
        ) {
            SessionInfoPill(
                label = stringResource(R.string.meter_session_weighting),
                value = labels.weighting,
                modifier = Modifier.weight(1f),
            )
            SessionInfoPill(
                label = stringResource(R.string.meter_session_response),
                value = labels.response,
                modifier = Modifier.weight(1f),
            )
        }

        if (showProDetails) {
            Spacer(Modifier.height(DbCheckTheme.spacing.space3))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space3),
            ) {
                SessionInfoPill(
                    label = stringResource(R.string.meter_session_sample_rate),
                    value = labels.sampleRate,
                    modifier = Modifier.weight(0.8f),
                )
                SessionInfoPill(
                    label = stringResource(R.string.meter_session_input),
                    value = labels.inputDevice,
                    modifier = Modifier.weight(1.2f),
                )
            }
        }
    }
}

@Composable
private fun RecordingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space2),
    ) {
        Box(
            modifier =
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(DbCheckTheme.colorScheme.material.error),
        )
        Text(
            text = stringResource(R.string.meter_session_rec),
            style = DbCheckTheme.typography.labelMd,
            color = DbCheckTheme.colorScheme.material.error,
        )
    }
}

@Composable
private fun SessionInfoPill(label: String, value: String, modifier: Modifier = Modifier) {
    MetricValueTile(
        label = label,
        value = value,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        valueMaxLines = 2,
    )
}

internal object MeterSessionInfoFormatter {
    fun durationLabel(durationMs: Long): String = DurationFormatter.formatClockDuration(durationMs)

    fun sampleRateLabel(sampleRateHz: Int): String {
        val sampleRateKhz = sampleRateHz / HERTZ_PER_KILOHERTZ.toFloat()
        return if (sampleRateHz % HERTZ_PER_KILOHERTZ == 0) {
            "${sampleRateHz / HERTZ_PER_KILOHERTZ} kHz"
        } else {
            "%.1f kHz".format(Locale.US, sampleRateKhz)
        }
    }

    fun inputDeviceLabel(inputDeviceName: String?, defaultInputLabel: String): String =
        inputDeviceName?.trim()?.takeIf { it.isNotEmpty() } ?: defaultInputLabel

    private const val HERTZ_PER_KILOHERTZ = 1_000
}
