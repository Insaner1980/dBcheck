package com.dbcheck.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.dbcheck.app.ui.theme.DbCheckTheme
import kotlin.math.sin

@Composable
fun WaveformBackground(
    modifier: Modifier = Modifier,
    alpha: Float = 0.15f,
) {
    val colors = DbCheckTheme.colorScheme
    val waveColor = colors.tertiaryFixedDim.copy(alpha = alpha)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
    ) {
        val width = size.width
        val height = size.height
        val path = Path()
        val midY = height / 2

        path.moveTo(0f, midY)
        for (x in 0..width.toInt() step 2) {
            val xFloat = x.toFloat()
            val y = midY + sin(xFloat * 0.02f) * height * 0.3f +
                sin(xFloat * 0.005f) * height * 0.15f
            path.lineTo(xFloat, y)
        }

        drawPath(
            path = path,
            color = waveColor,
            style = Stroke(width = 2f, cap = StrokeCap.Round),
        )
    }
}
