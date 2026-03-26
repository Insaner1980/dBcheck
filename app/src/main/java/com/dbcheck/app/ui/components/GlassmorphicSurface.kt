package com.dbcheck.app.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun GlassmorphicSurface(
    modifier: Modifier = Modifier,
    shape: Shape = DbCheckTheme.spacing.let { androidx.compose.foundation.shape.RoundedCornerShape(16.dp) },
    opacity: Float = 0.6f,
    blurRadius: Dp = 20.dp,
    color: Color = DbCheckTheme.colorScheme.material.surface,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.blur(blurRadius)
                } else {
                    Modifier
                },
            )
            .background(color.copy(alpha = opacity)),
        content = content,
    )
}
