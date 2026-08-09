@file:Suppress("MatchingDeclarationName")

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
    val effectiveHeight = if (height < MIN_TOUCH_TARGET_SIZE) MIN_TOUCH_TARGET_SIZE else height
    val colors = DbCheckTheme.colorScheme
    val visuals = dbCheckButtonVisuals(colors, style, enabled, isPressed)

    Box(
        modifier =
            modifier
                .dbCheckButtonModifier(
                    style = style,
                    effectiveHeight = effectiveHeight,
                    containerColor = visuals.containerColor,
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
            text = text,
            style = dbCheckButtonTextStyle(style),
            color = visuals.contentColor,
        )
    }
}

private data class DbCheckButtonVisuals(val containerColor: Color, val contentColor: Color)

private fun dbCheckButtonVisuals(
    colors: DbCheckColorScheme,
    style: DbCheckButtonStyle,
    enabled: Boolean,
    isPressed: Boolean,
): DbCheckButtonVisuals = when (style) {
    DbCheckButtonStyle.Primary -> primaryButtonVisuals(colors, enabled, isPressed)
    DbCheckButtonStyle.Secondary -> secondaryButtonVisuals(colors, enabled, isPressed)
    DbCheckButtonStyle.Tertiary -> tertiaryButtonVisuals(colors, enabled, isPressed)
}

private fun primaryButtonVisuals(
    colors: DbCheckColorScheme,
    enabled: Boolean,
    isPressed: Boolean,
): DbCheckButtonVisuals = DbCheckButtonVisuals(
        containerColor =
            when {
                !enabled -> colors.material.surfaceContainerHighest
                isPressed -> colors.accentDim
                else -> colors.accent
            },
        contentColor = if (enabled) colors.onAccent else colors.material.onSurfaceVariant,
    )

private fun secondaryButtonVisuals(
    colors: DbCheckColorScheme,
    enabled: Boolean,
    isPressed: Boolean,
): DbCheckButtonVisuals = DbCheckButtonVisuals(
        containerColor =
            if (isPressed && enabled) {
                colors.material.surfaceContainerHigh
            } else {
                colors.material.surfaceContainerHighest
            },
        contentColor = if (enabled) colors.material.onSurface else colors.material.onSurfaceVariant,
    )

private fun tertiaryButtonVisuals(
    colors: DbCheckColorScheme,
    enabled: Boolean,
    isPressed: Boolean,
): DbCheckButtonVisuals = DbCheckButtonVisuals(
        containerColor =
            if (isPressed && enabled) {
                colors.material.onSurface.copy(alpha = TERTIARY_PRESSED_STATE_ALPHA)
            } else {
                Color.Transparent
            },
        contentColor = if (enabled) colors.material.onSurface else colors.material.onSurfaceVariant,
    )

private fun Modifier.dbCheckButtonModifier(
    style: DbCheckButtonStyle,
    effectiveHeight: Dp,
    containerColor: Color,
): Modifier = when (style) {
        DbCheckButtonStyle.Primary,
        DbCheckButtonStyle.Secondary,
        ->
            this
                .height(effectiveHeight)
                .sizeIn(minWidth = MIN_TOUCH_TARGET_SIZE)
                .clip(CircleShape)
                .background(
                    color = containerColor,
                    shape = CircleShape,
                )

        DbCheckButtonStyle.Tertiary ->
            this
                .sizeIn(minWidth = MIN_TOUCH_TARGET_SIZE, minHeight = MIN_TOUCH_TARGET_SIZE)
                .clip(CircleShape)
                .background(color = containerColor)
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

private val MIN_TOUCH_TARGET_SIZE = 48.dp
private const val TERTIARY_PRESSED_STATE_ALPHA = 0.08f
