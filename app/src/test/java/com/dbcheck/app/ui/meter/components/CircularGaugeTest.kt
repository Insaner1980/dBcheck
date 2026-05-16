package com.dbcheck.app.ui.meter.components

import org.junit.Assert.assertEquals
import org.junit.Test

class CircularGaugeTest {
    @Test
    fun idleGaugeUsesNeutralBreathingScale() {
        assertEquals(1f, gaugeBreathingScale(isRecording = false, animatedScale = 1.02f), 0f)
    }

    @Test
    fun recordingGaugeUsesAnimatedBreathingScale() {
        assertEquals(1.02f, gaugeBreathingScale(isRecording = true, animatedScale = 1.02f), 0f)
    }

    @Test
    fun gaugeTickUnitsIncludeBothArcEnds() {
        val ticks = gaugeTickUnits(tickCount = 27)

        assertEquals(28, ticks.size)
        assertEquals(-0.707f, ticks.first().cos, 0.001f)
        assertEquals(0.707f, ticks.first().sin, 0.001f)
        assertEquals(0.707f, ticks.last().cos, 0.001f)
        assertEquals(0.707f, ticks.last().sin, 0.001f)
    }
}
