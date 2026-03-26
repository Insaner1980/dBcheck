package com.dbcheck.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dbcheck.app.ui.theme.DbCheckTheme
import com.dbcheck.app.ui.theme.ManropeFamily

@Composable
fun DbCheckTopAppBar(
    modifier: Modifier = Modifier,
    actionIcon: ImageVector? = null,
    onActionClick: () -> Unit = {},
) {
    val colors = DbCheckTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.GraphicEq,
                contentDescription = null,
                tint = colors.material.primary,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = "dBcheck",
                style = DbCheckTheme.typography.bodyLg.copy(
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = colors.material.onSurface,
            )
        }

        if (actionIcon != null) {
            IconButton(onClick = onActionClick) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = null,
                    tint = colors.material.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        } else {
            Spacer(Modifier.size(48.dp))
        }
    }
}
