package com.dbcheck.app.ui.analytics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dbcheck.app.R
import com.dbcheck.app.ui.analytics.state.SoundDetectionChipUiState
import com.dbcheck.app.ui.analytics.state.SoundDetectionUiState
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.ProLockOverlay
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun SoundDetectionCard(
    soundDetectionState: SoundDetectionUiState,
    isLocked: Boolean,
    modifier: Modifier = Modifier,
    onUpgradeClick: () -> Unit = {},
) {
    Box(
        modifier = modifier.height(SOUND_DETECTION_CARD_HEIGHT),
    ) {
        ProLockOverlay(
            isLocked = isLocked,
            onUpgradeClick = onUpgradeClick,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(SOUND_DETECTION_CARD_HEIGHT),
        ) {
            val visibleState =
                if (isLocked) {
                    SoundDetectionUiState.LockedPreview
                } else {
                    soundDetectionState
                }
            SoundDetectionContent(
                visibleState = visibleState,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(SOUND_DETECTION_CARD_HEIGHT),
            )
        }
    }
}

@Composable
private fun SoundDetectionContent(
    visibleState: SoundDetectionUiState,
    modifier: Modifier = Modifier,
) {
    val contentDescription = soundDetectionContentDescription(visibleState)
    DbCheckCard(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics {
                    this.contentDescription = contentDescription
                },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SoundDetectionHeader(visibleState)

            Spacer(Modifier.height(16.dp))

            CurrentSoundBlock(visibleState)

            Spacer(Modifier.height(16.dp))

            SoundDetectionConfidenceMeter(confidencePercentFor(visibleState))

            Spacer(Modifier.height(16.dp))

            RecentDetectionsList(recentDetectionsFor(visibleState))
        }
    }
}

@Composable
private fun SoundDetectionHeader(visibleState: SoundDetectionUiState) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.sound_detection_title),
            style = typography.labelMd,
            color = colors.material.onSurfaceVariant,
        )
        Text(
            text = stringResource(statusLabelResId(visibleState)),
            style = typography.labelMd,
            color = statusColor(visibleState),
        )
    }
}

@Composable
private fun CurrentSoundBlock(visibleState: SoundDetectionUiState) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.sound_detection_current_label),
            style = typography.labelSm,
            color = colors.material.onSurfaceVariant,
        )
        Text(
            text = currentSoundLabel(visibleState),
            style = typography.headlineMd,
            color = currentSoundColor(visibleState),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = currentSoundDescription(visibleState),
            style = typography.bodyMd,
            color = colors.material.onSurfaceVariant,
        )
    }
}

@Composable
private fun SoundDetectionConfidenceMeter(confidencePercent: Int?) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme
    val percentLabel =
        confidencePercent?.let { percent -> "$percent%" }
            ?: stringResource(R.string.value_unavailable)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.sound_detection_confidence),
                style = typography.labelSm,
                color = colors.material.onSurfaceVariant,
            )
            Text(
                text = percentLabel,
                style = typography.labelMd,
                color = colors.material.onSurface,
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.material.surfaceContainerHighest),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(((confidencePercent ?: 0).coerceIn(0, 100)) / 100f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.material.primary),
            )
        }
    }
}

@Composable
private fun RecentDetectionsList(recentDetections: List<SoundDetectionChipUiState>) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.sound_detection_recent),
            style = typography.labelSm,
            color = colors.material.onSurfaceVariant,
        )
        if (recentDetections.isEmpty()) {
            Text(
                text = stringResource(R.string.sound_detection_no_recent),
                style = typography.bodyMd,
                color = colors.material.onSurfaceVariant,
            )
        } else {
            recentDetections.take(MAX_VISIBLE_RECENT_DETECTIONS).forEach { detection ->
                RecentDetectionRow(detection)
            }
        }
    }
}

@Composable
private fun RecentDetectionRow(detection: SoundDetectionChipUiState) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.material.surfaceContainerHigh)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = detection.label,
            style = typography.bodyMd,
            color = colors.material.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${detection.confidencePercent}%",
            style = typography.labelMd,
            color = colors.material.onSurfaceVariant,
        )
    }
}

@Composable
private fun statusColor(visibleState: SoundDetectionUiState): Color {
    val colors = DbCheckTheme.colorScheme
    return when (visibleState) {
        is SoundDetectionUiState.Live -> colors.material.primary
        is SoundDetectionUiState.Error -> colors.material.error
        SoundDetectionUiState.Idle,
        SoundDetectionUiState.LockedPreview,
        -> colors.material.onSurfaceVariant
    }
}

@Composable
private fun currentSoundLabel(visibleState: SoundDetectionUiState): String = when (visibleState) {
    is SoundDetectionUiState.Live -> visibleState.label
    is SoundDetectionUiState.Error -> stringResource(R.string.sound_detection_error_value)
    SoundDetectionUiState.Idle -> stringResource(R.string.sound_detection_idle_value)
    SoundDetectionUiState.LockedPreview -> stringResource(R.string.sound_detection_preview_value)
}

@Composable
private fun currentSoundDescription(visibleState: SoundDetectionUiState): String = when (visibleState) {
    is SoundDetectionUiState.Live -> stringResource(R.string.sound_detection_live_description)
    is SoundDetectionUiState.Error -> visibleState.message
    SoundDetectionUiState.Idle -> stringResource(R.string.sound_detection_idle_description)
    SoundDetectionUiState.LockedPreview -> stringResource(R.string.sound_detection_preview_description)
}

@Composable
private fun currentSoundColor(visibleState: SoundDetectionUiState): Color {
    val colors = DbCheckTheme.colorScheme
    return when (visibleState) {
        is SoundDetectionUiState.Error -> colors.material.error
        else -> colors.material.onSurface
    }
}

private fun statusLabelResId(visibleState: SoundDetectionUiState): Int = when (visibleState) {
    is SoundDetectionUiState.Live -> R.string.sound_detection_status_live
    is SoundDetectionUiState.Error -> R.string.sound_detection_status_error
    SoundDetectionUiState.Idle -> R.string.sound_detection_status_waiting
    SoundDetectionUiState.LockedPreview -> R.string.sound_detection_status_preview
}

private fun confidencePercentFor(visibleState: SoundDetectionUiState): Int? = when (visibleState) {
    is SoundDetectionUiState.Live -> visibleState.confidencePercent
    SoundDetectionUiState.LockedPreview -> LOCKED_PREVIEW_CONFIDENCE
    SoundDetectionUiState.Idle,
    is SoundDetectionUiState.Error,
    -> null
}

private fun recentDetectionsFor(visibleState: SoundDetectionUiState): List<SoundDetectionChipUiState> =
    when (visibleState) {
        is SoundDetectionUiState.Live -> visibleState.recentDetections
        SoundDetectionUiState.LockedPreview -> LOCKED_PREVIEW_RECENT_DETECTIONS
        SoundDetectionUiState.Idle,
        is SoundDetectionUiState.Error,
        -> emptyList()
    }

@Composable
private fun soundDetectionContentDescription(visibleState: SoundDetectionUiState): String =
    when (visibleState) {
        is SoundDetectionUiState.Live ->
            stringResource(
                R.string.a11y_sound_detection_live,
                visibleState.label,
                visibleState.confidencePercent,
            )

        is SoundDetectionUiState.Error ->
            stringResource(R.string.a11y_sound_detection_error, visibleState.message)

        SoundDetectionUiState.Idle -> stringResource(R.string.a11y_sound_detection_idle)

        SoundDetectionUiState.LockedPreview -> stringResource(R.string.a11y_sound_detection_locked)
    }

private val LOCKED_PREVIEW_RECENT_DETECTIONS =
    listOf(
        SoundDetectionChipUiState(label = "Speech", confidencePercent = 82),
        SoundDetectionChipUiState(label = "Music", confidencePercent = 61),
        SoundDetectionChipUiState(label = "Vehicle", confidencePercent = 47),
    )

private const val LOCKED_PREVIEW_CONFIDENCE = 82
private const val MAX_VISIBLE_RECENT_DETECTIONS = 3
private val SOUND_DETECTION_CARD_HEIGHT = 352.dp
