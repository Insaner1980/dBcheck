package com.dbcheck.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun signatureButtonGradient(): Brush {
    val colors = DbCheckTheme.colorScheme
    val angle = 135.0 * PI / 180.0
    return Brush.linearGradient(
        colors = listOf(colors.material.primary, colors.material.secondary),
        start = Offset(0f, 0f),
        end =
            Offset(
                (cos(angle) * 1000).toFloat(),
                (sin(angle) * 1000).toFloat(),
            ),
    )
}

@Composable
fun signatureSweepGradient(): Brush {
    val colors = DbCheckTheme.colorScheme
    return Brush.sweepGradient(
        colors =
            listOf(
                colors.material.primary,
                colors.material.secondary,
                colors.material.primary,
            ),
    )
}

@Composable
fun signatureVerticalGradient(): Brush {
    val colors = DbCheckTheme.colorScheme
    return Brush.verticalGradient(
        colors = listOf(colors.material.primary, colors.material.secondary),
    )
}
