@file:Suppress("MatchingDeclarationName")

package com.dbcheck.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dbcheck.app.R
import com.dbcheck.app.ui.theme.DbCheckTheme

enum class DbCheckChipDensity {
    Default,
    Compact,
}

@Composable
fun DbCheckChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    density: DbCheckChipDensity = DbCheckChipDensity.Default,
    showSelectedCheck: Boolean = false,
    horizontalPadding: Dp? = null,
) {
    val colors = DbCheckTheme.colorScheme
    val selectedStateDescription = stringResource(R.string.a11y_selected)
    val notSelectedStateDescription = stringResource(R.string.a11y_not_selected)
    val effectiveHorizontalPadding =
        horizontalPadding ?: when (density) {
            DbCheckChipDensity.Default -> 16.dp
            DbCheckChipDensity.Compact -> 10.dp
        }
    val contentColor =
        if (selected) {
            colors.material.onPrimaryContainer
        } else {
            colors.material.onSurfaceVariant
        }

    Box(
        modifier =
            modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .clip(CircleShape)
                .background(
                    if (selected) {
                        colors.material.primaryContainer
                    } else {
                        colors.material.surfaceContainerHigh
                    },
                ).semantics {
                    stateDescription =
                        if (selected) {
                            selectedStateDescription
                        } else {
                            notSelectedStateDescription
                        }
                }.selectable(
                    selected = selected,
                    role = Role.Checkbox,
                    onClick = onClick,
                )
                .padding(horizontal = effectiveHorizontalPadding, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selected && showSelectedCheck) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                    )
                }
                leadingIcon?.invoke()
                Text(
                    text = text,
                    style = DbCheckTheme.typography.labelLg,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
