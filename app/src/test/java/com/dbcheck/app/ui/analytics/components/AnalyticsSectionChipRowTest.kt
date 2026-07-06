package com.dbcheck.app.ui.analytics.components

import com.dbcheck.app.ui.analytics.state.AnalyticsSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsSectionChipRowTest {
    @Test
    fun freeUserSeesLockedProSectionsWithoutHidingThem() {
        val items =
            analyticsSectionChipItems(
                selectedSection = AnalyticsSection.OVERVIEW,
                isProUser = false,
            )

        assertEquals(
            listOf(
                AnalyticsSection.OVERVIEW,
                AnalyticsSection.SPECTRAL,
                AnalyticsSection.ENVIRONMENT,
            ),
            items.map { it.section },
        )
        assertFalse(items[0].isLocked)
        assertTrue(items[1].isLocked)
        assertTrue(items[2].isLocked)
    }

    @Test
    fun selectedSectionCanBeAFreeLockedSection() {
        val items =
            analyticsSectionChipItems(
                selectedSection = AnalyticsSection.SPECTRAL,
                isProUser = false,
            )

        assertEquals(AnalyticsSection.SPECTRAL, items.single { it.isSelected }.section)
        assertTrue(items.single { it.section == AnalyticsSection.SPECTRAL }.isLocked)
    }

    @Test
    fun proUserSectionsAreUnlocked() {
        val items =
            analyticsSectionChipItems(
                selectedSection = AnalyticsSection.ENVIRONMENT,
                isProUser = true,
            )

        assertTrue(items.none { it.isLocked })
        assertEquals(AnalyticsSection.ENVIRONMENT, items.single { it.isSelected }.section)
    }
}
