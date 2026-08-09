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
import com.dbcheck.app.domain.noise.NoiseLevel

@Immutable
data class NoiseLevelColors(
    val quiet: Color,
    val normal: Color,
    val elevated: Color,
    val dangerous: Color,
    val content: Color,
) {
    fun colorFor(level: NoiseLevel): Color = when (level) {
        NoiseLevel.QUIET -> quiet
        NoiseLevel.NORMAL -> normal
        NoiseLevel.ELEVATED -> elevated
        NoiseLevel.DANGEROUS -> dangerous
    }

    fun contentColorFor(level: NoiseLevel): Color = when (level) {
        NoiseLevel.QUIET,
        NoiseLevel.NORMAL,
        NoiseLevel.ELEVATED,
        NoiseLevel.DANGEROUS,
        -> content
    }
}

@Immutable
data class DbCheckColorScheme(
    val material: ColorScheme,
    val warning: Color,
    val success: Color,
    val accent: Color,
    val accentDim: Color,
    val onAccent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    val noiseLevels: NoiseLevelColors,
    val surfaceContainerLowest: Color,
    val tertiaryFixedDim: Color,
    val signatureGradient: Brush,
    val ghostBorder: Color,
)

val LocalDbCheckColorScheme =
    staticCompositionLocalOf<DbCheckColorScheme> {
        error("No DbCheckColorScheme provided")
    }

val LocalDbCheckTypography = staticCompositionLocalOf { DbCheckTypography() }

private fun darkDbCheckColorScheme() = DbCheckColorScheme(
        material =
            darkColorScheme(
                background = DarkBackground,
                surface = DarkSurface,
                surfaceContainer = DarkSurfaceContainer,
                surfaceContainerHigh = DarkSurfaceContainerHigh,
                surfaceContainerHighest = DarkSurfaceContainerHighest,
                onSurface = DarkOnSurface,
                onSurfaceVariant = DarkOnSurfaceVariant,
                primary = DarkAccent,
                primaryContainer = DarkAccentContainer,
                onPrimaryContainer = DarkOnAccentContainer,
                secondary = DarkSecondary,
                tertiary = DarkTertiary,
                outlineVariant = DarkOutlineVariant.copy(alpha = 0.15f),
                error = DarkError,
                onPrimary = DarkOnAccent,
                onSecondary = DarkOnAccent,
                onBackground = DarkOnSurface,
            ),
        warning = DarkWarning,
        success = DarkSuccess,
        accent = DarkAccent,
        accentDim = DarkAccentDim,
        onAccent = DarkOnAccent,
        accentContainer = DarkAccentContainer,
        onAccentContainer = DarkOnAccentContainer,
        noiseLevels =
            NoiseLevelColors(
                quiet = DarkLevelQuiet,
                normal = DarkLevelNormal,
                elevated = DarkLevelElevated,
                dangerous = DarkLevelDangerous,
                content = DarkLevelContent,
            ),
        surfaceContainerLowest = DarkSurfaceContainerLowest,
        tertiaryFixedDim = DarkTertiaryFixedDim,
        signatureGradient =
            Brush.linearGradient(
                colors = listOf(DarkGaugeGradientStart, DarkGaugeGradientEnd),
            ),
        ghostBorder = DarkOutlineVariant.copy(alpha = 0.15f),
    )

private fun lightDbCheckColorScheme() = DbCheckColorScheme(
        material =
            lightColorScheme(
                background = LightBackground,
                surface = LightSurface,
                surfaceContainer = LightSurfaceContainer,
                surfaceContainerHigh = LightSurfaceContainerHigh,
                surfaceContainerHighest = LightSurfaceContainerHighest,
                onSurface = LightOnSurface,
                onSurfaceVariant = LightOnSurfaceVariant,
                primary = LightAccent,
                primaryContainer = LightAccentContainer,
                onPrimaryContainer = LightOnAccentContainer,
                secondary = LightSecondary,
                tertiary = LightTertiary,
                outlineVariant = LightOutlineVariant.copy(alpha = 0.20f),
                error = LightError,
                onPrimary = LightOnAccent,
                onSecondary = LightSurfaceContainerLowest,
                onBackground = LightOnSurface,
            ),
        warning = LightWarning,
        success = LightSuccess,
        accent = LightAccent,
        accentDim = LightAccentDim,
        onAccent = LightOnAccent,
        accentContainer = LightAccentContainer,
        onAccentContainer = LightOnAccentContainer,
        noiseLevels =
            NoiseLevelColors(
                quiet = LightLevelQuiet,
                normal = LightLevelNormal,
                elevated = LightLevelElevated,
                dangerous = LightLevelDangerous,
                content = LightLevelContent,
            ),
        surfaceContainerLowest = LightSurfaceContainerLowest,
        tertiaryFixedDim = LightTertiaryFixedDim,
        signatureGradient =
            Brush.linearGradient(
                colors = listOf(LightGaugeGradientStart, LightGaugeGradientEnd),
            ),
        ghostBorder = LightOutlineVariant.copy(alpha = 0.20f),
    )

private fun materialTypography() = Typography(
        displayLarge =
            TextStyle(
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 56.sp,
                letterSpacing = (-0.02).sp,
            ),
        displayMedium =
            TextStyle(
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp,
                letterSpacing = (-0.02).sp,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 32.sp,
                letterSpacing = (-0.01).sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                letterSpacing = 0.05.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                letterSpacing = 0.08.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                letterSpacing = 0.05.sp,
            ),
    )

@Composable
fun DbCheckTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
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
