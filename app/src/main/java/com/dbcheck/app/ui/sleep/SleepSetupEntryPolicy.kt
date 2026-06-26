package com.dbcheck.app.ui.sleep

internal enum class SleepSetupEntryDestination {
    SleepSetup,
    Upgrade,
}

internal object SleepSetupEntryPolicy {
    fun destination(isProUser: Boolean): SleepSetupEntryDestination = if (isProUser) {
            SleepSetupEntryDestination.SleepSetup
        } else {
            SleepSetupEntryDestination.Upgrade
        }
}
