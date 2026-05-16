package com.dbcheck.app.ui.analytics.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dbcheck.app.ui.analytics.state.DailyExposureUiState
import com.dbcheck.app.ui.components.DbCheckMetricChartCard
import java.util.Locale

@Composable
fun ExposureSummaryCard(
    averageDb: Float,
    dailyAverages: List<DailyExposureUiState>,
    modifier: Modifier = Modifier,
) {
    DbCheckMetricChartCard(
        title = "LAST 7 DAYS (DB AVERAGE)",
        metricValue = String.format(Locale.getDefault(), "%.1f", averageDb),
        metricLabel = "AVG DB/DAY",
        modifier = modifier,
    ) {
        WeeklyBarChart(
            dailyAverages = dailyAverages,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(120.dp),
        )
    }
}
