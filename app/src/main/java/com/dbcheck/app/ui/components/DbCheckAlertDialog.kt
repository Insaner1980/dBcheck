package com.dbcheck.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun DbCheckAlertDialog(
    title: String,
    body: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissText: String? = null,
    onDismissClick: (() -> Unit)? = null,
    icon: ImageVector? = null,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        icon =
            icon?.let {
                {
                    Icon(imageVector = it, contentDescription = null)
                }
            },
        title = {
            Text(
                text = title,
                style = DbCheckTheme.typography.headlineMd,
            )
        },
        text = {
            Text(
                text = body,
                style = DbCheckTheme.typography.bodyMd,
            )
        },
        confirmButton = {
            DbCheckButton(
                text = confirmText,
                onClick = onConfirm,
                height = DbCheckTheme.spacing.space12,
                contentPadding = PaddingValues(horizontal = DbCheckTheme.spacing.space6),
            )
        },
        dismissButton =
            dismissText?.let {
                {
                    DbCheckButton(
                        text = it,
                        onClick = onDismissClick ?: onDismiss,
                        style = DbCheckButtonStyle.Tertiary,
                        height = DbCheckTheme.spacing.space12,
                        contentPadding = PaddingValues(horizontal = DbCheckTheme.spacing.space4),
                    )
                }
            },
    )
}
