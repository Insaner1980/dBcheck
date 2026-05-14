package com.dbcheck.app.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult

internal fun BillingResult.toPurchaseLaunchFailure(): PurchaseLaunchResult = when (responseCode) {
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
        -> PurchaseLaunchResult.Unavailable("Google Play Billing is unavailable")

        else -> PurchaseLaunchResult.Failed("Unable to start purchase")
    }
