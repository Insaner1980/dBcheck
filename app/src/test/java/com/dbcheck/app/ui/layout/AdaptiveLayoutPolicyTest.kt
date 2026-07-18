package com.dbcheck.app.ui.layout

import com.dbcheck.app.ui.components.bottomNavItemSlotWeight
import com.dbcheck.app.ui.components.shouldUseCompactHeightScrolling
import com.dbcheck.app.ui.navigation.shouldApplyContentNavigationBarPadding
import com.dbcheck.app.ui.navigation.shouldUseNavigationRail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLayoutPolicyTest {
    @Test
    fun navigationRailStartsAtSixHundredDp() {
        assertFalse(shouldUseNavigationRail(windowWidthDp = 599.9f))
        assertTrue(shouldUseNavigationRail(windowWidthDp = 600f))
        assertTrue(shouldUseNavigationRail(windowWidthDp = 840f))
    }

    @Test
    fun contentGetsNavigationBarPaddingWhenBottomBarIsNotPresent() {
        assertFalse(shouldApplyContentNavigationBarPadding(useRail = false, showNavigation = true))
        assertTrue(shouldApplyContentNavigationBarPadding(useRail = true, showNavigation = true))
        assertTrue(shouldApplyContentNavigationBarPadding(useRail = false, showNavigation = false))
        assertTrue(shouldApplyContentNavigationBarPadding(useRail = true, showNavigation = false))
    }

    @Test
    fun bottomNavigationItemsUseEqualWeightedSlots() {
        assertEquals(1f, bottomNavItemSlotWeight(itemCount = 5))
        assertEquals(0f, bottomNavItemSlotWeight(itemCount = 0))
    }

    @Test
    fun compactHeightScreensUseScrollableContent() {
        assertTrue(shouldUseCompactHeightScrolling(availableHeightDp = 639.9f))
        assertFalse(shouldUseCompactHeightScrolling(availableHeightDp = 640f))
    }
}
