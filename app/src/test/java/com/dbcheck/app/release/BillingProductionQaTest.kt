package com.dbcheck.app.release

import com.dbcheck.app.billing.BillingManager
import com.dbcheck.app.domain.entitlement.ProEntitlementPolicy
import com.dbcheck.app.projectFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BillingProductionQaTest {
    @Test
    fun billingProductionQaDocumentsPlayConsoleStateAndManualPurchaseCoverage() {
        val qaDoc = billingProductionQaFile()

        assertTrue("Osa 96 requires docs/qa/billing-production-qa.md", qaDoc.isFile)

        val content = qaDoc.readText()
        expectedQaMarkers.forEach { marker ->
            assertTrue("Billing production QA must document $marker", content.contains(marker))
        }
    }

    @Test
    fun billingCodeMatchesReleaseQaContract() {
        assertEquals("dbcheck_pro", BillingManager.PRO_PRODUCT_ID)

        val manifest = projectFile("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("com.android.vending.BILLING"))

        val versions = projectRootFile("gradle/libs.versions.toml").readText()
        val billingVersion =
            Regex("""billing = "(\d+)\.(\d+)\.(\d+)"""")
                .find(versions)
                ?.groupValues
                ?: error("Billing version must be declared in gradle/libs.versions.toml")
        assertTrue("Play Billing Library major version must be 8+", billingVersion[1].toInt() >= 8)

        val billingManager = projectFile("src/main/java/com/dbcheck/app/billing/BillingManager.kt").readText()
        listOf(
            "setProductId(PRO_PRODUCT_ID)",
            "setProductType(BillingClient.ProductType.INAPP)",
            "enablePendingPurchases",
            "enableOneTimeProducts",
            "enableAutoServiceReconnection",
            "queryProductDetails",
            "queryPurchasesAsync",
            "acknowledgePurchase",
            "Purchase.PurchaseState.PENDING",
            "PurchaseEvent.Pending",
            "PurchaseEvent.Cancelled",
            "PurchaseEvent.AlreadyOwned",
        ).forEach { marker ->
            assertTrue("BillingManager must keep $marker for release QA", billingManager.contains(marker))
        }

        assertFalse(
            "Release without purchase must not open debug Pro",
            ProEntitlementPolicy.isProUser(
                isPurchased = false,
                isDebugBuild = false,
                debugForceFreeEnabled = false,
            ),
        )
        assertFalse(
            "Release must ignore debug-force-free and stay non-Pro without purchase",
            ProEntitlementPolicy.isProUser(
                isPurchased = false,
                isDebugBuild = false,
                debugForceFreeEnabled = true,
            ),
        )
        assertTrue(
            "Release Pro must come from a verified purchase",
            ProEntitlementPolicy.isProUser(
                isPurchased = true,
                isDebugBuild = false,
                debugForceFreeEnabled = false,
            ),
        )
    }

    private fun billingProductionQaFile(): File = listOf(
        File("docs/qa/billing-production-qa.md"),
        File("..", "docs/qa/billing-production-qa.md"),
    ).firstOrNull(File::isFile) ?: File("docs/qa/billing-production-qa.md")

    private fun projectRootFile(path: String): File = listOf(
        File(path),
        File("..", path),
    ).first(File::isFile)

    private companion object {
        val expectedQaMarkers = listOf(
            "# dBcheck Billing production QA",
            "dbcheck_pro",
            "com.android.vending.BILLING",
            "Billing Library 8.3.0",
            "Play Console: NOT VERIFIED",
            "Manual purchase QA: NOT RUN",
            "License tester purchase: NOT RUN",
            "Release entitlement",
            "debug Pro",
            "PurchaseEvent.Completed",
            "PurchaseEvent.Pending",
            "PurchaseEvent.Cancelled",
            "PurchaseEvent.AlreadyOwned",
            "PurchaseLaunchResult.AlreadyOwned",
            "queryProductDetails",
            "queryPurchasesAsync",
            "acknowledgePurchase",
            "Test card, always approves",
            "Test card, always declines",
            "Slow test card, approves after a few minutes",
            "Slow test card, declines after a few minutes",
            "restore",
            "Release risk",
            "Osa 97 - Release signing QA",
            "https://developer.android.com/google/play/billing/test",
            "https://developer.android.com/google/play/billing/lifecycle/one-time",
            "https://support.google.com/googleplay/android-developer/answer/1153481",
        )
    }
}
