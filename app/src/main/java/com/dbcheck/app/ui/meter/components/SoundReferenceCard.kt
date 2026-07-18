package com.dbcheck.app.ui.meter.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dbcheck.app.R
import com.dbcheck.app.domain.noise.SoundReference
import com.dbcheck.app.domain.noise.SoundReferenceMarker
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.theme.DbCheckRadii
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun SoundReferenceCard(
    currentDb: Float,
    markers: List<SoundReferenceMarker>,
    nearestMarker: SoundReferenceMarker,
    currentPosition: Float,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val expandedLabel = stringResource(R.string.a11y_sound_reference_expanded)
    val collapsedLabel = stringResource(R.string.a11y_sound_reference_collapsed)
    val cardDescription =
        stringResource(
            R.string.a11y_sound_reference_card,
            nearestMarker.reference.label,
            nearestMarker.reference.db.toInt(),
            currentDb.toInt(),
            if (expanded) expandedLabel else collapsedLabel,
        )

    DbCheckCard(
        modifier =
            modifier
                .fillMaxWidth()
                .animateContentSize()
                .semantics {
                    contentDescription = cardDescription
                },
        contentPadding = PaddingValues(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SoundReferenceCollapsedRow(
                currentDb = currentDb,
                nearestReference = nearestMarker.reference,
                expanded = expanded,
                onExpandedChange = onExpandedChange,
            )

            if (expanded) {
                SoundReferenceExpandedContent(
                    markers = markers,
                    nearestMarker = nearestMarker,
                    currentPosition = currentPosition,
                )
            }
        }
    }
}

@Composable
private fun SoundReferenceCollapsedRow(
    currentDb: Float,
    nearestReference: SoundReference,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing
    val stateLabel =
        if (expanded) {
            stringResource(R.string.a11y_sound_reference_expanded)
        } else {
            stringResource(R.string.a11y_sound_reference_collapsed)
        }
    val actionDescription =
        if (expanded) {
            stringResource(R.string.a11y_sound_reference_collapse)
        } else {
            stringResource(R.string.a11y_sound_reference_expand)
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = spacing.space12)
                .semantics {
                    this.stateDescription = stateLabel
                }.clickable(
                    role = Role.Button,
                    onClick = { onExpandedChange(!expanded) },
                ).padding(horizontal = spacing.cardPadding, vertical = spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.sound_reference_title),
                style = typography.labelSm,
                color = colors.material.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = nearestReference.label,
                    style = typography.bodyMd,
                    color = colors.material.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(spacing.space2))
                Text(
                    text = stringResource(R.string.sound_reference_db_short, nearestReference.db.toInt()),
                    style = typography.labelMd,
                    color = colors.material.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.width(spacing.space3))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stringResource(R.string.sound_reference_current_label),
                style = typography.labelSm,
                color = colors.material.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.sound_reference_current_db, currentDb.toInt()),
                style = typography.dataMd,
                color = colors.material.primary,
            )
        }

        Spacer(Modifier.width(spacing.space2))

        Icon(
            imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = actionDescription,
            tint = colors.material.onSurfaceVariant,
        )
    }
}

@Composable
private fun SoundReferenceExpandedContent(
    markers: List<SoundReferenceMarker>,
    nearestMarker: SoundReferenceMarker,
    currentPosition: Float,
) {
    val spacing = DbCheckTheme.spacing

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = spacing.cardPadding,
                    end = spacing.cardPadding,
                    bottom = spacing.cardPadding,
                ),
    ) {
        Spacer(Modifier.height(spacing.space2))
        SoundReferenceRail(
            currentPosition = currentPosition,
            nearestPosition = nearestMarker.position,
        )
        Spacer(Modifier.height(spacing.space4))
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
            markers.forEach { marker ->
                SoundReferenceRow(
                    marker = marker,
                    isNearest = marker.reference.id == nearestMarker.reference.id,
                )
            }
        }
    }
}

@Composable
private fun SoundReferenceRail(currentPosition: Float, nearestPosition: Float) {
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing
    val trackColor = colors.material.outlineVariant
    val referenceColor = colors.warning
    val currentColor = colors.material.primary

    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(spacing.space6),
    ) {
        val y = size.height / 2f
        val clampedCurrent = currentPosition.coerceIn(0f, 1f)
        val clampedNearest = nearestPosition.coerceIn(0f, 1f)

        drawLine(
            color = trackColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = referenceColor,
            radius = 5.dp.toPx(),
            center = Offset(clampedNearest * size.width, y),
        )
        drawCircle(
            color = currentColor,
            radius = 7.dp.toPx(),
            center = Offset(clampedCurrent * size.width, y),
        )
    }
}

@Composable
private fun SoundReferenceRow(marker: SoundReferenceMarker, isNearest: Boolean) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val spacing = DbCheckTheme.spacing
    val backgroundColor =
        if (isNearest) {
            colors.material.primaryContainer
        } else {
            colors.material.surfaceContainerHighest
        }
    val foregroundColor =
        if (isNearest) {
            colors.material.onPrimaryContainer
        } else {
            colors.material.onSurface
        }
    val secondaryColor =
        if (isNearest) {
            colors.material.onPrimaryContainer.copy(alpha = 0.74f)
        } else {
            colors.material.onSurfaceVariant
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = spacing.space12)
                .clip(RoundedCornerShape(DbCheckRadii.Row))
                .background(backgroundColor)
                .padding(horizontal = spacing.space3, vertical = spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isNearest) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = foregroundColor,
                    modifier = Modifier.size(spacing.space5),
                )
                Spacer(Modifier.width(spacing.space2))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = marker.reference.label,
                    style = typography.bodyMd,
                    color = foregroundColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = referenceRangeLabel(marker.reference),
                    style = typography.labelSm,
                    color = secondaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.width(spacing.space3))

        if (isNearest) {
            ClosestBadge()
            Spacer(Modifier.width(spacing.space3))
        }

        Text(
            text = stringResource(R.string.sound_reference_db_short, marker.reference.db.toInt()),
            style = typography.dataMd,
            color = foregroundColor,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun ClosestBadge() {
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing

    Row(
        modifier =
            Modifier
                .clip(CircleShape)
                .background(colors.material.surface.copy(alpha = 0.32f))
                .padding(horizontal = spacing.space2, vertical = spacing.space1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.sound_reference_closest_badge),
            style = DbCheckTheme.typography.labelSm,
            color = colors.material.onPrimaryContainer,
            maxLines = 1,
        )
    }
}

@Composable
private fun referenceRangeLabel(reference: SoundReference): String =
    if (reference.sourceMinDb == reference.sourceMaxDb) {
        stringResource(R.string.sound_reference_db_reference, reference.db.toInt())
    } else {
        stringResource(
            R.string.sound_reference_db_range,
            reference.sourceMinDb.toInt(),
            reference.sourceMaxDb.toInt(),
        )
    }
