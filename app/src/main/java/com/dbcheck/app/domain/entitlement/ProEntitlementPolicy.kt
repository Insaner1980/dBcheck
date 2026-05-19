package com.dbcheck.app.domain.entitlement

object ProEntitlementPolicy {
    fun isProUser(isPurchased: Boolean, isDebugBuild: Boolean, debugForceFreeEnabled: Boolean): Boolean =
        if (isDebugBuild) {
            !debugForceFreeEnabled
        } else {
            isPurchased
        }
}
