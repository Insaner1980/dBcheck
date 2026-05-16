package com.dbcheck.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class HearingTestRouteAccessPolicyTest {
    @Test
    fun freeUserRequiresUpgradeForHearingTestRoutes() {
        assertEquals(
            HearingTestRouteAccess.UpgradeRequired,
            HearingTestRouteAccessPolicy.accessFor(isProUser = false),
        )
    }

    @Test
    fun proUserCanOpenHearingTestRoutes() {
        assertEquals(
            HearingTestRouteAccess.Allowed,
            HearingTestRouteAccessPolicy.accessFor(isProUser = true),
        )
    }
}
