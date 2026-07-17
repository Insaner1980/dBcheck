package com.dbcheck.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.dbcheck.app.R
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun DbCheckTopAppBar(
    modifier: Modifier = Modifier,
    actionIcon: ImageVector? = null,
    actionContentDescription: String? = null,
    onActionClick: () -> Unit = {},
) {
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.pageMargin, vertical = spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_dbcheck_mark),
            contentDescription = stringResource(R.string.app_name),
            tint = colors.material.primary,
            modifier = Modifier.size(spacing.space8),
        )

        if (actionIcon != null) {
            IconButton(onClick = onActionClick) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = actionContentDescription,
                    tint = colors.material.onSurfaceVariant,
                    modifier = Modifier.size(spacing.space6),
                )
            }
        } else {
            Spacer(Modifier.size(spacing.space12))
        }
    }
}
