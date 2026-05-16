package com.dbcheck.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dbcheck.app.domain.noise.NoiseLevel
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun SessionCard(
    state: SessionCardState,
    modifier: Modifier = Modifier,
    editAction: SessionCardEditAction? = null,
    onClick: () -> Unit = {},
) {
    val colors = DbCheckTheme.colorScheme
    val shapes = DbCheckTheme.shapes
    val spacing = DbCheckTheme.spacing
    val typography = DbCheckTheme.typography

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shapes.extraLarge)
                .clickable(onClick = onClick)
                .background(colors.material.surfaceContainerHigh)
                .padding(spacing.space5),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.space4),
    ) {
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.material.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = state.emoji, style = typography.headlineMd)
        }

        SessionCardText(
            title = state.title,
            metadata = state.metadata,
            tags = state.tags,
            modifier = Modifier.weight(1f),
        )

        SessionCardStats(peakDb = state.peakDb, avgDb = state.avgDb)

        if (editAction != null) {
            IconButton(onClick = editAction.onClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (editAction.isLocked) Icons.Outlined.Lock else Icons.Outlined.Edit,
                    contentDescription = if (editAction.isLocked) "Unlock session naming" else "Edit session",
                    tint = colors.material.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun SessionCardText(
    title: String,
    metadata: String,
    tags: List<String>,
    modifier: Modifier,
) {
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing
    val typography = DbCheckTheme.typography

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.space1),
    ) {
        Text(
            text = title,
            style = typography.bodyLg,
            color = colors.material.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = metadata.uppercase(),
            style = typography.labelSm,
            color = colors.material.onSurfaceVariant,
        )
        if (tags.isNotEmpty()) {
            SessionTagPreview(tags)
        }
    }
}

@Composable
private fun SessionTagPreview(tags: List<String>) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    Row(horizontalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space2)) {
        tags.take(2).forEach { tag ->
            Text(
                text = "#$tag",
                style = typography.labelSm,
                color = colors.material.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SessionCardStats(
    peakDb: Float,
    avgDb: Float,
) {
    val spacing = DbCheckTheme.spacing

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.space4)) {
            StatValue(
                label = "PEAK",
                value = peakDb.toInt().toString(),
                isWarning = peakDb >= NoiseLevel.ELEVATED.maxDb,
            )
            StatValue(
                label = "AVG",
                value = avgDb.toInt().toString(),
                isWarning = false,
            )
        }
    }
}

@Composable
private fun StatValue(
    label: String,
    value: String,
    isWarning: Boolean,
) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = value,
            style = typography.dataLg,
            color =
                when {
                    isWarning -> colors.warning
                    else -> colors.material.onSurface
                },
        )
        Text(
            text = label,
            style = typography.labelMd,
            color = colors.material.onSurfaceVariant,
        )
    }
}
