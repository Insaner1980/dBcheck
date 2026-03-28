package com.dbcheck.app.ui.analytics.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dbcheck.app.data.local.db.dao.DailyAverage
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.DbCheckChip
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun MonthlyTrendChart(
    dailyAverages: List<DailyAverage>,
    selectedPeriod: String,
    onPeriodChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    DbCheckCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("EXPOSURE TRENDS", style = typography.labelMd, color = colors.material.onSurfaceVariant)

            Spacer(Modifier.height(12.dp))

            Row {
                listOf("Week" to "week", "Month" to "month", "Year" to "year").forEach { (label, value) ->
                    DbCheckChip(
                        text = label,
                        selected = selectedPeriod == value,
                        onClick = { onPeriodChange(value) },
                    )
                    Spacer(Modifier.width(8.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Reuse the bar chart for different time periods
            WeeklyBarChart(
                dailyAverages = dailyAverages,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp),
            )
        }
    }
}
