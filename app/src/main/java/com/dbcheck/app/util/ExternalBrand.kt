package com.dbcheck.app.util

import androidx.compose.ui.graphics.Color
import com.dbcheck.app.domain.noise.NoiseLevel

object ExternalBrand {
    const val Wordmark = "dBcheck"
    const val ShareCardMarginPx = 80f
    const val ShareCardPanelRadiusPx = 24f
    const val FontManrope = "Manrope"
    const val FontSpaceGrotesk = "Space Grotesk"

    fun noiseLevelColor(level: NoiseLevel): Color =
        when (level) {
            NoiseLevel.QUIET -> Color(0xFF8EA58E)
            NoiseLevel.NORMAL -> Color(0xFFF7F7F7)
            NoiseLevel.ELEVATED -> Color(0xFFC9A24D)
            NoiseLevel.DANGEROUS -> Color(0xFFF87171)
        }
}
