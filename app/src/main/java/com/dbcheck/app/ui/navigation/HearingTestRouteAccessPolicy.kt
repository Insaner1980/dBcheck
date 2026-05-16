package com.dbcheck.app.ui.navigation

internal enum class HearingTestRouteAccess {
    Allowed,
    UpgradeRequired,
}

internal object HearingTestRouteAccessPolicy {
    fun accessFor(isProUser: Boolean): HearingTestRouteAccess = if (isProUser) {
            HearingTestRouteAccess.Allowed
        } else {
            HearingTestRouteAccess.UpgradeRequired
        }
}
