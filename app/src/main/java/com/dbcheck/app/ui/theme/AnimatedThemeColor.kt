package com.dbcheck.app.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

@Composable
fun animatedThemeColor(targetValue: Color, animationsEnabled: Boolean, label: String): Color {
    if (!animationsEnabled) return targetValue

    val color by animateColorAsState(
        targetValue = targetValue,
        animationSpec = tween(durationMillis = DbCheckMotion.StateChange),
        label = label,
    )
    return color
}
