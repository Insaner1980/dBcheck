@file:Suppress("MatchingDeclarationName")

package com.dbcheck.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class DbCheckSpacing(
    val space1: Dp = 4.dp,
    val space2: Dp = 8.dp,
    val space3: Dp = 12.dp,
    val space4: Dp = 16.dp,
    val space5: Dp = 20.dp,
    val space6: Dp = 24.dp,
    val space8: Dp = 32.dp,
    val space10: Dp = 40.dp,
    val space12: Dp = 48.dp,
    val space16: Dp = 64.dp,
    val pageMargin: Dp = 20.dp,
    val groupGap: Dp = 12.dp,
    val sectionGap: Dp = 32.dp,
    val cardPadding: Dp = 20.dp,
    val heroPadding: Dp = 24.dp,
    val tilePadding: Dp = 16.dp,
    val iconCircle: Dp = 48.dp,
    val stateIcon: Dp = 64.dp,
    val hairline: Dp = 1.dp,
    val meterScrollEdgeFade: Dp = 28.dp,
    val sliderThumbSize: Dp = 20.dp,
    val sliderTrackHeight: Dp = 4.dp,
    val chipHorizontalGap: Dp = 8.dp,
    val chipVerticalGap: Dp = 8.dp,
    val bottomNavIconSize: Dp = 20.dp,
    val bottomNavPillHeight: Dp = 28.dp,
    val bottomNavPillHorizontalPadding: Dp = 12.dp,
    val bottomNavItemVerticalPadding: Dp = 6.dp,
    val bottomNavItemGap: Dp = 2.dp,
    val meterGaugeScaleInset: Dp = 22.dp,
    val meterGaugeLabelOffset: Dp = 16.dp,
    val meterGaugeStrokeWidth: Dp = 12.dp,
)

val LocalDbCheckSpacing = staticCompositionLocalOf { DbCheckSpacing() }
