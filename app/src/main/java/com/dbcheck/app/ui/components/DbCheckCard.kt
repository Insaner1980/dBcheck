package com.dbcheck.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun DbCheckCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = DbCheckTheme.colorScheme.material.surfaceContainerHigh,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(24.dp))
                .background(backgroundColor)
                .padding(20.dp),
        content = content,
    )
}
