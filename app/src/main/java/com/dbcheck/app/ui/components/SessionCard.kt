package com.dbcheck.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun SessionCard(
    emoji: String,
    title: String,
    metadata: String,
    peakDb: Float,
    avgDb: Float,
    modifier: Modifier = Modifier,
) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(colors.material.surfaceContainerHigh)
                .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Emoji in tinted circle
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.material.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, style = typography.headlineMd)
        }

        // Title + metadata
        Column(
            modifier = Modifier.weight(1f),
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
        }

        // Stats
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatValue(
                    label = "PEAK",
                    value = peakDb.toInt().toString(),
                    isWarning = peakDb >= 85f,
                )
                StatValue(
                    label = "AVG",
                    value = avgDb.toInt().toString(),
                    isWarning = false,
                )
            }
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
