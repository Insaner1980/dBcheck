package com.dbcheck.app.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.dbcheck.app.BuildConfig
import com.dbcheck.app.R
import com.dbcheck.app.di.MainDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager :
    BillingGateway,
    PurchasesUpdatedListener {
        companion object {
            private const val TAG = "BillingManager"
            private const val LOG_BILLING_SERVICE_DISCONNECTED =
                "Billing service disconnected, auto-reconnection enabled"
            private const val LOG_EXISTING_PURCHASE_QUERY_FAILED = "Existing purchase query failed"
            private const val LOG_USER_CANCELLED_PURCHASE = "User cancelled purchase"
            private const val LOG_PURCHASE_FAILED = "Purchase failed"
            private const val LOG_ACKNOWLEDGE_PURCHASE_FAILED = "Failed to acknowledge purchase"
            private const val LOG_BILLING_CALLBACK_FAILED = "Billing callback handling failed"
            const val PRO_PRODUCT_ID = "dbcheck_pro"
        }

        private val _isPurchased = MutableStateFlow<Boolean?>(null)
        val isPurchased: StateFlow<Boolean?> = _isPurchased
        private val _purchaseEvents = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 1)
        override val purchaseEvents: SharedFlow<PurchaseEvent> = _purchaseEvents.asSharedFlow()

        private val scope: CoroutineScope
        private val context: Context
        private val billingFlowParamsFactory: BillingFlowParamsFactory

        private var billingClient: BillingClient

        @Inject
        constructor(
            @ApplicationContext context: Context,
            @MainDispatcher mainDispatcher: CoroutineDispatcher,
        ) {
            scope = CoroutineScope(SupervisorJob() + mainDispatcher)
            this.context = context
            billingFlowParamsFactory = defaultBillingFlowParamsFactory
            billingClient = createBillingClient(context, this)
        }

        internal constructor(
            mainDispatcher: CoroutineDispatcher,
            billingClient: BillingClient,
            context: Context,
            billingFlowParamsFactory: BillingFlowParamsFactory = defaultBillingFlowParamsFactory,
        ) {
            scope = CoroutineScope(SupervisorJob() + mainDispatcher)
            this.billingClient = billingClient
            this.context = context
            this.billingFlowParamsFactory = billingFlowParamsFactory
        }

        fun startConnection() {
            billingClient.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            launchBillingTask { refreshPurchases() }
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        logWarning { LOG_BILLING_SERVICE_DISCONNECTED }
                    }
                },
            )
        }

        suspend fun refreshPurchases(): Boolean = runCatching {
                queryExistingPurchases()
            }.getOrElse { error ->
                if (error is CancellationException) throw error
                logError { error.toBillingFailureLogMessage(LOG_EXISTING_PURCHASE_QUERY_FAILED) }
                false
            }

        private suspend fun queryExistingPurchases(): Boolean {
            val params =
                QueryPurchasesParams
                    .newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()

            val result = billingClient.queryPurchasesAsync(params)
            return if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                processExistingPurchaseSnapshot(result.purchasesList)
            } else {
                logWarning { result.billingResult.toBillingLogMessage(LOG_EXISTING_PURCHASE_QUERY_FAILED) }
                false
            }
        }

        internal suspend fun processExistingPurchaseSnapshot(purchases: List<Purchase>): Boolean {
            val proPurchases = purchases.filter { it.isProProduct() }
            val completedProPurchases =
                proPurchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }

            if (completedProPurchases.isEmpty()) {
                _isPurchased.value = false
            } else {
                completedProPurchases.forEach { purchase ->
                    processPurchasedProduct(purchase, emitCompletionEvent = false)
                }
            }

            if (proPurchases.any { it.purchaseState == Purchase.PurchaseState.PENDING }) {
                _purchaseEvents.emit(PurchaseEvent.Pending)
            }
            return completedProPurchases.isNotEmpty()
        }

        override suspend fun launchPurchaseFlow(activity: Activity): PurchaseLaunchResult = runCatching {
                if (_isPurchased.value == true) {
                    _purchaseEvents.tryEmit(PurchaseEvent.AlreadyOwned)
                    PurchaseLaunchResult.AlreadyOwned
                } else {
                    launchNewPurchaseFlow(activity)
                }
            }.getOrElse { error ->
                if (error is CancellationException) throw error
                logError { error.toBillingFailureLogMessage(LOG_PURCHASE_FAILED) }
                PurchaseLaunchResult.Failed(context.getString(R.string.billing_start_purchase_failed))
            }

        private suspend fun launchNewPurchaseFlow(activity: Activity): PurchaseLaunchResult {
            val productList =
                listOf(
                    QueryProductDetailsParams.Product
                        .newBuilder()
                        .setProductId(PRO_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                )

            val params =
                QueryProductDetailsParams
                    .newBuilder()
                    .setProductList(productList)
                    .build()

            val result = billingClient.queryProductDetails(params)
            val productDetailsList = result.productDetailsList
            return if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                result.billingResult.toLaunchFailure()
            } else {
                val productDetails = productDetailsList?.firstOrNull()
                if (productDetails == null) {
                    PurchaseLaunchResult.Unavailable(context.getString(R.string.billing_pro_not_available))
                } else {
                    billingClient
                        .launchBillingFlow(activity, billingFlowParamsFactory.create(productDetails))
                        .toLaunchResult()
                }
            }
        }

        override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    purchases?.filter { it.isProProduct() }?.forEach { purchase ->
                        when (purchase.purchaseState) {
                            Purchase.PurchaseState.PURCHASED ->
                                launchBillingTask { processPurchasedProduct(purchase, emitCompletionEvent = true) }

                            Purchase.PurchaseState.PENDING ->
                                _purchaseEvents.tryEmit(PurchaseEvent.Pending)
                        }
                    }
                }

                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    markAlreadyOwnedAndRefreshPurchases()
                }

                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    logDebug { LOG_USER_CANCELLED_PURCHASE }
                    _purchaseEvents.tryEmit(PurchaseEvent.Cancelled)
                }

                else -> {
                    logError { billingResult.toBillingLogMessage(LOG_PURCHASE_FAILED) }
                    _purchaseEvents.tryEmit(
                        PurchaseEvent.Failed(context.getString(R.string.billing_purchase_failed)),
                    )
                }
            }
        }

        private fun markAlreadyOwnedAndRefreshPurchases() {
            launchBillingTask {
                if (queryExistingPurchases()) {
                    _purchaseEvents.emit(PurchaseEvent.AlreadyOwned)
                }
            }
        }

        private suspend fun processPurchasedProduct(purchase: Purchase, emitCompletionEvent: Boolean) {
            _isPurchased.value = true
            if (!purchase.isAcknowledged) {
                val params =
                    AcknowledgePurchaseParams
                        .newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                val acknowledgeResult = runCatching { billingClient.acknowledgePurchase(params) }
                val result = acknowledgeResult.getOrNull()
                if (result == null) {
                    val error = acknowledgeResult.exceptionOrNull()
                    if (error is CancellationException) throw error
                    logError {
                        error
                            ?.toBillingFailureLogMessage(LOG_ACKNOWLEDGE_PURCHASE_FAILED)
                            ?: LOG_ACKNOWLEDGE_PURCHASE_FAILED
                    }
                    _purchaseEvents.emit(
                        PurchaseEvent.Failed(context.getString(R.string.billing_purchase_acknowledge_failed)),
                    )
                    return
                }
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    logError { result.toBillingLogMessage(LOG_ACKNOWLEDGE_PURCHASE_FAILED) }
                    _purchaseEvents.emit(
                        PurchaseEvent.Failed(context.getString(R.string.billing_purchase_acknowledge_failed)),
                    )
                    return
                }
            }
            if (emitCompletionEvent) {
                _purchaseEvents.emit(PurchaseEvent.Completed)
            }
        }

        private fun BillingResult.toLaunchResult(): PurchaseLaunchResult = when (responseCode) {
                BillingClient.BillingResponseCode.OK -> PurchaseLaunchResult.Started

                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    markAlreadyOwnedAndRefreshPurchases()
                    PurchaseLaunchResult.AlreadyOwned
                }

                else -> toLaunchFailure()
            }

        private fun BillingResult.toLaunchFailure(): PurchaseLaunchResult = toPurchaseLaunchFailure(context)

        private fun logDebug(message: () -> String) {
            if (BuildConfig.DEBUG) {
                runCatching { Log.d(TAG, message()) }
            }
        }

        private fun logWarning(message: () -> String) {
            if (BuildConfig.DEBUG) {
                runCatching { Log.w(TAG, message()) }
            }
        }

        private fun logError(message: () -> String) {
            if (BuildConfig.DEBUG) {
                runCatching { Log.e(TAG, message()) }
            }
        }

        private fun launchBillingTask(block: suspend () -> Unit) {
            scope.launch {
                runCatching {
                    block()
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    logError { error.toBillingFailureLogMessage(LOG_BILLING_CALLBACK_FAILED) }
                }
            }
        }

        private fun Purchase.isProProduct(): Boolean = products.contains(PRO_PRODUCT_ID)
    }

private fun BillingResult.toBillingLogMessage(prefix: String): String = "$prefix: $responseCode - $debugMessage"

private fun Throwable.toBillingFailureLogMessage(prefix: String): String = "$prefix: ${message ?: javaClass.simpleName}"

internal fun interface BillingFlowParamsFactory {
    fun create(productDetails: ProductDetails): BillingFlowParams
}

private val defaultBillingFlowParamsFactory =
    BillingFlowParamsFactory { productDetails -> productDetails.toBillingFlowParams() }

private fun ProductDetails.toBillingFlowParams(): BillingFlowParams {
    val productDetailsParams =
        BillingFlowParams.ProductDetailsParams
            .newBuilder()
            .setProductDetails(this)
            .apply {
                val offerToken =
                    oneTimePurchaseOfferDetailsList
                        ?.firstOrNull()
                        ?.offerToken
                if (!offerToken.isNullOrBlank()) {
                    setOfferToken(offerToken)
                }
            }.build()
    return BillingFlowParams
        .newBuilder()
        .setProductDetailsParamsList(listOf(productDetailsParams))
        .build()
}

private fun createBillingClient(context: Context, listener: PurchasesUpdatedListener): BillingClient = BillingClient
        .newBuilder(context)
        .setListener(listener)
        .enablePendingPurchases(
            PendingPurchasesParams
                .newBuilder()
                .enableOneTimeProducts()
                .build(),
        ).enableAutoServiceReconnection()
        .build()
