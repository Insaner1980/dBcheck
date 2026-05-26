package com.dbcheck.app.privacy

import com.dbcheck.app.data.export.ExportFileCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class PrivacyConfigTest {
    @Test
    fun manifestDisablesSystemBackupAndDeclaresBackupRules() {
        val application = projectXml("src/main/AndroidManifest.xml").documentElement
            .getElementsByTagName("application")
            .item(0) as Element

        assertEquals("false", application.androidAttribute("allowBackup"))
        assertEquals("@xml/backup_rules", application.androidAttribute("fullBackupContent"))
        assertEquals("@xml/data_extraction_rules", application.androidAttribute("dataExtractionRules"))
    }

    @Test
    fun fileProviderAllowsOnlyExportCacheDirectory() {
        val paths = projectXml("src/main/res/xml/file_paths.xml").documentElement
        val cachePath = paths.getElementsByTagName("cache-path").item(0) as Element

        assertEquals(ExportFileCache.EXPORT_DIRECTORY_NAME, cachePath.getAttribute("name"))
        assertEquals(ExportFileCache.EXPORT_DIRECTORY_PATH, cachePath.getAttribute("path"))
        assertNotEquals("/", cachePath.getAttribute("path"))
    }

    @Test
    fun manifestFileProviderAuthorityUsesRuntimeContractSuffix() {
        val application = projectXml("src/main/AndroidManifest.xml").documentElement
            .getElementsByTagName("application")
            .item(0) as Element
        val provider = application
            .elementsNamed("provider")
            .first { it.androidAttribute("name") == "androidx.core.content.FileProvider" }

        assertEquals(
            "\${applicationId}.${ExportFileCache.FILE_PROVIDER_AUTHORITY_SUFFIX}",
            provider.androidAttribute("authorities"),
        )
        assertEquals("false", provider.androidAttribute("exported"))
        assertEquals("true", provider.androidAttribute("grantUriPermissions"))
    }

    @Test
    fun backupRulesExcludePrivateAppDataFromAllTransferModes() {
        val fullBackupRules = projectXml("src/main/res/xml/backup_rules.xml")
        val dataExtractionRules = projectXml("src/main/res/xml/data_extraction_rules.xml")

        assertHasRootExclude(fullBackupRules.documentElement)
        assertHasRootExclude(dataExtractionRules.getElementsByTagName("cloud-backup").item(0) as Element)
        assertHasRootExclude(dataExtractionRules.getElementsByTagName("device-transfer").item(0) as Element)
    }

    @Test
    fun healthConnectAliasesTargetOnlyDisclosureActivity() {
        val application = projectXml("src/main/AndroidManifest.xml").documentElement
            .getElementsByTagName("application")
            .item(0) as Element
        val disclosureActivity = application
            .elementsNamed("activity")
            .first { it.androidAttribute("name") == ".HealthConnectPermissionDisclosureActivity" }

        assertEquals("false", disclosureActivity.androidAttribute("exported"))

        val aliases = application.elementsNamed("activity-alias")
        val rationaleAlias = aliases.first {
            it.androidAttribute("name") == ".HealthConnectPermissionsRationaleActivity"
        }
        val usageAlias = aliases.first {
            it.androidAttribute("name") == ".HealthConnectPermissionUsageActivity"
        }

        assertEquals(".HealthConnectPermissionDisclosureActivity", rationaleAlias.androidAttribute("targetActivity"))
        assertEquals(".HealthConnectPermissionDisclosureActivity", usageAlias.androidAttribute("targetActivity"))
        assertEquals("android.permission.START_VIEW_PERMISSION_USAGE", usageAlias.androidAttribute("permission"))
    }

    @Test
    fun settingsFooterDoesNotExposeInactiveLegalLinks() {
        val strings = projectXml("src/main/res/values/strings.xml").documentElement
        val footer =
            strings
                .elementsNamed("string")
                .first { it.getAttribute("name") == "settings_footer" }
                .textContent

        assertEquals("dBcheck v%1\$s", footer)
        assertFalse(footer.contains("Privacy"))
        assertFalse(footer.contains("Terms"))
    }

    private fun assertHasRootExclude(parent: Element) {
        val excludes = parent.getElementsByTagName("exclude")
        val hasRootExclude =
            (0 until excludes.length)
                .map { excludes.item(it) as Element }
                .any { element ->
                    element.getAttribute("domain") == "root" &&
                        element.getAttribute("path") == "."
                }

        assertTrue("Expected root data exclude in ${parent.tagName}", hasRootExclude)
    }

    private fun projectXml(relativePath: String) = DocumentBuilderFactory
            .newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(projectFile(relativePath))

    private fun Element.androidAttribute(name: String): String =
        getAttributeNS("http://schemas.android.com/apk/res/android", name)

    private fun Element.elementsNamed(tagName: String): List<Element> = getElementsByTagName(tagName).let { nodes ->
        (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private fun projectFile(relativePath: String): File {
        val userDir = File(requireNotNull(System.getProperty("user.dir")))
        return listOf(
            File(userDir, relativePath),
            File(userDir, "app/$relativePath"),
            File(userDir.parentFile ?: userDir, "app/$relativePath"),
        ).first { it.isFile }
    }
}
