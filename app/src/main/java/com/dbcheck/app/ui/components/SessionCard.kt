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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dbcheck.app.R
import com.dbcheck.app.domain.noise.NoiseAlertPolicy
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun SessionCard(
    state: SessionCardState,
    modifier: Modifier = Modifier,
    editAction: SessionCardEditAction? = null,
    onClick: () -> Unit = {},
) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .clickable(onClick = onClick)
                .background(colors.material.surfaceContainerHigh)
                .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
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
                    contentDescription =
                        if (editAction.isLocked) {
                            stringResource(R.string.session_unlock_naming_content_description)
                        } else {
                            stringResource(R.string.session_edit_content_description)
                        },
                    tint = colors.material.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun SessionCardText(title: String, metadata: String, tags: List<String>, modifier: Modifier) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
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

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
private fun SessionCardStats(peakDb: Float, avgDb: Float) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatValue(
                label = stringResource(R.string.session_stat_peak),
                value = peakDb.toInt().toString(),
                isWarning = peakDb >= NoiseAlertPolicy.PEAK_WARNING_DB,
            )
            StatValue(
                label = stringResource(R.string.session_stat_avg),
                value = avgDb.toInt().toString(),
                isWarning = false,
            )
        }
    }
}

@Composable
private fun StatValue(label: String, value: String, isWarning: Boolean) {
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
