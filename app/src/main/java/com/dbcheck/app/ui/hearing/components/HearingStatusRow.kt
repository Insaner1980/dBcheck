package com.dbcheck.app.ui.hearing.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.dbcheck.app.R
import com.dbcheck.app.domain.hearing.HearingHealthSummary
import com.dbcheck.app.ui.theme.DbCheckRadii
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun HearingStatusRow(summary: HearingHealthSummary?, onNavigateToHearing: () -> Unit, modifier: Modifier = Modifier) {
    val presentation = hearingHealthPresentation(summary)
    val colors = DbCheckTheme.colorScheme
    val visualStyle = presentation.visual.resolveStyle()

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .sizeIn(minHeight = DbCheckTheme.spacing.space12)
                .clip(RoundedCornerShape(DbCheckRadii.Row))
                .background(colors.material.surfaceContainer)
                .clickable(role = Role.Button, onClick = onNavigateToHearing)
                .padding(DbCheckTheme.spacing.tilePadding),
        horizontalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = visualStyle.icon,
            contentDescription = null,
            tint = visualStyle.tint,
            modifier = Modifier.size(DbCheckTheme.spacing.space5),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space1),
        ) {
            Text(
                text = presentation.statusText.resolve(),
                style = DbCheckTheme.typography.bodyMd,
                color = colors.material.onSurface,
            )
            presentation.comparisonText?.let { comparison ->
                Text(
                    text = comparison.resolve(),
                    style = DbCheckTheme.typography.labelSm,
                    color = colors.material.onSurfaceVariant,
                )
            }
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = stringResource(R.string.hearing_status_row_open),
            tint = colors.material.onSurfaceVariant,
            modifier = Modifier.size(DbCheckTheme.spacing.space5),
        )
    }
}
