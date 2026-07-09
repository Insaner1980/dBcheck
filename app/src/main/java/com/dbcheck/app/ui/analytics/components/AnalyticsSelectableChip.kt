package com.dbcheck.app.ui.analytics.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dbcheck.app.ui.components.DbCheckChip
import com.dbcheck.app.ui.components.DbCheckChipDensity

@Composable
internal fun AnalyticsSelectableChip(
    text: String,
    selected: Boolean,
    locked: Boolean,
    chipContentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DbCheckChip(
        text = text,
        selected = selected,
        onClick = onClick,
        density = DbCheckChipDensity.Compact,
        leadingIcon =
            if (locked) {
                {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                }
            } else {
                null
            },
        modifier =
            modifier.semantics {
                contentDescription = chipContentDescription
            },
    )
}
