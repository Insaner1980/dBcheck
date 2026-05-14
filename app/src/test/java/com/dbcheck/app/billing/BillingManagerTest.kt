package com.dbcheck.app.billing

import app.cash.turbine.test
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.dbcheck.app.MainDispatcherRule
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

    private fun createManager(billingClient: BillingClient): BillingManager = BillingManager(
            mainDispatcher = UnconfinedTestDispatcher(),
            billingClient = billingClient,
        )

    private fun BillingClient.respondToAcknowledge() {
        every { acknowledgePurchase(any(), any()) } answers {
            secondArg<AcknowledgePurchaseResponseListener>()
                .onAcknowledgePurchaseResponse(billingResult(BillingClient.BillingResponseCode.OK))
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

    private fun billingResult(responseCode: Int): BillingResult = BillingResult
            .newBuilder()
            .setResponseCode(responseCode)
            .build()

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
