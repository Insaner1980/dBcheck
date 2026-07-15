package com.dbcheck.app.ui.analytics.components

import com.dbcheck.app.R
import com.dbcheck.app.projectFile
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
        assertEquals(R.string.exposure_summary_empty_title, state.emptyTitleRes)
    }

    @Test
    fun weeklyChartLabelsUseNumericTypeface() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/analytics/components/WeeklyBarChart.kt").readText()

        assertTrue(source.contains("ResourcesCompat.getFont(context, R.font.space_grotesk_regular)"))
        assertTrue(source.contains("typeface = labelTypeface"))
    }
}
