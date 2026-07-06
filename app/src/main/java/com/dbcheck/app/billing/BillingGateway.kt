package com.dbcheck.app.billing

import android.app.Activity
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BillingGateway {
    val purchaseEvents: SharedFlow<PurchaseEvent>

    suspend fun launchPurchaseFlow(activity: Activity): PurchaseLaunchResult
}

interface BillingRuntimeGateway {
    fun startConnection()

    suspend fun refreshPurchases(): Boolean
}

interface BillingEntitlementSource {
    val isPurchased: StateFlow<Boolean?>
}

sealed interface PurchaseLaunchResult {
    data object Started : PurchaseLaunchResult

    data object AlreadyOwned : PurchaseLaunchResult

    data class Unavailable(val reason: String) : PurchaseLaunchResult

    data class Failed(val reason: String) : PurchaseLaunchResult
}

sealed interface PurchaseEvent {
    data object Completed : PurchaseEvent

    data object Pending : PurchaseEvent

    data object Cancelled : PurchaseEvent

    data object AlreadyOwned : PurchaseEvent

    data class Failed(val reason: String) : PurchaseEvent
}
