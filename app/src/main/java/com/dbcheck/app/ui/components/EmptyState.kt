@file:Suppress("MatchingDeclarationName")

package com.dbcheck.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dbcheck.app.ui.theme.DbCheckTheme

enum class EmptyStateSize {
    Default,
    Compact,
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    ctaText: String,
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: EmptyStateSize = EmptyStateSize.Default,
) {
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing
    val horizontalPadding =
        when (size) {
            EmptyStateSize.Default -> spacing.space12
            EmptyStateSize.Compact -> spacing.space6
        }
    val iconSize =
        when (size) {
            EmptyStateSize.Default -> spacing.stateIcon
            EmptyStateSize.Compact -> spacing.iconCircle
        }
    val titleStyle =
        when (size) {
            EmptyStateSize.Default -> DbCheckTheme.typography.headlineMd
            EmptyStateSize.Compact -> DbCheckTheme.typography.bodyLg
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.material.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(iconSize),
        )
        Spacer(Modifier.height(spacing.space6))
        Text(
            text = title,
            style = titleStyle,
            color = colors.material.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.space2))
        Text(
            text = description,
            style = DbCheckTheme.typography.bodyMd,
            color = colors.material.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.space6))
        DbCheckButton(
            text = ctaText,
            onClick = onCtaClick,
            height = 48.dp,
        )
    }
}
