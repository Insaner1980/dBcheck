package com.dbcheck.app.ui.meter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dbcheck.app.domain.noise.NoiseLevel
import com.dbcheck.app.ui.theme.DbCheckTheme
import com.dbcheck.app.ui.theme.animatedThemeColor
import com.dbcheck.app.util.labelStringRes

@Composable
fun NoiseLevelPill(noiseLevel: NoiseLevel, modifier: Modifier = Modifier, animationsEnabled: Boolean = true) {
    val colors = DbCheckTheme.colorScheme
    val pillColor =
        animatedThemeColor(
            targetValue = colors.noiseLevels.colorFor(noiseLevel),
            animationsEnabled = animationsEnabled,
            label = "noiseLevelPillColor",
        )
    val contentColor =
        animatedThemeColor(
            targetValue = colors.noiseLevels.contentColorFor(noiseLevel),
            animationsEnabled = animationsEnabled,
            label = "noiseLevelPillContentColor",
        )

    NoiseLevelLabel(
        level = noiseLevel,
        pillColor = pillColor,
        contentColor = contentColor,
        modifier = modifier,
    )
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
