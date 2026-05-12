package com.dbcheck.app.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import org.junit.Assert.assertEquals
import org.junit.Test

class BillingFailureMessagesTest {
    @Test
    fun launchFailureDoesNotExposeBillingDebugMessage() {
        val result =
            BillingResult
                .newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.DEVELOPER_ERROR)
                .setDebugMessage("Debug purchase token path should not be shown")
                .build()

        assertEquals(
            PurchaseLaunchResult.Failed("Unable to start purchase"),
            result.toPurchaseLaunchFailure(),
        )
    }

    @Test
    fun unavailableLaunchDoesNotExposeBillingDebugMessage() {
        val result =
            BillingResult
                .newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.BILLING_UNAVAILABLE)
                .setDebugMessage("Play backend internal reason")
                .build()

        assertEquals(
            PurchaseLaunchResult.Unavailable("Google Play Billing is unavailable"),
            result.toPurchaseLaunchFailure(),
        )
    }
}
