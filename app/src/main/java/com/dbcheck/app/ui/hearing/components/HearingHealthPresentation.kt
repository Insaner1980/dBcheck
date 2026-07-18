package com.dbcheck.app.ui.hearing.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.dbcheck.app.R
import com.dbcheck.app.domain.hearing.HearingHealthStatus
import com.dbcheck.app.domain.hearing.HearingHealthSummary
import com.dbcheck.app.ui.theme.DbCheckTheme
import kotlin.math.abs

internal enum class HearingHealthVisual { INFO, SAFE, WARNING, DANGER }

internal data class HearingHealthText(@param:StringRes val resourceId: Int, val formatArgument: Int? = null)

internal data class HearingHealthPresentation(
    val visual: HearingHealthVisual,
    val statusText: HearingHealthText,
    val comparisonText: HearingHealthText?,
)

internal fun hearingHealthPresentation(summary: HearingHealthSummary?): HearingHealthPresentation {
    if (summary == null) {
        return HearingHealthPresentation(
            visual = HearingHealthVisual.INFO,
            statusText = HearingHealthText(R.string.hearing_status_row_no_data),
            comparisonText = null,
        )
    }

    val (visual, statusText) =
        when (summary.healthStatus) {
            HearingHealthStatus.SAFE ->
                HearingHealthVisual.SAFE to HearingHealthText(R.string.hearing_health_safe)

            HearingHealthStatus.WARNING ->
                HearingHealthVisual.WARNING to HearingHealthText(R.string.hearing_health_warning)

            HearingHealthStatus.DANGER ->
                HearingHealthVisual.DANGER to HearingHealthText(R.string.hearing_health_danger)
        }
    val comparisonText =
        when {
            summary.todayVsWeekPercent < 0 ->
                HearingHealthText(R.string.hearing_health_today_below, abs(summary.todayVsWeekPercent))

            summary.todayVsWeekPercent > 0 ->
                HearingHealthText(R.string.hearing_health_today_above, summary.todayVsWeekPercent)

            else -> HearingHealthText(R.string.hearing_health_today_matches)
        }

    return HearingHealthPresentation(
        visual = visual,
        statusText = statusText,
        comparisonText = comparisonText,
    )
}

internal data class HearingHealthVisualStyle(val icon: ImageVector, val tint: Color)

@Composable
internal fun HearingHealthVisual.resolveStyle(): HearingHealthVisualStyle {
    val colors = DbCheckTheme.colorScheme
    return when (this) {
        HearingHealthVisual.INFO -> HearingHealthVisualStyle(Icons.Outlined.Info, colors.material.primary)
        HearingHealthVisual.SAFE -> HearingHealthVisualStyle(Icons.Filled.CheckCircle, colors.success)
        HearingHealthVisual.WARNING -> HearingHealthVisualStyle(Icons.Filled.Warning, colors.warning)
        HearingHealthVisual.DANGER -> HearingHealthVisualStyle(Icons.Filled.Error, colors.material.error)
    }
}

@Composable
internal fun HearingHealthText.resolve(): String =
    formatArgument?.let { argument -> stringResource(resourceId, argument) } ?: stringResource(resourceId)
