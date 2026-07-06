package com.dbcheck.app.architecture

import com.dbcheck.app.projectFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BillingInterfaceBindingContractTest {
    @Test
    fun billingRuntimeResponsibilitiesAreExposedAsInterfaces() {
        val gatewaySource = projectFile("src/main/java/com/dbcheck/app/billing/BillingGateway.kt").readText()
        val moduleSource = projectFile("src/main/java/com/dbcheck/app/di/BillingModule.kt").readText()
        val managerSource = projectFile("src/main/java/com/dbcheck/app/billing/BillingManager.kt").readText()

        assertTrue(gatewaySource.contains("interface BillingRuntimeGateway"))
        assertTrue(gatewaySource.contains("interface BillingEntitlementSource"))
        assertTrue(moduleSource.contains("interface BillingModule"))
        assertFalse(moduleSource.contains("abstract class BillingModule"))
        assertTrue(moduleSource.contains("@Binds"))
        assertTrue(moduleSource.contains("BillingRuntimeGateway"))
        assertTrue(moduleSource.contains("BillingEntitlementSource"))
        assertTrue(managerSource.contains("BillingRuntimeGateway"))
        assertTrue(managerSource.contains("BillingEntitlementSource"))
    }

    @Test
    fun productionConsumersDependOnBillingInterfacesInsteadOfBillingManager() {
        val violations =
            productionKotlinSources()
                .filterNot { file ->
                    val normalizedPath = file.invariantSeparatorsPath
                    normalizedPath.endsWith("billing/BillingManager.kt") ||
                        normalizedPath.endsWith("di/BillingModule.kt")
                }.filter { file -> file.readText().contains("BillingManager") }
                .map { file -> file.relativeTo(mainSourceRoot()).invariantSeparatorsPath }

        val message =
            "Production code should use billing interfaces instead of BillingManager:\n" +
                violations.joinToString("\n")

        assertTrue(message, violations.isEmpty())
    }

    private fun productionKotlinSources(): List<File> = mainSourceRoot()
        .walkTopDown()
        .filter { file -> file.isFile && file.extension == "kt" }
        .toList()

    private fun mainSourceRoot(): File = listOf(
            File("src/main/java/com/dbcheck/app"),
            File("app/src/main/java/com/dbcheck/app"),
        ).firstOrNull(File::isDirectory)
        ?: error("Main source root does not exist")
}
