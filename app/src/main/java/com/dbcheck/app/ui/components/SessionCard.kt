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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.dbcheck.app.R
import com.dbcheck.app.domain.noise.NoiseAlertPolicy
import com.dbcheck.app.ui.theme.DbCheckRadii
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun SessionCard(
    state: SessionCardState,
    modifier: Modifier = Modifier,
    editAction: SessionCardEditAction? = null,
    onClick: () -> Unit = {},
) {
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing
    val typography = DbCheckTheme.typography

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DbCheckRadii.Card))
                .clickable(role = Role.Button, onClick = onClick)
                .background(colors.material.surfaceContainerHigh)
                .padding(spacing.cardPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.space4),
    ) {
        Box(
            modifier =
                Modifier
                    .size(spacing.iconCircle)
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
            isSleepSession = state.isSleepSession,
            modifier = Modifier.weight(1f),
        )

        SessionCardStats(peakDb = state.peakDb, avgDb = state.avgDb)

        if (editAction != null) {
            IconButton(onClick = editAction.onClick, modifier = Modifier.size(spacing.iconCircle)) {
                Icon(
                    imageVector = if (editAction.isLocked) Icons.Outlined.Lock else Icons.Outlined.Edit,
                    contentDescription =
                        if (editAction.isLocked) {
                            stringResource(R.string.session_unlock_naming_content_description)
                        } else {
                            stringResource(R.string.session_edit_content_description)
                        },
                    tint = colors.material.onSurfaceVariant,
                    modifier = Modifier.size(spacing.space5),
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
    isSleepSession: Boolean,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = metadata.uppercase(),
                style = typography.labelSm,
                color = colors.material.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (isSleepSession) {
                SleepSessionBadge()
            }
            tags.take(2).forEach { tag ->
                SessionTagText(tag, modifier = Modifier.weight(1f, fill = false))
            }
        }
    }
}

@Composable
private fun SleepSessionBadge() {
    Text(
        text = stringResource(R.string.session_badge_sleep),
        style = DbCheckTheme.typography.labelSm,
        color = DbCheckTheme.colorScheme.material.primary,
        modifier =
            Modifier
                .clip(RoundedCornerShape(DbCheckRadii.Row))
                .background(DbCheckTheme.colorScheme.material.primary.copy(alpha = 0.14f))
                .padding(horizontal = DbCheckTheme.spacing.space2, vertical = DbCheckTheme.spacing.space1),
    )
}

@Composable
private fun SessionTagText(tag: String, modifier: Modifier = Modifier) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    Text(
        text = "#$tag",
        style = typography.labelSm,
        color = colors.material.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
private fun SessionCardStats(peakDb: Float, avgDb: Float) {
    val peakIsWarning = peakDb >= NoiseAlertPolicy.PEAK_WARNING_DB
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space1),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space4)) {
            StatValue(
                label = stringResource(R.string.session_stat_peak),
                value = peakDb.toInt().toString(),
                tone = if (peakIsWarning) SessionStatTone.Warning else SessionStatTone.Default,
            )
            StatValue(
                label = stringResource(R.string.session_stat_avg),
                value = avgDb.toInt().toString(),
                tone = if (peakIsWarning) SessionStatTone.Muted else SessionStatTone.Default,
            )
        }
    }
}

@Composable
private fun StatValue(label: String, value: String, tone: SessionStatTone) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = value,
            style = typography.dataLg,
            color =
                when (tone) {
                    SessionStatTone.Default -> colors.material.onSurface
                    SessionStatTone.Warning -> colors.warning
                    SessionStatTone.Muted -> colors.material.onSurfaceVariant
                },
        )
        Text(
            text = label,
            style = typography.labelMd,
            color = colors.material.onSurfaceVariant,
        )
    }
}

private enum class SessionStatTone {
    Default,
    Warning,
    Muted,
}
