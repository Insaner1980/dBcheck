package com.dbcheck.app.ui.analytics.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyBarChartTest {
    @Test
    fun labelBaselineLeavesDescentAndBottomPaddingInsideCanvas() {
        val baseline =
            weeklyBarLabelBaseline(
                canvasHeight = 100f,
                labelBottomPadding = 4f,
                textDescent = 3f,
            )

        assertEquals(93f, baseline, 0.001f)
        assertTrue(baseline < 100f)
    }

    @Test
    fun emptyWeeklyChartStateDoesNotReportZeroDecibels() {
        val state = weeklyExposureSectionState(hasExposureData = false)

        assertEquals(false, state.showExposureMetrics)
        assertEquals("No weekly exposure data", state.emptyTitle)
    }
}
