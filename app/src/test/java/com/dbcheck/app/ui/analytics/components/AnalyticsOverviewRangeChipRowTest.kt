package com.dbcheck.app.ui.analytics.components

import com.dbcheck.app.ui.analytics.state.AnalyticsOverviewRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsOverviewRangeChipRowTest {
    @Test
    fun freeUserSeesMonthlyRangeLockedWithoutHidingIt() {
        val items =
            analyticsOverviewRangeChipItems(
                selectedRange = AnalyticsOverviewRange.WEEKLY,
                isProUser = false,
            )

        assertEquals(
            listOf(
                AnalyticsOverviewRange.WEEKLY,
                AnalyticsOverviewRange.MONTHLY,
            ),
            items.map { it.range },
        )
        assertFalse(items.single { it.range == AnalyticsOverviewRange.WEEKLY }.isLocked)
        assertTrue(items.single { it.range == AnalyticsOverviewRange.MONTHLY }.isLocked)
    }

    @Test
    fun selectedRangeCanBeFreeLockedMonthlyRange() {
        val items =
            analyticsOverviewRangeChipItems(
                selectedRange = AnalyticsOverviewRange.MONTHLY,
                isProUser = false,
            )

        assertEquals(AnalyticsOverviewRange.MONTHLY, items.single { it.isSelected }.range)
        assertTrue(items.single { it.range == AnalyticsOverviewRange.MONTHLY }.isLocked)
    }

    @Test
    fun proUserRangesAreUnlocked() {
        val items =
            analyticsOverviewRangeChipItems(
                selectedRange = AnalyticsOverviewRange.MONTHLY,
                isProUser = true,
            )

        assertTrue(items.none { it.isLocked })
        assertEquals(AnalyticsOverviewRange.MONTHLY, items.single { it.isSelected }.range)
    }
}
