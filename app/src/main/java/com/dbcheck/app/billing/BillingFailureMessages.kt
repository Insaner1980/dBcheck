package com.dbcheck.app.billing

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.dbcheck.app.R

internal fun BillingResult.toPurchaseLaunchFailure(context: Context): PurchaseLaunchResult = when (responseCode) {
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
        -> PurchaseLaunchResult.Unavailable(context.getString(R.string.billing_google_play_unavailable))

        else -> PurchaseLaunchResult.Failed(context.getString(R.string.billing_start_purchase_failed))
    }
