package com.dbcheck.app.ui.analytics.components

import org.junit.Assert.assertEquals
import org.junit.Test

class HearingTestCtaActionTest {
    @Test
    fun lockedStartClickNavigatesToUpgradeInsteadOfStartingTest() {
        var startClicks = 0
        var upgradeClicks = 0

        runHearingTestCtaStartClick(
            isLocked = true,
            onStartTest = { startClicks++ },
            onUpgradeClick = { upgradeClicks++ },
        )

        assertEquals(0, startClicks)
        assertEquals(1, upgradeClicks)
    }

    @Test
    fun unlockedStartClickStartsTest() {
        var startClicks = 0
        var upgradeClicks = 0

        runHearingTestCtaStartClick(
            isLocked = false,
            onStartTest = { startClicks++ },
            onUpgradeClick = { upgradeClicks++ },
        )

        assertEquals(1, startClicks)
        assertEquals(0, upgradeClicks)
    }
}
