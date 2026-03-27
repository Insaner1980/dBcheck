package com.dbcheck.app.ui.analytics.components

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
import androidx.compose.ui.unit.dp
import com.dbcheck.app.ui.analytics.state.HealthStatus
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.theme.DbCheckTheme
import kotlin.math.abs

@Composable
fun HearingHealthCard(
    healthStatus: HealthStatus,
    todayVsWeekPercent: Int,
    modifier: Modifier = Modifier,
) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    val (icon, tint, title) = when (healthStatus) {
        HealthStatus.SAFE -> Triple(Icons.Filled.CheckCircle, colors.success, "Your hearing is in the Safe Zone.")
        HealthStatus.WARNING -> Triple(Icons.Filled.Warning, colors.warning, "Noise levels are elevated.")
        HealthStatus.DANGER -> Triple(Icons.Filled.Error, colors.material.error, "Dangerous noise exposure detected.")
    }

    val comparisonText = when {
        todayVsWeekPercent < 0 -> "Exposure today is ${abs(todayVsWeekPercent)}% below your weekly average."
        todayVsWeekPercent > 0 -> "Exposure today is ${todayVsWeekPercent}% above your weekly average."
        else -> "Exposure today matches your weekly average."
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
            Spacer(Modifier.height(16.dp))
            DbCheckButton(
                text = "View Tips",
                onClick = { },
                height = 44.dp,
            )
        }
    }
}
