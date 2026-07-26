package com.dbcheck.app.ui.meter.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dbcheck.app.ui.theme.DbCheckSpacing

internal fun Modifier.expandableCardHeader(
    spacing: DbCheckSpacing,
    stateLabel: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    verticalPadding: Dp = 0.dp,
): Modifier = fillMaxWidth()
        .heightIn(min = spacing.space12)
        .semantics {
            stateDescription = stateLabel
        }.clickable(
            role = Role.Button,
            onClick = { onExpandedChange(!expanded) },
        ).padding(horizontal = spacing.cardPadding, vertical = verticalPadding)
