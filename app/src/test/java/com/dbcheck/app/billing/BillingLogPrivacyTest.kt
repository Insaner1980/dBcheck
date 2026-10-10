package com.dbcheck.app.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import org.junit.Assert.assertEquals
import org.junit.Test

class BillingLogPrivacyTest {
    @Test
    fun sdkDiagnosticMessageIsNotIncludedInLog() {
        val result = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.ERROR)
            .setDebugMessage("synthetic-private-token")
            .build()
        val method = Class.forName("com.dbcheck.app.billing.BillingManagerKt")
            .declaredMethods.single { it.name == "toBillingLogMessage" }
            .apply { isAccessible = true }
        assertEquals("Query failed: 6", method.invoke(null, result, "Query failed"))
    }

    @Test
    fun throwableDiagnosticMessageIsNotIncludedInLog() {
        val error = IllegalStateException("synthetic-private-token /private/user/path")
        val method = Class.forName("com.dbcheck.app.billing.BillingManagerKt")
            .declaredMethods.single { it.name == "toBillingFailureLogMessage" }
            .apply { isAccessible = true }
        assertEquals("Query failed: IllegalStateException", method.invoke(null, error, "Query failed"))
    }
}
