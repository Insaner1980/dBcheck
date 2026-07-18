package com.dbcheck.app.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.dbcheck.app.R
import com.dbcheck.app.ui.components.DbCheckAlertDialog
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
internal fun CompactDisclosureInfo(
    fullText: String,
    compactLabel: String,
    dialogTitle: String,
    showFullInline: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing
    var dialogVisible by rememberSaveable { mutableStateOf(false) }

    if (showFullInline) {
        Text(
            text = fullText,
            style = DbCheckTheme.typography.bodyMd,
            color = colors.warning,
            modifier = modifier,
        )
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = compactLabel,
                style = DbCheckTheme.typography.bodyMd,
                color = colors.material.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { dialogVisible = true },
                modifier =
                    Modifier.sizeIn(
                        minWidth = spacing.space12,
                        minHeight = spacing.space12,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = compactLabel,
                    tint = colors.material.primary,
                )
            }
        }
    }

    if (dialogVisible) {
        DbCheckAlertDialog(
            title = dialogTitle,
            body = fullText,
            confirmText = stringResource(R.string.action_close),
            onConfirm = { dialogVisible = false },
            onDismiss = { dialogVisible = false },
            icon = Icons.Outlined.Info,
        )
    }
}
