package com.dbcheck.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class AmbientShadow(
    val offsetY: Dp = 12.dp,
    val blur: Dp = 24.dp,
    val color: Color,
)

@Composable
fun ambientShadow(): AmbientShadow {
    val colors = DbCheckTheme.colorScheme
    return AmbientShadow(
        color = colors.primaryDim.copy(alpha = 0.04f),
    )
}
