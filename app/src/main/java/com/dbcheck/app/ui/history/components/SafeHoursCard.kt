package com.dbcheck.app.ui.history.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun SafeHoursCard(
    hours: Float,
    modifier: Modifier = Modifier,
) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    DbCheckCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = String.format("%.1fh", hours),
                style = typography.dataXl,
                color = colors.success,
            )
            Text(
                text = "Within safe dB limits",
                style = typography.bodyMd,
                color = colors.material.onSurfaceVariant,
            )
        }
    }
}
