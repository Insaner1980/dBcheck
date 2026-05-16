package com.dbcheck.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dbcheck.app.ui.theme.DbCheckTheme
import com.dbcheck.app.ui.theme.ManropeFamily

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
    val colors = DbCheckTheme.colorScheme

    when (style) {
        DbCheckButtonStyle.Primary -> {
            Box(
                modifier =
                    modifier
                        .height(height)
                        .clip(CircleShape)
                        .background(
                            brush = colors.signatureGradient,
                            shape = CircleShape,
                        ).alpha(if (isPressed) 0.85f else 1f)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            enabled = enabled,
                            onClick = onClick,
                        ).padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = text,
                    style =
                        DbCheckTheme.typography.bodyLg.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                    color = colors.material.onPrimaryContainer,
                )
            }
        }

        DbCheckButtonStyle.Secondary -> {
            Box(
                modifier =
                    modifier
                        .height(height)
                        .clip(CircleShape)
                        .background(
                            color =
                                if (isPressed) {
                                    colors.material.surfaceContainerHighest.copy(alpha = 0.92f)
                                } else {
                                    colors.material.surfaceContainerHighest
                                },
                            shape = CircleShape,
                        ).clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            enabled = enabled,
                            onClick = onClick,
                        ).padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = text,
                    style = DbCheckTheme.typography.bodyLg,
                    color = colors.material.onSurface,
                )
            }
        }

        DbCheckButtonStyle.Tertiary -> {
            Box(
                modifier =
                    modifier
                        .clip(CircleShape)
                        .background(
                            color =
                                if (isPressed) {
                                    colors.material.primary.copy(alpha = 0.08f)
                                } else {
                                    Color.Transparent
                                },
                        ).clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            enabled = enabled,
                            onClick = onClick,
                        ).padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = text.uppercase(),
                    style = DbCheckTheme.typography.labelLg,
                    color = colors.material.primary,
                )
            }
        }
    }
}
