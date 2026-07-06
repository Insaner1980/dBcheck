package com.dbcheck.app.ui.sleep

import org.junit.Assert.assertEquals
import org.junit.Test

class SleepSetupEntryPolicyTest {
    @Test
    fun proUserCanOpenSleepSetupRoute() {
        assertEquals(
            SleepSetupEntryDestination.SleepSetup,
            SleepSetupEntryPolicy.destination(isProUser = true),
        )
    }

    @Test
    fun freeUserIsSentToUpgradeInsteadOfSleepSetupExecution() {
        assertEquals(
            SleepSetupEntryDestination.Upgrade,
            SleepSetupEntryPolicy.destination(isProUser = false),
        )
    }
}
