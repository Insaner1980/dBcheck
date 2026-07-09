package com.dbcheck.app.util

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.dbcheck.app.R
import com.dbcheck.app.domain.noise.NoiseLevel

object ExternalBrand {
    const val Wordmark = ProductIdentity.FILE_NAME_PREFIX
    const val ShareCardMarginPx = 80f
    const val ShareCardPanelRadiusPx = 24f
    const val FontManrope = "Manrope"
    const val FontSpaceGrotesk = "Space Grotesk"

    fun noiseLevelColor(level: NoiseLevel): Color =
        when (level) {
            NoiseLevel.QUIET -> Color(0xFF8EA58E)
            NoiseLevel.NORMAL -> Color(0xFF5C6060)
            NoiseLevel.ELEVATED -> Color(0xFFC9A24D)
            NoiseLevel.DANGEROUS -> Color(0xFFE07A7A)
        }

    fun noiseLevelArgb(level: NoiseLevel): Int = noiseLevelColor(level).toArgb()

    fun manropePaint(
        context: Context,
        color: Int,
        textSize: Float,
        semibold: Boolean = false,
        align: Paint.Align = Paint.Align.LEFT,
    ): Paint =
        brandPaint(
            color = color,
            textSize = textSize,
            typeface = manropeTypeface(context, semibold),
            align = align,
        )

    fun spaceGroteskPaint(
        context: Context,
        color: Int,
        textSize: Float,
        bold: Boolean = true,
        align: Paint.Align = Paint.Align.LEFT,
    ): Paint =
        brandPaint(
            color = color,
            textSize = textSize,
            typeface = spaceGroteskTypeface(context, bold),
            align = align,
        )

    private fun manropeTypeface(context: Context, semibold: Boolean): Typeface =
        bundledFont(
            context = context,
            id = if (semibold) R.font.manrope_semibold else R.font.manrope_regular,
            fallbackFamily = FontManrope,
            fallbackStyle = if (semibold) Typeface.BOLD else Typeface.NORMAL,
        )

    private fun spaceGroteskTypeface(context: Context, bold: Boolean): Typeface =
        bundledFont(
            context = context,
            id = if (bold) R.font.space_grotesk_bold else R.font.space_grotesk_semibold,
            fallbackFamily = FontSpaceGrotesk,
            fallbackStyle = Typeface.BOLD,
        )

    private fun bundledFont(context: Context, id: Int, fallbackFamily: String, fallbackStyle: Int): Typeface =
        runCatching { ResourcesCompat.getFont(context, id) }
            .getOrNull()
            ?: Typeface.create(fallbackFamily, fallbackStyle)

    private fun brandPaint(color: Int, textSize: Float, typeface: Typeface, align: Paint.Align): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.textSize = textSize
            this.typeface = typeface
            textAlign = align
        }
}
