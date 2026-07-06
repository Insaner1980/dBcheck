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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dbcheck.app.domain.noise.NoiseLevel
import com.dbcheck.app.ui.theme.DbCheckTheme
import com.dbcheck.app.util.labelStringRes

@Composable
fun NoiseLevelPill(noiseLevel: NoiseLevel, modifier: Modifier = Modifier, animationsEnabled: Boolean = true) {
    val colors = DbCheckTheme.colorScheme
    val pillColor =
        when (noiseLevel) {
            NoiseLevel.QUIET -> colors.success
            NoiseLevel.NORMAL -> colors.material.primary
            NoiseLevel.ELEVATED -> colors.warning
            NoiseLevel.DANGEROUS -> colors.material.error
        }
    val contentColor =
        readableContentColorFor(
            background = pillColor,
            first = colors.material.onPrimary,
            second = colors.material.onPrimaryContainer,
        )

    if (animationsEnabled) {
        AnimatedContent(
            targetState = noiseLevel,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "noiseLevelPill",
            modifier = modifier,
        ) { level ->
            NoiseLevelLabel(level = level, pillColor = pillColor, contentColor = contentColor)
        }
    } else {
        NoiseLevelLabel(
            level = noiseLevel,
            pillColor = pillColor,
            contentColor = contentColor,
            modifier = modifier,
        )
    }
}

@Composable
private fun NoiseLevelLabel(level: NoiseLevel, pillColor: Color, contentColor: Color, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(level.labelStringRes()).uppercase(),
        style = DbCheckTheme.typography.labelMd,
        color = contentColor,
        modifier =
            modifier
                .clip(CircleShape)
                .background(pillColor)
                .padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

private fun readableContentColorFor(background: Color, first: Color, second: Color): Color =
    if (contrastRatio(first, background) >= contrastRatio(second, background)) {
        first
    } else {
        second
    }

private fun contrastRatio(foreground: Color, background: Color): Float {
    val foregroundLuminance = foreground.luminance()
    val backgroundLuminance = background.luminance()
    val lighter = maxOf(foregroundLuminance, backgroundLuminance)
    val darker = minOf(foregroundLuminance, backgroundLuminance)
    return (lighter + CONTRAST_OFFSET) / (darker + CONTRAST_OFFSET)
}

private const val CONTRAST_OFFSET = 0.05f
