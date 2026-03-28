package com.dbcheck.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dbcheck.app.R

val ManropeFamily =
    FontFamily(
        Font(R.font.manrope_regular, FontWeight.Normal),
        Font(R.font.manrope_medium, FontWeight.Medium),
        Font(R.font.manrope_semibold, FontWeight.SemiBold),
        Font(R.font.manrope_bold, FontWeight.Bold),
    )

val SpaceGroteskFamily =
    FontFamily(
        Font(R.font.space_grotesk_regular, FontWeight.Normal),
        Font(R.font.space_grotesk_medium, FontWeight.Medium),
        Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
        Font(R.font.space_grotesk_bold, FontWeight.Bold),
    )

@Immutable
data class DbCheckTypography(
    val displayLg: TextStyle =
        TextStyle(
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 56.sp,
            lineHeight = (56 * 1.3).sp,
            letterSpacing = (-0.02).sp,
        ),
    val displayMd: TextStyle =
        TextStyle(
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 44.sp,
            lineHeight = (44 * 1.3).sp,
            letterSpacing = (-0.02).sp,
        ),
    val headlineLg: TextStyle =
        TextStyle(
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            lineHeight = (32 * 1.3).sp,
            letterSpacing = (-0.01).sp,
        ),
    val headlineMd: TextStyle =
        TextStyle(
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = (24 * 1.3).sp,
            letterSpacing = 0.sp,
        ),
    val bodyLg: TextStyle =
        TextStyle(
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = (16 * 1.5).sp,
            letterSpacing = 0.sp,
        ),
    val bodyMd: TextStyle =
        TextStyle(
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = (14 * 1.5).sp,
            letterSpacing = 0.sp,
        ),
    val labelLg: TextStyle =
        TextStyle(
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = (14 * 1.2).sp,
            letterSpacing = 0.05.sp,
        ),
    val labelMd: TextStyle =
        TextStyle(
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = (12 * 1.2).sp,
            letterSpacing = 0.08.sp,
        ),
    val labelSm: TextStyle =
        TextStyle(
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            lineHeight = (11 * 1.2).sp,
            letterSpacing = 0.05.sp,
        ),
    val dataXl: TextStyle =
        TextStyle(
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            lineHeight = (32 * 1.2).sp,
            letterSpacing = (-0.01).sp,
        ),
    val dataLg: TextStyle =
        TextStyle(
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = (24 * 1.2).sp,
            letterSpacing = 0.sp,
        ),
    val dataMd: TextStyle =
        TextStyle(
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = (16 * 1.2).sp,
            letterSpacing = 0.sp,
        ),
)
