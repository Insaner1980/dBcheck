@file:Suppress("MatchingDeclarationName")

package com.dbcheck.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object DbCheckRadii {
    val Card: Dp = 24.dp
    val Tile: Dp = 16.dp
    val Row: Dp = 12.dp
    val ModalSheet: Dp = 28.dp
}

val DbCheckShapes =
    Shapes(
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(DbCheckRadii.Row),
        large = RoundedCornerShape(DbCheckRadii.Tile),
        extraLarge = RoundedCornerShape(DbCheckRadii.Card),
    )

val RoundedXxl = RoundedCornerShape(DbCheckRadii.ModalSheet)
