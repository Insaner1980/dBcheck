package com.dbcheck.app.privacy

import org.junit.Assert.assertEquals
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

        assertEquals("exports", cachePath.getAttribute("name"))
        assertEquals("exports/", cachePath.getAttribute("path"))
        assertNotEquals("/", cachePath.getAttribute("path"))
    }

    @Test
    fun backupRulesExcludePrivateAppDataFromAllTransferModes() {
        val fullBackupRules = projectXml("src/main/res/xml/backup_rules.xml")
        val dataExtractionRules = projectXml("src/main/res/xml/data_extraction_rules.xml")

        assertHasRootExclude(fullBackupRules.documentElement)
        assertHasRootExclude(dataExtractionRules.getElementsByTagName("cloud-backup").item(0) as Element)
        assertHasRootExclude(dataExtractionRules.getElementsByTagName("device-transfer").item(0) as Element)
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

    private fun projectFile(relativePath: String): File {
        val userDir = File(requireNotNull(System.getProperty("user.dir")))
        return listOf(
            File(userDir, relativePath),
            File(userDir, "app/$relativePath"),
            File(userDir.parentFile ?: userDir, "app/$relativePath"),
        ).first { it.isFile }
    }
}
