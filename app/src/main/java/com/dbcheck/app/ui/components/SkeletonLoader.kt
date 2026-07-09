package com.dbcheck.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dbcheck.app.ui.theme.DbCheckMotion
import com.dbcheck.app.ui.theme.DbCheckRadii
import com.dbcheck.app.ui.theme.DbCheckTheme

data class SkeletonSpec(
    val height: Dp = 120.dp,
    val cornerRadius: Dp = DbCheckRadii.Card,
    val shimmerDurationMillis: Int = DbCheckMotion.Shimmer,
)

@Composable
fun SkeletonLoader(
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
    cornerRadius: Dp = DbCheckRadii.Card,
    spec: SkeletonSpec = SkeletonSpec(height = height, cornerRadius = cornerRadius),
) {
    val colors = DbCheckTheme.colorScheme
    val shimmerColors =
        listOf(
            colors.material.surfaceContainerHigh,
            colors.material.surfaceContainerHighest,
            colors.material.surfaceContainerHigh,
        )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(spec.shimmerDurationMillis, easing = LinearEasing),
            ),
        label = "shimmerTranslate",
    )

    val brush =
        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnimation - 200f, 0f),
            end = Offset(translateAnimation + 200f, 0f),
        )

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(spec.height)
                .clip(RoundedCornerShape(spec.cornerRadius))
                .background(brush),
    )
}
