package com.dbcheck.app.domain.entitlement

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProEntitlementPolicyTest {
    @Test
    fun releaseWithoutPurchaseIsNotPro() {
        assertFalse(
            ProEntitlementPolicy.isProUser(
                isPurchased = false,
                isDebugBuild = false,
                debugForceFreeEnabled = false,
            ),
        )
    }

    @Test
    fun releaseWithPurchaseIsPro() {
        assertTrue(
            ProEntitlementPolicy.isProUser(
                isPurchased = true,
                isDebugBuild = false,
                debugForceFreeEnabled = false,
            ),
        )
    }

    @Test
    fun debugDefaultsToProWhenForceFreeIsDisabled() {
        assertTrue(
            ProEntitlementPolicy.isProUser(
                isPurchased = false,
                isDebugBuild = true,
                debugForceFreeEnabled = false,
            ),
        )
    }

    @Test
    fun debugForceFreeDisablesProWithoutPurchase() {
        assertFalse(
            ProEntitlementPolicy.isProUser(
                isPurchased = false,
                isDebugBuild = true,
                debugForceFreeEnabled = true,
            ),
        )
    }

    @Test
    fun debugForceFreeDisablesProEvenWithPurchase() {
        assertFalse(
            ProEntitlementPolicy.isProUser(
                isPurchased = true,
                isDebugBuild = true,
                debugForceFreeEnabled = true,
            ),
        )
    }
}
