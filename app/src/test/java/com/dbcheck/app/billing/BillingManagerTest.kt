package com.dbcheck.app.billing

import android.app.Activity
import app.cash.turbine.test
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.QueryProductDetailsResult
import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.testStringContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BillingManagerTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun queriedPurchasedProductIsAcknowledgedWithoutUserCompletionEvent() = runTest {
            val billingClient = mockk<BillingClient>(relaxed = true)
            billingClient.respondToAcknowledge()
            val manager = createManager(billingClient)

            manager.purchaseEvents.test {
                manager.processExistingPurchaseSnapshot(
                    listOf(
                        billingPurchase(
                            acknowledged = false,
                            token = "query-token",
                        ),
                    ),
                )

                assertEquals(true, manager.isPurchased.value)
                expectNoEvents()
            }
            verify {
                billingClient.acknowledgePurchase(
                    match { it.purchaseToken == "query-token" },
                    any(),
                )
            }
        }

    @Test
    fun pendingPurchaseUpdateDoesNotUnlockProAndEmitsPendingEvent() = runTest {
            val billingClient = mockk<BillingClient>(relaxed = true)
            val manager = createManager(billingClient)

            manager.purchaseEvents.test {
                manager.onPurchasesUpdated(
                    billingResult(BillingClient.BillingResponseCode.OK),
                    mutableListOf(pendingBillingPurchase()),
                )

                assertEquals(null, manager.isPurchased.value)
                assertEquals(PurchaseEvent.Pending, awaitItem())
                expectNoEvents()
            }
            verify(exactly = 0) {
                billingClient.acknowledgePurchase(any(), any())
            }
        }

    @Test
    fun purchasedNonProProductUpdateDoesNotUnlockPro() = runTest {
            val billingClient = mockk<BillingClient>(relaxed = true)
            val manager = createManager(billingClient)

            manager.purchaseEvents.test {
                manager.onPurchasesUpdated(
                    billingResult(BillingClient.BillingResponseCode.OK),
                    mutableListOf(
                        billingPurchase(
                            productId = "other_product",
                            acknowledged = false,
                            token = "other-token",
                        ),
                    ),
                )

                assertEquals(null, manager.isPurchased.value)
                expectNoEvents()
            }
            verify(exactly = 0) {
                billingClient.acknowledgePurchase(any(), any())
            }
        }

    @Test
    fun alreadyOwnedResponseDoesNotUnlockBeforeVerifiedProPurchase() = runTest {
            val billingClient = mockk<BillingClient>(relaxed = true)
            billingClient.respondToQueryPurchases(emptyList())
            val manager = createManager(billingClient)

            manager.isPurchased.test {
                assertEquals(null, awaitItem())

                manager.onPurchasesUpdated(
                    billingResult(BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED),
                    null,
                )

                assertEquals(false, awaitItem())
                expectNoEvents()
            }
            verify {
                billingClient.queryPurchasesAsync(any(), any())
            }
        }

    @Test
    fun alreadyOwnedResponseRefreshesAndAcknowledgesExistingPurchase() = runTest {
            val billingClient = mockk<BillingClient>(relaxed = true)
            billingClient.respondToQueryPurchases(
                listOf(
                    billingPurchase(
                        acknowledged = false,
                        token = "already-owned-token",
                    ),
                ),
            )
            billingClient.respondToAcknowledge()
            val manager = createManager(billingClient)

            manager.purchaseEvents.test {
                manager.onPurchasesUpdated(
                    billingResult(BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED),
                    null,
                )

                assertEquals(PurchaseEvent.AlreadyOwned, awaitItem())
                expectNoEvents()
            }
            verify {
                billingClient.queryPurchasesAsync(any(), any())
                billingClient.acknowledgePurchase(
                    match { it.purchaseToken == "already-owned-token" },
                    any(),
                )
            }
        }

    @Test
    fun refreshPurchasesProcessesCurrentPurchaseSnapshot() = runTest {
            val billingClient = mockk<BillingClient>(relaxed = true)
            billingClient.respondToQueryPurchases(
                listOf(
                    billingPurchase(
                        acknowledged = false,
                        token = "resume-token",
                    ),
                ),
            )
            billingClient.respondToAcknowledge()
            val manager = createManager(billingClient)

            manager.purchaseEvents.test {
                val hasPurchase = manager.refreshPurchases()

                assertEquals(true, hasPurchase)
                assertEquals(true, manager.isPurchased.value)
                expectNoEvents()
            }
            verify {
                billingClient.queryPurchasesAsync(any(), any())
                billingClient.acknowledgePurchase(
                    match { it.purchaseToken == "resume-token" },
                    any(),
                )
            }
        }

    @Test
    fun refreshPurchasesFailureReturnsFalse() = runTest {
            val billingClient = mockk<BillingClient>(relaxed = true)
            every { billingClient.queryPurchasesAsync(any(), any()) } throws
                IllegalStateException("query failed")
            val manager = createManager(billingClient)

            assertEquals(false, manager.refreshPurchases())
            assertEquals(null, manager.isPurchased.value)
        }

    @Test
    fun launchPurchaseFlowStartsBillingWithQueriedProductDetails() = runTest {
            val activity = mockk<Activity>()
            val productDetails = billingProductDetails()
            val billingFlowParams = mockk<BillingFlowParams>()
            val billingFlowParamsFactory =
                BillingFlowParamsFactory {
                    assertEquals(productDetails, it)
                    billingFlowParams
                }
            val billingClient = mockk<BillingClient>(relaxed = true)
            billingClient.respondToQueryProductDetails(listOf(productDetails))
            every { billingClient.launchBillingFlow(activity, billingFlowParams) } returns
                billingResult(BillingClient.BillingResponseCode.OK)
            val manager = createManager(
                billingClient = billingClient,
                billingFlowParamsFactory = billingFlowParamsFactory,
            )

            val result = manager.launchPurchaseFlow(activity)

            assertEquals(PurchaseLaunchResult.Started, result)
            verify {
                billingClient.queryProductDetailsAsync(any(), any())
                billingClient.launchBillingFlow(activity, billingFlowParams)
            }
        }

    @Test
    fun launchPurchaseFlowReturnsUnavailableWhenProductDetailsAreMissing() = runTest {
            val activity = mockk<Activity>()
            val billingClient = mockk<BillingClient>(relaxed = true)
            billingClient.respondToQueryProductDetails(emptyList())
            val manager = createManager(billingClient)

            val result = manager.launchPurchaseFlow(activity)

            assertEquals(PurchaseLaunchResult.Unavailable("dBcheck Pro is not available"), result)
            verify(exactly = 0) {
                billingClient.launchBillingFlow(any(), any())
            }
        }

    @Test
    fun launchPurchaseFlowUnavailableResponseDoesNotStartBillingUi() = runTest {
            val activity = mockk<Activity>()
            val billingClient = mockk<BillingClient>(relaxed = true)
            billingClient.respondToQueryProductDetails(
                productDetails = emptyList(),
                responseCode = BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            )
            val manager = createManager(billingClient)

            val result = manager.launchPurchaseFlow(activity)

            assertEquals(PurchaseLaunchResult.Unavailable("Google Play Billing is unavailable"), result)
            verify(exactly = 0) {
                billingClient.launchBillingFlow(any(), any())
            }
        }

    @Test
    fun acknowledgePurchaseExceptionEmitsFailureAndKeepsBillingEventsAlive() = runTest {
            val billingClient = mockk<BillingClient>(relaxed = true)
            every { billingClient.acknowledgePurchase(any(), any()) } throws
                IllegalStateException("acknowledge failed")
            val manager = createManager(billingClient)

            manager.purchaseEvents.test {
                manager.onPurchasesUpdated(
                    billingResult(BillingClient.BillingResponseCode.OK),
                    mutableListOf(
                        billingPurchase(
                            acknowledged = false,
                            token = "crash-token",
                        ),
                    ),
                )

                assertEquals(
                    PurchaseEvent.Failed("Purchase could not be finalized. Try again from Google Play."),
                    awaitItem(),
                )
                assertEquals(null, manager.isPurchased.value)

                manager.onPurchasesUpdated(
                    billingResult(BillingClient.BillingResponseCode.USER_CANCELED),
                    null,
                )

                assertEquals(PurchaseEvent.Cancelled, awaitItem())
                expectNoEvents()
            }
        }

    @Test
    fun acknowledgePurchaseFailureDoesNotUnlockPro() = runTest {
            val billingClient = mockk<BillingClient>(relaxed = true)
            billingClient.respondToAcknowledge(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE)
            val manager = createManager(billingClient)

            manager.purchaseEvents.test {
                manager.onPurchasesUpdated(
                    billingResult(BillingClient.BillingResponseCode.OK),
                    mutableListOf(
                        billingPurchase(
                            acknowledged = false,
                            token = "failed-ack-token",
                        ),
                    ),
                )

                assertEquals(
                    PurchaseEvent.Failed("Purchase could not be finalized. Try again from Google Play."),
                    awaitItem(),
                )
                assertEquals(null, manager.isPurchased.value)
                expectNoEvents()
            }
        }

    private fun createManager(
        billingClient: BillingClient,
        billingFlowParamsFactory: BillingFlowParamsFactory = BillingFlowParamsFactory {
            mockk(relaxed = true)
        },
    ): BillingManager = BillingManager(
            mainDispatcher = UnconfinedTestDispatcher(),
            billingClient = billingClient,
            context = testStringContext(),
            billingFlowParamsFactory = billingFlowParamsFactory,
        )

    private fun BillingClient.respondToAcknowledge(responseCode: Int = BillingClient.BillingResponseCode.OK) {
        every { acknowledgePurchase(any(), any()) } answers {
            secondArg<AcknowledgePurchaseResponseListener>()
                .onAcknowledgePurchaseResponse(billingResult(responseCode))
        }
    }

    private fun BillingClient.respondToQueryPurchases(purchases: List<Purchase>) {
        every { queryPurchasesAsync(any(), any()) } answers {
            secondArg<PurchasesResponseListener>()
                .onQueryPurchasesResponse(
                    billingResult(BillingClient.BillingResponseCode.OK),
                    purchases,
                )
        }
    }

    private fun BillingClient.respondToQueryProductDetails(
        productDetails: List<ProductDetails>,
        responseCode: Int = BillingClient.BillingResponseCode.OK,
    ) {
        every { queryProductDetailsAsync(any(), any()) } answers {
            secondArg<ProductDetailsResponseListener>()
                .onProductDetailsResponse(
                    billingResult(responseCode),
                    QueryProductDetailsResult.create(productDetails, emptyList()),
                )
        }
    }

    private fun billingResult(responseCode: Int): BillingResult = BillingResult
            .newBuilder()
            .setResponseCode(responseCode)
            .build()

    private fun billingProductDetails(): ProductDetails = mockk {
            every { oneTimePurchaseOfferDetails } returns null
            every { oneTimePurchaseOfferDetailsList } returns emptyList()
        }

    private fun billingPurchase(
        productId: String = BillingManager.PRO_PRODUCT_ID,
        acknowledged: Boolean,
        token: String,
    ): Purchase = mockk {
            every { products } returns listOf(productId)
            every { purchaseState } returns Purchase.PurchaseState.PURCHASED
            every { isAcknowledged } returns acknowledged
            every { purchaseToken } returns token
        }

    private fun pendingBillingPurchase(): Purchase = mockk {
            every { products } returns listOf(BillingManager.PRO_PRODUCT_ID)
            every { purchaseState } returns Purchase.PurchaseState.PENDING
            every { isAcknowledged } returns false
            every { purchaseToken } returns "pending-token"
        }
}
