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
    const val WORDMARK = ProductIdentity.FILE_NAME_PREFIX
    const val SHARE_CARD_MARGIN_PX = 80f
    const val SHARE_CARD_PANEL_RADIUS_PX = 24f
    const val FONT_MANROPE = "Manrope"
    const val FONT_SPACE_GROTESK = "Space Grotesk"

    fun noiseLevelColor(level: NoiseLevel): Color {
        return when (level) {
            NoiseLevel.QUIET -> Color(0xFF8EA58E)
            NoiseLevel.NORMAL -> Color(0xFF5C6060)
            NoiseLevel.ELEVATED -> Color(0xFFC9A24D)
            NoiseLevel.DANGEROUS -> Color(0xFFE07A7A)
        }
    }

    fun noiseLevelArgb(level: NoiseLevel): Int = noiseLevelColor(level).toArgb()

    fun manropePaint(
        context: Context,
        color: Int,
        textSize: Float,
        semibold: Boolean = false,
        align: Paint.Align = Paint.Align.LEFT,
    ): Paint {
        return brandPaint(
            color = color,
            textSize = textSize,
            typeface = manropeTypeface(context, semibold),
            align = align,
        )
    }

    fun spaceGroteskPaint(
        context: Context,
        color: Int,
        textSize: Float,
        bold: Boolean = true,
        align: Paint.Align = Paint.Align.LEFT,
    ): Paint {
        return brandPaint(
            color = color,
            textSize = textSize,
            typeface = spaceGroteskTypeface(context, bold),
            align = align,
        )
    }

    private fun manropeTypeface(context: Context, semibold: Boolean): Typeface {
        return bundledFont(
            context = context,
            id = if (semibold) R.font.manrope_semibold else R.font.manrope_regular,
            fallbackFamily = FONT_MANROPE,
            fallbackStyle = if (semibold) Typeface.BOLD else Typeface.NORMAL,
        )
    }

    private fun spaceGroteskTypeface(context: Context, bold: Boolean): Typeface {
        return bundledFont(
            context = context,
            id = if (bold) R.font.space_grotesk_bold else R.font.space_grotesk_semibold,
            fallbackFamily = FONT_SPACE_GROTESK,
            fallbackStyle = Typeface.BOLD,
        )
    }

    private fun bundledFont(context: Context, id: Int, fallbackFamily: String, fallbackStyle: Int): Typeface {
        return runCatching { ResourcesCompat.getFont(context, id) }
            .getOrNull()
            ?: Typeface.create(fallbackFamily, fallbackStyle)
    }

    private fun brandPaint(color: Int, textSize: Float, typeface: Typeface, align: Paint.Align): Paint {
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.textSize = textSize
            this.typeface = typeface
            textAlign = align
        }
    }
}
