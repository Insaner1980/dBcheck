package com.dbcheck.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class DbCheckColorScheme(
    val material: ColorScheme,
    val warning: Color,
    val success: Color,
    val primaryDim: Color,
    val surfaceContainerLowest: Color,
    val tertiaryFixedDim: Color,
    val signatureGradient: Brush,
    val ghostBorder: Color,
)

val LocalDbCheckColorScheme = staticCompositionLocalOf<DbCheckColorScheme> {
    error("No DbCheckColorScheme provided")
}

val LocalDbCheckTypography = staticCompositionLocalOf { DbCheckTypography() }

private fun darkDbCheckColorScheme() = DbCheckColorScheme(
    material = darkColorScheme(
        background = DarkBackground,
        surface = DarkSurface,
        surfaceContainer = DarkSurfaceContainer,
        surfaceContainerHigh = DarkSurfaceContainerHigh,
        surfaceContainerHighest = DarkSurfaceContainerHighest,
        onSurface = DarkOnSurface,
        onSurfaceVariant = DarkOnSurfaceVariant,
        primary = DarkPrimary,
        primaryContainer = DarkPrimaryContainer,
        onPrimaryContainer = DarkOnPrimaryContainer,
        secondary = DarkSecondary,
        tertiary = DarkTertiary,
        outlineVariant = DarkOutlineVariant.copy(alpha = 0.15f),
        error = DarkError,
        onPrimary = DarkOnPrimaryContainer,
        onSecondary = DarkOnPrimaryContainer,
        onBackground = DarkOnSurface,
    ),
    warning = DarkWarning,
    success = DarkSuccess,
    primaryDim = DarkPrimaryDim,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    tertiaryFixedDim = DarkTertiaryFixedDim,
    signatureGradient = Brush.linearGradient(
        colors = listOf(DarkPrimary, DarkSecondary),
    ),
    ghostBorder = DarkOutlineVariant.copy(alpha = 0.15f),
)

private fun lightDbCheckColorScheme() = DbCheckColorScheme(
    material = lightColorScheme(
        background = LightBackground,
        surface = LightSurface,
        surfaceContainer = LightSurfaceContainer,
        surfaceContainerHigh = LightSurfaceContainerHigh,
        surfaceContainerHighest = LightSurfaceContainerHighest,
        onSurface = LightOnSurface,
        onSurfaceVariant = LightOnSurfaceVariant,
        primary = LightPrimary,
        primaryContainer = LightPrimaryContainer,
        onPrimaryContainer = LightOnPrimaryContainer,
        secondary = LightSecondary,
        tertiary = LightTertiary,
        outlineVariant = LightOutlineVariant.copy(alpha = 0.20f),
        error = LightError,
        onPrimary = LightSurfaceContainerLowest,
        onSecondary = LightSurfaceContainerLowest,
        onBackground = LightOnSurface,
    ),
    warning = LightWarning,
    success = LightSuccess,
    primaryDim = LightPrimaryDim,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    tertiaryFixedDim = LightTertiaryFixedDim,
    signatureGradient = Brush.linearGradient(
        colors = listOf(LightPrimary, LightSecondary),
    ),
    ghostBorder = LightOutlineVariant.copy(alpha = 0.20f),
)

private fun materialTypography() = Typography(
    displayLarge = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
        letterSpacing = (-0.02).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        letterSpacing = (-0.02).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        letterSpacing = (-0.01).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.05.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.08.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        letterSpacing = 0.05.sp,
    ),
)

@Composable
fun DbCheckTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val dbCheckColors = if (darkTheme) darkDbCheckColorScheme() else lightDbCheckColorScheme()

    CompositionLocalProvider(
        LocalDbCheckColorScheme provides dbCheckColors,
        LocalDbCheckTypography provides DbCheckTypography(),
        LocalDbCheckSpacing provides DbCheckSpacing(),
    ) {
        MaterialTheme(
            colorScheme = dbCheckColors.material,
            typography = materialTypography(),
            shapes = DbCheckShapes,
            content = content,
        )
    }
}

object DbCheckTheme {
    val colorScheme: DbCheckColorScheme
        @Composable get() = LocalDbCheckColorScheme.current
    val typography: DbCheckTypography
        @Composable get() = LocalDbCheckTypography.current
    val spacing: DbCheckSpacing
        @Composable get() = LocalDbCheckSpacing.current
}
