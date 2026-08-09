package com.dbcheck.app.ui.meter.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CircularGaugeGeometryTest {
    @Test
    fun scaleLabelsUseSharedSoundLevelDisplayCoordinates() {
        assertEquals(135f, gaugeAngleForDb(0f), 0.001f)
        assertEquals(218.077f, gaugeAngleForDb(40f), 0.001f)
        assertEquals(301.154f, gaugeAngleForDb(80f), 0.001f)
        assertEquals(384.231f, gaugeAngleForDb(120f), 0.001f)
    }

    @Test
    fun labelPositionsFollowTheGaugeArcAroundItsCenter() {
        val center = Offset(100f, 100f)
        val radius = 80f
        val positions = listOf(0f, 40f, 80f, 120f).map { gaugeLabelPosition(it, center, radius) }

        assertTrue(positions[0].x < center.x && positions[0].y > center.y)
        assertTrue(positions[1].x < center.x && positions[1].y < center.y)
        assertTrue(positions[2].x > center.x && positions[2].y < center.y)
        assertTrue(positions[3].x > center.x && positions[3].y > center.y)
        positions.forEach { position ->
            val dx = position.x - center.x
            val dy = position.y - center.y
            assertEquals(radius, kotlin.math.sqrt(dx * dx + dy * dy), 0.001f)
        }
    }

    @Test
    fun idleCopyWidthStaysInsideTheCompactGaugeInnerArc() {
        assertEquals(
            108.dp,
            gaugeInnerContentDiameter(
                gaugeSize = 176.dp,
                scaleInset = 22.dp,
                strokeWidth = 12.dp,
            ),
        )
    }
}
