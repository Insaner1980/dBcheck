package com.dbcheck.app.ui.analytics.components

import androidx.annotation.StringRes
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
import com.dbcheck.app.ui.analytics.state.DailyExposureUiState
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.theme.DbCheckTheme
import java.util.Locale

@Composable
fun ExposureSummaryCard(averageDb: Float, dailyAverages: List<DailyExposureUiState>, modifier: Modifier = Modifier) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    DbCheckCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.exposure_summary_last_7_days),
                        style = typography.labelMd,
                        color = colors.material.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f", averageDb),
                        style = typography.dataXl,
                        color = colors.material.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.exposure_summary_avg_db_day),
                        style = typography.labelSm,
                        color = colors.material.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            WeeklyBarChart(
                dailyAverages = dailyAverages,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(120.dp),
            )
        }
    }
}

@Composable
internal fun WeeklyExposureEmptyCard(state: WeeklyExposureSectionState, modifier: Modifier = Modifier) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    DbCheckCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(state.emptyTitleRes),
                style = typography.bodyLg,
                color = colors.material.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(state.emptyDescriptionRes),
                style = typography.bodyMd,
                color = colors.material.onSurfaceVariant,
            )
        }
    }
}

internal data class WeeklyExposureSectionState(
    val showExposureMetrics: Boolean,
    @param:StringRes val emptyTitleRes: Int,
    @param:StringRes val emptyDescriptionRes: Int,
)

internal fun weeklyExposureSectionState(hasExposureData: Boolean): WeeklyExposureSectionState = if (hasExposureData) {
        WeeklyExposureSectionState(
            showExposureMetrics = true,
            emptyTitleRes = R.string.exposure_summary_empty_title,
            emptyDescriptionRes = R.string.exposure_summary_empty_description,
        )
    } else {
        WeeklyExposureSectionState(
            showExposureMetrics = false,
            emptyTitleRes = R.string.exposure_summary_empty_title,
            emptyDescriptionRes = R.string.exposure_summary_empty_description,
        )
    }
