package com.dbcheck.app.ui.hearing.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dbcheck.app.R
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.theme.DbCheckTheme
import kotlin.math.abs

enum class HearingHealthCardStatus { SAFE, WARNING, DANGER }

data class HearingHealthCardState(val healthStatus: HearingHealthCardStatus, val todayVsWeekPercent: Int)

@Composable
fun HearingHealthCard(state: HearingHealthCardState, modifier: Modifier = Modifier) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    val (icon, tint, title) =
        when (state.healthStatus) {
            HearingHealthCardStatus.SAFE ->
                Triple(Icons.Filled.CheckCircle, colors.success, stringResource(R.string.hearing_health_safe))

            HearingHealthCardStatus.WARNING ->
                Triple(Icons.Filled.Warning, colors.warning, stringResource(R.string.hearing_health_warning))

            HearingHealthCardStatus.DANGER ->
                Triple(Icons.Filled.Error, colors.material.error, stringResource(R.string.hearing_health_danger))
        }

    val comparisonText =
        when {
            state.todayVsWeekPercent < 0 ->
                stringResource(R.string.hearing_health_today_below, abs(state.todayVsWeekPercent))

            state.todayVsWeekPercent > 0 ->
                stringResource(R.string.hearing_health_today_above, state.todayVsWeekPercent)

            else -> stringResource(R.string.hearing_health_today_matches)
        }

    DbCheckCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = typography.bodyLg,
                color = colors.material.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = comparisonText,
                style = typography.bodyMd,
                color = colors.material.onSurfaceVariant,
            )
        }
    }
}
