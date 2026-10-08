@file:Suppress("MatchingDeclarationName")

package com.dbcheck.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.dbcheck.app.R
import com.dbcheck.app.ui.theme.DbCheckTheme

data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String,
)

@Composable
fun BottomNavBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DbCheckTheme.colorScheme
    val surfaceColor = colors.material.surface

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(surfaceColor)
                .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                Box(
                    modifier = Modifier.weight(bottomNavItemSlotWeight(items.size)),
                    contentAlignment = Alignment.Center,
                ) {
                    BottomNavBarItem(
                        item = item,
                        isSelected = isSelected,
                        onClick = { onItemClick(item) },
                    )
                }
            }
        }
    }
}

internal fun bottomNavItemSlotWeight(itemCount: Int): Float = if (itemCount > 0) 1f else 0f

@Composable
private fun BottomNavBarItem(item: BottomNavItem, isSelected: Boolean, onClick: () -> Unit) {
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing
    val interactionSource = remember { MutableInteractionSource() }
    val selectedStateDescription = stringResource(R.string.a11y_selected)
    val notSelectedStateDescription = stringResource(R.string.a11y_not_selected)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = item.label
                    stateDescription =
                        if (isSelected) {
                            selectedStateDescription
                        } else {
                            notSelectedStateDescription
                        }
                }
                .selectable(
                    selected = isSelected,
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Tab,
                    onClick = onClick,
                ).padding(vertical = spacing.bottomNavItemVerticalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.bottomNavItemGap),
    ) {
        DbCheckNavigationIconPill(
            selected = isSelected,
            selectedIcon = item.selectedIcon,
            unselectedIcon = item.unselectedIcon,
        )
        Box(
            modifier = Modifier.heightIn(min = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.label,
                style = DbCheckTheme.typography.labelSm,
                color = if (isSelected) colors.accent else colors.material.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun DbCheckNavigationIconPill(
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    modifier: Modifier = Modifier,
) {
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .height(spacing.bottomNavPillHeight)
                .clip(CircleShape)
                .background(
                    if (selected) {
                        colors.accentContainer
                    } else {
                        Color.Transparent
                    },
                ).padding(horizontal = spacing.bottomNavPillHorizontalPadding),
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = null,
            tint = if (selected) colors.accent else colors.material.onSurfaceVariant,
            modifier = Modifier.size(spacing.bottomNavIconSize),
        )
    }
}
