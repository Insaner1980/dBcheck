package com.dbcheck.app.ui.hearing.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dbcheck.app.domain.hearing.HearingHealthSummary
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun HearingHealthCard(summary: HearingHealthSummary, modifier: Modifier = Modifier) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val presentation = hearingHealthPresentation(summary)
    val visualStyle = presentation.visual.resolveStyle()

    DbCheckCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = visualStyle.icon,
                contentDescription = null,
                tint = visualStyle.tint,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = presentation.statusText.resolve(),
                style = typography.bodyLg,
                color = colors.material.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = requireNotNull(presentation.comparisonText).resolve(),
                style = typography.bodyMd,
                color = colors.material.onSurfaceVariant,
            )
        }
    }
}
