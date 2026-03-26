package com.dbcheck.app.ui.history.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun WeeklyTrendCard(
    percent: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    DbCheckCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "${if (percent >= 0) "+" else ""}$percent%",
                style = typography.dataXl,
                color = if (percent < 0) colors.success else colors.warning,
            )
            Text(
                text = label,
                style = typography.bodyMd,
                color = colors.material.onSurfaceVariant,
            )
        }
    }
}
