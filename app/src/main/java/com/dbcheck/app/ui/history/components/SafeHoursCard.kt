package com.dbcheck.app.ui.history.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dbcheck.app.R
import com.dbcheck.app.ui.common.currentLocale
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun SafeHoursCard(hours: Float?, modifier: Modifier = Modifier) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val locale = currentLocale()

    DbCheckCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = hours?.let { String.format(locale, "%.1fh", it) } ?: "--",
                style = typography.dataXl,
                color = if (hours != null) colors.success else colors.material.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.safe_hours_description),
                style = typography.bodyMd,
                color = colors.material.onSurfaceVariant,
            )
        }
    }
}
