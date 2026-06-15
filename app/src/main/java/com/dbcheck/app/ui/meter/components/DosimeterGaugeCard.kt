package com.dbcheck.app.ui.meter.components

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dbcheck.app.R
import com.dbcheck.app.domain.noise.DosimeterStandard
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.meter.state.DosimeterUiState
import com.dbcheck.app.ui.theme.DbCheckTheme
import com.dbcheck.app.util.DurationFormatter
import com.dbcheck.app.util.ReportTextFormatter
import kotlin.math.roundToInt

@Composable
fun DosimeterGaugeCard(dosimeter: DosimeterUiState, modifier: Modifier = Modifier) {
    when (dosimeter) {
        DosimeterUiState.LockedPreview ->
            DosimeterMessageCard(
                message = stringResource(R.string.meter_dosimeter_locked_description),
                contentDescription = stringResource(R.string.a11y_dosimeter_gauge_locked),
                modifier = modifier,
            )

        is DosimeterUiState.Unavailable ->
            DosimeterMessageCard(
                standard = dosimeter.standard,
                message = stringResource(R.string.meter_dosimeter_unavailable_description),
                contentDescription =
                    stringResource(
                        R.string.a11y_dosimeter_gauge_unavailable,
                        stringResource(dosimeter.standard.labelRes()),
                    ),
                modifier = modifier,
            )

        is DosimeterUiState.Data ->
            DosimeterDataCard(
                dosimeter = dosimeter,
                modifier = modifier,
            )
    }
}

@Composable
private fun DosimeterMessageCard(
    message: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    standard: DosimeterStandard? = null,
) {
    DbCheckCard(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { this.contentDescription = contentDescription },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            DosimeterHeader(standard = standard)
            Spacer(Modifier.height(DbCheckTheme.spacing.space4))
            Text(
                text = message,
                style = DbCheckTheme.typography.bodyMd,
                color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DosimeterDataCard(dosimeter: DosimeterUiState.Data, modifier: Modifier = Modifier) {
    val unavailableLabel = stringResource(R.string.value_unavailable)
    val standardLabel = stringResource(dosimeter.standard.labelRes())
    val doseLabel = DosimeterGaugeFormatter.percent(dosimeter.dosePercent)
    val twaLabel = DosimeterGaugeFormatter.decibel(dosimeter.twaDb)
    val laeqLabel = DosimeterGaugeFormatter.decibel(dosimeter.laeqDb)
    val projectedDoseLabel = DosimeterGaugeFormatter.percent(dosimeter.projectedDosePercent)
    val remainingLabel = DosimeterGaugeFormatter.remainingTime(dosimeter.remainingExposureMs, unavailableLabel)
    val contentDescription =
        stringResource(
            R.string.a11y_dosimeter_gauge_data,
            standardLabel,
            doseLabel,
            twaLabel,
            laeqLabel,
            projectedDoseLabel,
            remainingLabel,
        )

    DbCheckCard(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { this.contentDescription = contentDescription },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            DosimeterHeader(standard = dosimeter.standard)

            Spacer(Modifier.height(DbCheckTheme.spacing.space4))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DosimeterGauge(
                    dosePercent = dosimeter.dosePercent,
                    doseLabel = doseLabel,
                )

                Spacer(Modifier.width(DbCheckTheme.spacing.space4))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space3),
                ) {
                    MetricValueTile(
                        label = stringResource(R.string.report_metric_twa),
                        value = twaLabel,
                    )
                    MetricValueTile(
                        label = stringResource(R.string.report_metric_laeq),
                        value = laeqLabel,
                    )
                }
            }

            Spacer(Modifier.height(DbCheckTheme.spacing.space3))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space3),
            ) {
                MetricValueTile(
                    label = stringResource(R.string.meter_dosimeter_projected_dose),
                    value = projectedDoseLabel,
                    modifier = Modifier.weight(1f),
                )
                MetricValueTile(
                    label = stringResource(R.string.meter_dosimeter_remaining),
                    value = remainingLabel,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(DbCheckTheme.spacing.space3))

            Text(
                text = stringResource(dosimeter.standard.referenceRes()),
                style = DbCheckTheme.typography.labelSm,
                color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DosimeterHeader(standard: DosimeterStandard?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.meter_dosimeter_title),
            style = DbCheckTheme.typography.labelMd,
            color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
        )

        if (standard != null) {
            DosimeterStandardBadge(label = stringResource(standard.labelRes()))
        }
    }
}

@Composable
private fun DosimeterStandardBadge(label: String) {
    Text(
        text = label,
        style = DbCheckTheme.typography.labelSm,
        color = DbCheckTheme.colorScheme.material.onPrimaryContainer,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier =
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(DbCheckTheme.colorScheme.material.primaryContainer)
                .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun DosimeterGauge(dosePercent: Float, doseLabel: String) {
    val colors = DbCheckTheme.colorScheme
    val riskLevel = DosimeterGaugeFormatter.riskLevel(dosePercent)
    val progressColor =
        when (riskLevel) {
            DosimeterGaugeRiskLevel.LOW -> colors.success
            DosimeterGaugeRiskLevel.NEAR_LIMIT -> colors.warning
            DosimeterGaugeRiskLevel.OVER_LIMIT -> colors.material.error
        }
    val trackColor = colors.material.outlineVariant.copy(alpha = 0.32f)

    Box(
        modifier = Modifier.size(DOSIMETER_GAUGE_SIZE_DP),
        contentAlignment = Alignment.Center,
    ) {
        DosimeterGaugeCanvas(
            dosePercent = dosePercent,
            trackColor = trackColor,
            progressColor = progressColor,
            modifier = Modifier.size(DOSIMETER_GAUGE_SIZE_DP),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = doseLabel,
                style = DbCheckTheme.typography.dataXl,
                color = colors.material.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.meter_dosimeter_dose_label),
                style = DbCheckTheme.typography.labelSm,
                color = colors.material.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DosimeterGaugeCanvas(
    dosePercent: Float,
    trackColor: Color,
    progressColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokeWidth = DOSIMETER_GAUGE_STROKE_DP.toPx()
        val arcSize = Size(
            width = size.width - strokeWidth,
            height = size.height - strokeWidth,
        )
        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        drawArc(
            color = trackColor,
            startAngle = DOSIMETER_GAUGE_START_ANGLE,
            sweepAngle = DOSIMETER_GAUGE_SWEEP_ANGLE,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke,
        )
        drawArc(
            color = progressColor,
            startAngle = DOSIMETER_GAUGE_START_ANGLE,
            sweepAngle = DOSIMETER_GAUGE_SWEEP_ANGLE * DosimeterGaugeFormatter.doseProgressFraction(dosePercent),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke,
        )
    }
}

internal object DosimeterGaugeFormatter {
    fun doseProgressFraction(dosePercent: Float): Float = (dosePercent / PERCENT_TOTAL).coerceIn(0f, 1f)

    fun riskLevel(dosePercent: Float): DosimeterGaugeRiskLevel = when {
        dosePercent >= PERCENT_TOTAL -> DosimeterGaugeRiskLevel.OVER_LIMIT
        dosePercent >= NEAR_LIMIT_PERCENT -> DosimeterGaugeRiskLevel.NEAR_LIMIT
        else -> DosimeterGaugeRiskLevel.LOW
    }

    fun percent(value: Float): String = "${value.roundToInt()}%"

    fun decibel(value: Float): String = "${ReportTextFormatter.oneDecimal(value)} dB"

    fun remainingTime(remainingExposureMs: Long?, unavailableLabel: String): String =
        remainingExposureMs?.let(DurationFormatter::formatClockDuration) ?: unavailableLabel

    private const val PERCENT_TOTAL = 100f
    private const val NEAR_LIMIT_PERCENT = 80f
}

internal enum class DosimeterGaugeRiskLevel {
    LOW,
    NEAR_LIMIT,
    OVER_LIMIT,
}

@StringRes
private fun DosimeterStandard.labelRes(): Int = when (this) {
    DosimeterStandard.NIOSH_REL -> R.string.meter_dosimeter_standard_niosh_rel
    DosimeterStandard.OSHA_PEL -> R.string.meter_dosimeter_standard_osha_pel
}

@StringRes
private fun DosimeterStandard.referenceRes(): Int = when (this) {
    DosimeterStandard.NIOSH_REL -> R.string.meter_dosimeter_reference_niosh_rel
    DosimeterStandard.OSHA_PEL -> R.string.meter_dosimeter_reference_osha_pel
}

private val DOSIMETER_GAUGE_SIZE_DP = 140.dp
private val DOSIMETER_GAUGE_STROKE_DP = 14.dp
private const val DOSIMETER_GAUGE_START_ANGLE = 140f
private const val DOSIMETER_GAUGE_SWEEP_ANGLE = 260f
