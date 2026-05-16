package com.dbcheck.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun DbCheckCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = DbCheckTheme.colorScheme.material.surfaceContainerHigh,
    content: @Composable BoxScope.() -> Unit,
) {
    val shapes = DbCheckTheme.shapes
    val spacing = DbCheckTheme.spacing

    Box(
        modifier =
            modifier
                .clip(shapes.extraLarge)
                .background(backgroundColor)
                .padding(spacing.space5),
        content = content,
    )
}
