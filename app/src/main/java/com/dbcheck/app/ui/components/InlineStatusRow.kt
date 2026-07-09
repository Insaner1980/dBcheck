package com.dbcheck.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.dbcheck.app.ui.theme.DbCheckRadii
import com.dbcheck.app.ui.theme.DbCheckTheme

enum class InlineStatusTone {
    Info,
    Success,
    Warning,
    Error,
}

@Composable
fun InlineStatusRow(
    text: String,
    modifier: Modifier = Modifier,
    tone: InlineStatusTone = InlineStatusTone.Info,
    icon: ImageVector? = null,
) {
    val colors = DbCheckTheme.colorScheme
    val statusColor = inlineStatusColor(tone)

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(DbCheckRadii.Row))
                .padding(horizontal = DbCheckTheme.spacing.space4, vertical = DbCheckTheme.spacing.space3),
        horizontalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon ?: inlineStatusIcon(tone),
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier.size(DbCheckTheme.spacing.space5),
        )
        Text(
            text = text,
            style = DbCheckTheme.typography.bodyMd,
            color = colors.material.onSurface,
        )
    }
}

@Composable
private fun inlineStatusColor(tone: InlineStatusTone): Color {
    val colors = DbCheckTheme.colorScheme
    return when (tone) {
        InlineStatusTone.Info -> colors.material.primary
        InlineStatusTone.Success -> colors.success
        InlineStatusTone.Warning -> colors.warning
        InlineStatusTone.Error -> colors.material.error
    }
}

private fun inlineStatusIcon(tone: InlineStatusTone): ImageVector =
    when (tone) {
        InlineStatusTone.Info -> Icons.Outlined.Info
        InlineStatusTone.Success -> Icons.Outlined.CheckCircle
        InlineStatusTone.Warning -> Icons.Outlined.WarningAmber
        InlineStatusTone.Error -> Icons.Outlined.ErrorOutline
    }
