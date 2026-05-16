package com.dbcheck.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dbcheck.app.R

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

val LocalDbCheckColorScheme =
    staticCompositionLocalOf<DbCheckColorScheme> {
        error("No DbCheckColorScheme provided")
    }

val LocalDbCheckTypography = staticCompositionLocalOf { DbCheckTypography() }

private data class DbCheckColorResourceSet(
    val background: Int,
    val surface: Int,
    val surfaceContainer: Int,
    val surfaceContainerHigh: Int,
    val surfaceContainerHighest: Int,
    val surfaceContainerLowest: Int,
    val onSurface: Int,
    val onSurfaceVariant: Int,
    val primary: Int,
    val primaryDim: Int,
    val primaryContainer: Int,
    val onPrimaryContainer: Int,
    val secondary: Int,
    val tertiary: Int,
    val tertiaryFixedDim: Int,
    val outlineVariant: Int,
    val error: Int,
    val warning: Int,
    val success: Int,
)

private data class ResolvedDbCheckColors(
    val background: Color,
    val surface: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val surfaceContainerLowest: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val primary: Color,
    val primaryDim: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val tertiary: Color,
    val tertiaryFixedDim: Color,
    val outlineVariant: Color,
    val error: Color,
    val warning: Color,
    val success: Color,
)

@Composable
private fun darkDbCheckColorScheme(): DbCheckColorScheme =
    dbCheckColorScheme(
        resources = DarkDbCheckColors,
        darkMaterial = true,
        outlineAlpha = DbCheckOpacity.OUTLINE_DARK,
        onPrimary = { it.onPrimaryContainer },
    )

@Composable
private fun lightDbCheckColorScheme(): DbCheckColorScheme =
    dbCheckColorScheme(
        resources = LightDbCheckColors,
        darkMaterial = false,
        outlineAlpha = DbCheckOpacity.OUTLINE_LIGHT,
        onPrimary = { it.surfaceContainerLowest },
    )

@Composable
private fun dbCheckColorScheme(
    resources: DbCheckColorResourceSet,
    darkMaterial: Boolean,
    outlineAlpha: Float,
    onPrimary: (ResolvedDbCheckColors) -> Color,
): DbCheckColorScheme {
    val colors = resources.resolve()
    val outline = colors.outlineVariant.copy(alpha = outlineAlpha)
    val primaryForeground = onPrimary(colors)

    return DbCheckColorScheme(
        material =
            baseMaterialColorScheme(darkMaterial).copy(
                background = colors.background,
                surface = colors.surface,
                surfaceContainer = colors.surfaceContainer,
                surfaceContainerHigh = colors.surfaceContainerHigh,
                surfaceContainerHighest = colors.surfaceContainerHighest,
                onSurface = colors.onSurface,
                onSurfaceVariant = colors.onSurfaceVariant,
                primary = colors.primary,
                primaryContainer = colors.primaryContainer,
                onPrimaryContainer = colors.onPrimaryContainer,
                secondary = colors.secondary,
                tertiary = colors.tertiary,
                outlineVariant = outline,
                error = colors.error,
                onPrimary = primaryForeground,
                onSecondary = primaryForeground,
                onBackground = colors.onSurface,
            ),
        warning = colors.warning,
        success = colors.success,
        primaryDim = colors.primaryDim,
        surfaceContainerLowest = colors.surfaceContainerLowest,
        tertiaryFixedDim = colors.tertiaryFixedDim,
        signatureGradient = Brush.linearGradient(colors = listOf(colors.primary, colors.secondary)),
        ghostBorder = outline,
    )
}

private fun baseMaterialColorScheme(darkMaterial: Boolean): ColorScheme =
    if (darkMaterial) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }

@Composable
private fun DbCheckColorResourceSet.resolve(): ResolvedDbCheckColors =
    ResolvedDbCheckColors(
        background = colorResource(background),
        surface = colorResource(surface),
        surfaceContainer = colorResource(surfaceContainer),
        surfaceContainerHigh = colorResource(surfaceContainerHigh),
        surfaceContainerHighest = colorResource(surfaceContainerHighest),
        surfaceContainerLowest = colorResource(surfaceContainerLowest),
        onSurface = colorResource(onSurface),
        onSurfaceVariant = colorResource(onSurfaceVariant),
        primary = colorResource(primary),
        primaryDim = colorResource(primaryDim),
        primaryContainer = colorResource(primaryContainer),
        onPrimaryContainer = colorResource(onPrimaryContainer),
        secondary = colorResource(secondary),
        tertiary = colorResource(tertiary),
        tertiaryFixedDim = colorResource(tertiaryFixedDim),
        outlineVariant = colorResource(outlineVariant),
        error = colorResource(error),
        warning = colorResource(warning),
        success = colorResource(success),
    )

private val DarkDbCheckColors =
    DbCheckColorResourceSet(
        background = R.color.dbcheck_dark_background,
        surface = R.color.dbcheck_dark_surface,
        surfaceContainer = R.color.dbcheck_dark_surface_container,
        surfaceContainerHigh = R.color.dbcheck_dark_surface_container_high,
        surfaceContainerHighest = R.color.dbcheck_dark_surface_container_highest,
        surfaceContainerLowest = R.color.dbcheck_dark_surface_container_lowest,
        onSurface = R.color.dbcheck_dark_on_surface,
        onSurfaceVariant = R.color.dbcheck_dark_on_surface_variant,
        primary = R.color.dbcheck_dark_primary,
        primaryDim = R.color.dbcheck_dark_primary_dim,
        primaryContainer = R.color.dbcheck_dark_primary_container,
        onPrimaryContainer = R.color.dbcheck_dark_on_primary_container,
        secondary = R.color.dbcheck_dark_secondary,
        tertiary = R.color.dbcheck_dark_tertiary,
        tertiaryFixedDim = R.color.dbcheck_dark_tertiary_fixed_dim,
        outlineVariant = R.color.dbcheck_dark_outline_variant,
        error = R.color.dbcheck_dark_error,
        warning = R.color.dbcheck_dark_warning,
        success = R.color.dbcheck_dark_success,
    )

private val LightDbCheckColors =
    DbCheckColorResourceSet(
        background = R.color.dbcheck_light_background,
        surface = R.color.dbcheck_light_surface,
        surfaceContainer = R.color.dbcheck_light_surface_container,
        surfaceContainerHigh = R.color.dbcheck_light_surface_container_high,
        surfaceContainerHighest = R.color.dbcheck_light_surface_container_highest,
        surfaceContainerLowest = R.color.dbcheck_light_surface_container_lowest,
        onSurface = R.color.dbcheck_light_on_surface,
        onSurfaceVariant = R.color.dbcheck_light_on_surface_variant,
        primary = R.color.dbcheck_light_primary,
        primaryDim = R.color.dbcheck_light_primary_dim,
        primaryContainer = R.color.dbcheck_light_primary_container,
        onPrimaryContainer = R.color.dbcheck_light_on_primary_container,
        secondary = R.color.dbcheck_light_secondary,
        tertiary = R.color.dbcheck_light_tertiary,
        tertiaryFixedDim = R.color.dbcheck_light_tertiary_fixed_dim,
        outlineVariant = R.color.dbcheck_light_outline_variant,
        error = R.color.dbcheck_light_error,
        warning = R.color.dbcheck_light_warning,
        success = R.color.dbcheck_light_success,
    )

private fun materialTypography() =
    Typography(
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
    val shapes: Shapes
        @Composable get() = MaterialTheme.shapes
}
