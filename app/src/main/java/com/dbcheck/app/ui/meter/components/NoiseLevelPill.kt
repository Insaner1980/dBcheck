package com.dbcheck.app.ui.meter.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dbcheck.app.data.model.NoiseLevel
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun NoiseLevelPill(
    noiseLevel: NoiseLevel,
    modifier: Modifier = Modifier,
) {
    val colors = DbCheckTheme.colorScheme
    val pillColor = when (noiseLevel) {
        NoiseLevel.QUIET -> colors.success
        NoiseLevel.NORMAL -> colors.material.primary
        NoiseLevel.ELEVATED -> colors.warning
        NoiseLevel.DANGEROUS -> colors.material.error
    }

    AnimatedContent(
        targetState = noiseLevel,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "noiseLevelPill",
        modifier = modifier,
    ) { level ->
        Text(
            text = level.label.uppercase(),
            style = DbCheckTheme.typography.labelMd,
            color = colors.material.onPrimaryContainer,
            modifier = Modifier
                .clip(CircleShape)
                .background(pillColor.copy(alpha = 0.9f))
                .padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
}
