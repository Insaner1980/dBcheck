package com.dbcheck.app.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dbcheck.app.ui.theme.DbCheckOpacity
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
    val spacing = DbCheckTheme.spacing
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
                    .height(spacing.space16),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                BottomNavBarItem(
                    item = item,
                    isSelected = isSelected,
                    onClick = { onItemClick(item) },
                )
            }
        }
    }
}

@Composable
private fun BottomNavBarItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = DbCheckTheme.colorScheme
    val shapes = DbCheckTheme.shapes
    val spacing = DbCheckTheme.spacing
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier =
            Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ).padding(horizontal = spacing.space4, vertical = spacing.space2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.space1),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                if (isSelected) {
                    Modifier
                        .clip(shapes.large)
                        .background(colors.material.primary.copy(alpha = DbCheckOpacity.SELECTED_STATE_LAYER))
                        .padding(horizontal = spacing.space4, vertical = spacing.space1)
                } else {
                    Modifier.padding(horizontal = spacing.space4, vertical = spacing.space1)
                },
        ) {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.label,
                tint = if (isSelected) colors.material.primary else colors.material.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
        if (isSelected) {
            Text(
                text = item.label,
                style = DbCheckTheme.typography.labelSm,
                color = colors.material.primary,
            )
        }
    }
}
