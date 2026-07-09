@file:Suppress("MatchingDeclarationName")

package com.dbcheck.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.dbcheck.app.ui.theme.DbCheckRadii
import com.dbcheck.app.ui.theme.DbCheckSpacing
import com.dbcheck.app.ui.theme.DbCheckTheme

enum class DbCheckCardEmphasis {
    Default,
    Elevated,
}

@Composable
fun DbCheckCard(
    modifier: Modifier = Modifier,
    emphasis: DbCheckCardEmphasis = DbCheckCardEmphasis.Default,
    backgroundColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(DbCheckSpacing().cardPadding),
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = DbCheckTheme.colorScheme
    val containerColor =
        backgroundColor ?: when (emphasis) {
            DbCheckCardEmphasis.Default -> colors.material.surfaceContainer
            DbCheckCardEmphasis.Elevated -> colors.material.surfaceContainerHigh
        }

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(DbCheckRadii.Card))
                .background(containerColor)
                .padding(contentPadding),
        content = content,
    )
}
