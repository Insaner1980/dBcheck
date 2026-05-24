package com.dbcheck.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dbcheck.app.ui.theme.DbCheckColorScheme
import com.dbcheck.app.ui.theme.DbCheckTheme

enum class DbCheckButtonStyle {
    Primary,
    Secondary,
    Tertiary,
}

@Composable
fun DbCheckButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: DbCheckButtonStyle = DbCheckButtonStyle.Primary,
    height: Dp = 56.dp,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 32.dp),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val effectiveHeight = if (height < MinTouchTargetSize) MinTouchTargetSize else height
    val colors = DbCheckTheme.colorScheme

    Box(
        modifier =
            modifier
                .dbCheckButtonModifier(
                    colors = colors,
                    style = style,
                    effectiveHeight = effectiveHeight,
                    isPressed = isPressed,
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ).padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = dbCheckButtonText(style, text),
            style = dbCheckButtonTextStyle(style),
            color = dbCheckButtonTextColor(style),
        )
    }
}

private fun Modifier.dbCheckButtonModifier(
    colors: DbCheckColorScheme,
    style: DbCheckButtonStyle,
    effectiveHeight: Dp,
    isPressed: Boolean,
): Modifier = when (style) {
        DbCheckButtonStyle.Primary ->
            this
                .height(effectiveHeight)
                .sizeIn(minWidth = MinTouchTargetSize)
                .clip(CircleShape)
                .background(
                    brush = colors.signatureGradient,
                    shape = CircleShape,
                ).alpha(if (isPressed) 0.85f else 1f)

        DbCheckButtonStyle.Secondary ->
            this
                .height(effectiveHeight)
                .sizeIn(minWidth = MinTouchTargetSize)
                .clip(CircleShape)
                .background(
                    color =
                        if (isPressed) {
                            colors.material.surfaceContainerHighest.copy(alpha = 0.92f)
                        } else {
                            colors.material.surfaceContainerHighest
                        },
                    shape = CircleShape,
                )

        DbCheckButtonStyle.Tertiary ->
            this
                .sizeIn(minWidth = MinTouchTargetSize, minHeight = MinTouchTargetSize)
                .clip(CircleShape)
                .background(
                    color =
                        if (isPressed) {
                            colors.material.primary.copy(alpha = 0.08f)
                        } else {
                            Color.Transparent
                        },
                )
    }

private fun dbCheckButtonText(style: DbCheckButtonStyle, text: String): String =
    if (style == DbCheckButtonStyle.Tertiary) {
        text.uppercase()
    } else {
        text
    }

@Composable
private fun dbCheckButtonTextStyle(style: DbCheckButtonStyle) = when (style) {
        DbCheckButtonStyle.Primary ->
            DbCheckTheme.typography.bodyLg.copy(
                fontWeight = FontWeight.SemiBold,
            )

        DbCheckButtonStyle.Secondary -> DbCheckTheme.typography.bodyLg

        DbCheckButtonStyle.Tertiary -> DbCheckTheme.typography.labelLg
    }

@Composable
private fun dbCheckButtonTextColor(style: DbCheckButtonStyle): Color {
    val colors = DbCheckTheme.colorScheme
    return when (style) {
        DbCheckButtonStyle.Primary -> colors.material.onPrimary
        DbCheckButtonStyle.Secondary -> colors.material.onSurface
        DbCheckButtonStyle.Tertiary -> colors.material.primary
    }
}

private val MinTouchTargetSize = 48.dp
