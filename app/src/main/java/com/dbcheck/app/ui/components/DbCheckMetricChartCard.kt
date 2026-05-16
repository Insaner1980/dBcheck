package com.dbcheck.app.ui.components

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
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun DbCheckMetricChartCard(
    title: String,
    metricValue: String,
    metricLabel: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    footerContent: (@Composable () -> Unit)? = null,
    chartContent: @Composable () -> Unit,
) {
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing
    val typography = DbCheckTheme.typography

    DbCheckCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(title, style = typography.labelMd, color = colors.material.onSurfaceVariant)
                    subtitle?.let {
                        Text(it, style = typography.bodyMd, color = colors.material.onSurfaceVariant)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(metricValue, style = typography.dataXl, color = colors.material.onSurface)
                    Text(metricLabel, style = typography.labelSm, color = colors.material.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(spacing.space4))
            chartContent()

            footerContent?.let {
                Spacer(Modifier.height(spacing.space2))
                it()
            }
        }
    }
}
