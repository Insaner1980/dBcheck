package com.dbcheck.app.ui.meter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dbcheck.app.ui.theme.DbCheckRadii
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
internal fun MetricValueTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(DbCheckRadii.Tile),
    valueMaxLines: Int = 1,
) {
    Column(
        modifier =
            modifier
                .clip(shape)
                .background(DbCheckTheme.colorScheme.material.surfaceContainerHighest)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space1),
    ) {
        Text(
            text = label.uppercase(),
            style = DbCheckTheme.typography.labelSm,
            color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = DbCheckTheme.typography.dataMd,
            color = DbCheckTheme.colorScheme.material.onSurface,
            maxLines = valueMaxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
